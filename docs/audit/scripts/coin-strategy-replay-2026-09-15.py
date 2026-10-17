#!/usr/bin/env python3
"""
coin-strategy-replay-2026-09-15.py — trading 모듈(KRW-ADA) 매매 로직을 실제 Bithumb 1분봉 위에서 재현하는 감사용 리플레이.

근거 문서: docs/audit/coin-trading-strategy-review-2026-09-15.md (§2-3 표가 이 스크립트의 출력이다).
의존성: 표준 라이브러리만 (Python 3.10+). 저장소 코드·DB 를 건드리지 않는다 (공개 API GET + 로컬 캐시 파일).

사용법:
  python3 coin-strategy-replay-2026-09-15.py fetch [pages]      # Bithumb 공개 API 로 1분봉 수집 (200개/페이지, 기본 165 → 약 105일)
  python3 coin-strategy-replay-2026-09-15.py run  [variant]     # variant: all | complete/rb | frozen/rb | complete/norb | frozen/norb
  환경변수 STOP_AT_TRIGGER=1 (손절을 임계가에 체결, 낙관 상한) / NO_RISK_ON_RB=1 (리밸런스 포지션 SL/TP/트레일링 면제 = 2026-05 P1-3 이전 설계)

재현 대상 (application.yaml 운영값, 2026-09-15 기준):
  IndicatorService  — SMA 5/20/60, Wilder RSI 14(시드=가장 오래된 14변화), Slow %K = SMA3(Fast %K 14), RSI 추세(3봉 전, 델타 2.0), ATR 14
  DivergenceService — 피벗 강도 3, 최소 거리 5, 룩백 20, RSI/Stoch/거래량 (가격 LL+지표 HL=BULLISH)
  SignalService     — 8컴포넌트 점수(수렴 0.2% 억제 포함), ±40 임계, 3개 동의, RSI<70/StochK<85, MA60 하회 확인 게이트
  TradingBotService — 루프 순서(리스크→신호→강신호≥60→리밸런싱→신호매매), 쿨다운 30분, 최소보유 30분, maxPositions 2,
                      서킷브레이커(연속손실 3/일일 −5%), 물타기 차단, 노출상한 0.8, ATR 비중 15~35%, 슬리피지 버퍼 0.5%
  RiskManagementService — 손절 −1.5%(net), 트레일링 활성 +1.5%/추적 0.8%/손익분기 floor(정수 반올림), TP +3%, 시간청산 360분(net≥0)
  RebalanceService  — 국면 = 현재가 vs MA60(저장봉 60개), 목표 0.7/0.3/0.5, 편차 0.10, 쿨다운 480분, 최소 5,000원, 수익 포지션만 FIFO 전량 청산

실행 모델(가정): 저장봉 = 거래가 있던 분만(API 가 빈 분을 생략); 봇은 매 분 틱; mid = 최근 종가; 시장가 매수 = mid+0.5(매도호가),
  매도 = mid−0.5(매수호가); 수수료 taker 0.25% 양측; 시작 자본 1,000,000 KRW 전액 현금.
  'frozen' 변형: 활성 분의 40% 를 :05 형성봉 스텁(시가=종가=고가=저가, 거래량 5/60)으로 저장된 것으로 모델링 (ADR strategy/0009 캔들 동결 재현).
"""
import json, os, sys, math, hashlib, datetime, statistics, time, urllib.request, urllib.parse
from collections import Counter, defaultdict

HERE = os.path.dirname(os.path.abspath(__file__))
CACHE = os.environ.get("ADA_CACHE", os.path.join(HERE, "ada_1m.json"))
BASE = "https://api.bithumb.com/v1/candles/minutes/1"

# ---- application.yaml 운영값 ----
FEE = 0.0025; SL = -0.015; TP = 0.03; TRAIL = 0.008; TRAIL_ACT = 0.015; MINPROFIT = 0.001; STRONG_MAXLOSS = -0.02; SLIP = 0.005
BUY_T = 40; SELL_T = -40; MIN_AGREE = 3; CONV = 0.002; COOLDOWN = 30; MINHOLD = 30; MAXHOLD = 360; MAXPOS = 2; EXPO_CAP = 0.8
RB_BULL = 0.7; RB_BEAR = 0.3; RB_DEF = 0.5; RB_DEV = 0.10; RB_CD = 480; RB_MIN = 5000; RB_MINPNL = 0.0
CB_LOSSES = 3; CB_DAILY = -0.05; START_KRW = 1_000_000.0
STOP_AT_TRIGGER = os.environ.get("STOP_AT_TRIGGER") == "1"
NO_RISK_ON_RB = os.environ.get("NO_RISK_ON_RB") == "1"


def fetch(pages):
    rows, to = [], None
    for page in range(pages):
        q = {"market": "KRW-ADA", "count": 200}
        if to:
            q["to"] = to
        d = None
        for attempt in range(3):
            try:
                with urllib.request.urlopen(BASE + "?" + urllib.parse.urlencode(q), timeout=20) as r:
                    d = json.load(r)
                break
            except Exception as e:  # noqa: BLE001
                print("page", page, "attempt", attempt, "error", e); time.sleep(2)
        if not d:
            break
        rows.extend(d); to = d[-1]["candle_date_time_utc"]; time.sleep(0.25)
        if page % 20 == 0:
            print("page", page, "oldest", d[-1]["candle_date_time_kst"], flush=True)
    seen, uniq = set(), []
    for c in rows:
        if c["candle_date_time_kst"] in seen:
            continue
        seen.add(c["candle_date_time_kst"]); uniq.append(c)
    uniq.sort(key=lambda c: c["candle_date_time_kst"])
    json.dump(uniq, open(CACHE, "w"))
    print("saved", len(uniq), "candles", uniq[0]["candle_date_time_kst"], "->", uniq[-1]["candle_date_time_kst"])


def parse(t):
    return datetime.datetime.fromisoformat(t)


def build_bars(raw, variant):
    bars = []
    for c in raw:
        b = {"t": parse(c["candle_date_time_kst"]), "o": float(c["opening_price"]), "h": float(c["high_price"]),
             "l": float(c["low_price"]), "c": float(c["trade_price"]), "v": float(c["candle_acc_trade_volume"])}
        if variant == "frozen":
            hsh = int(hashlib.md5(c["candle_date_time_kst"].encode()).hexdigest(), 16) % 1000
            if hsh < 400:
                b = {"t": b["t"], "o": b["o"], "h": b["o"], "l": b["o"], "c": b["o"], "v": b["v"] * 5 / 60}
        bars.append(b)
    return bars


# ---- IndicatorService / DivergenceService 재현 (desc = 최신순) ----
def sma(desc, p):
    return sum(x["c"] for x in desc[:p]) / p if len(desc) >= p else None


def rsi(desc, p):
    n = len(desc)
    if n < p + 1:
        return None
    ag = al = 0.0
    for j in range(n - 2, n - 2 - p, -1):
        ch = desc[j]["c"] - desc[j + 1]["c"]; ag += max(ch, 0); al += max(-ch, 0)
    ag /= p; al /= p
    for j in range(n - 2 - p, -1, -1):
        ch = desc[j]["c"] - desc[j + 1]["c"]; ag = (ag * (p - 1) + max(ch, 0)) / p; al = (al * (p - 1) + max(-ch, 0)) / p
    return 100.0 if al == 0 else 100 - 100 / (1 + ag / al)


def stoch_k(desc, p):
    if len(desc) < p:
        return None
    c = desc[0]["c"]; lo = min(x["l"] for x in desc[:p]); hi = max(x["h"] for x in desc[:p])
    return 50.0 if hi == lo else (c - lo) / (hi - lo) * 100


def slow_k(desc, p, d):
    if len(desc) < p + d:
        return None
    return sum(stoch_k(desc[i:], p) for i in range(d)) / d


def atr(desc, p):
    if len(desc) < p + 1:
        return None
    s = 0.0
    for i in range(p):
        cur, prev = desc[i], desc[i + 1]
        s += max(cur["h"] - cur["l"], abs(cur["h"] - prev["c"]), abs(cur["l"] - prev["c"]))
    return s / p


def pivots(v, lb, k, is_min):
    out = []; end = min(len(v) - k, lb)
    for i in range(k, end):
        ok = all((v[i] < v[i - d] and v[i] < v[i + d]) if is_min else (v[i] > v[i - d] and v[i] > v[i + d]) for d in range(1, k + 1))
        if ok:
            out.append(i)
    return out


def diverge(prices, ind):
    lows = pivots(prices, 20, 3, True); highs = pivots(prices, 20, 3, False)
    if len(lows) >= 2:
        r, p = lows[0], lows[1]
        if p - r >= 5 and prices[r] < prices[p] and ind[r] > ind[p]:
            return 1
    if len(highs) >= 2:
        r, p = highs[0], highs[1]
        if p - r >= 5 and prices[r] > prices[p] and ind[r] < ind[p]:
            return -1
    return 0


def signal(desc100):
    ind = desc100[:80]
    if len(ind) < 80:
        return None
    prev = ind[1:]
    ma5, ma20, ma60 = sma(ind, 5), sma(ind, 20), sma(ind, 60); p5, p20 = sma(prev, 5), sma(prev, 20)
    gap = abs(ma5 - ma20) / ma20
    if gap < CONV:
        cross = 0
    elif p5 <= p20 and ma5 > ma20:
        cross = 25
    elif p5 >= p20 and ma5 < ma20:
        cross = -25
    else:
        cross = 5 if ma5 > ma20 else (-5 if ma5 < ma20 else 0)
    cp = ind[0]["c"]; trend = 8 if cp > ma60 else (-8 if cp < ma60 else 0)
    r = rsi(ind, 14); rlevel = 15 if r < 35 else (-15 if r > 65 else 0)
    sk = slow_k(ind, 14, 3); slevel = 15 if sk < 25 else (-15 if sk > 75 else 0)
    rp = rsi(ind[3:], 14); dl = r - rp; rtrend = 10 if dl > 2 else (-10 if dl < -2 else 0)
    rd = sd = vd = 0
    if len(desc100) >= 20:
        prices = [b["c"] for b in desc100]
        n_series = min(len(desc100) - 14, 20)
        rsis = [rsi(desc100[j:], 14) for j in range(n_series)]
        stochs = [stoch_k(desc100[j:], 14) for j in range(n_series)]
        vols = [b["v"] for b in desc100]
        if len(rsis) >= 20:
            rd = diverge(prices, rsis)
        if len(stochs) >= 20:
            sd = diverge(prices, stochs)
        vd = diverge(prices, vols)
    comps = [cross, trend, rd * 20, rlevel, sd * 15, slevel, vd * 20, rtrend]
    total = sum(comps); direction = 1 if total >= 0 else -1
    agree = sum(1 for s in comps if s * direction > 0)
    st = "HOLD"
    if agree >= MIN_AGREE:
        if total >= BUY_T and r < 70 and sk < 85:
            vols20 = [b["v"] for b in ind[:20]]; volma = sum(vols20) / 20; spike = ind[0]["v"] > 1.5 * volma
            st = "HOLD" if (cp < ma60 and not (rd == 1 or sd == 1 or vd == 1 or r < 30 or spike)) else "BUY"
        elif total <= SELL_T and r > 30 and sk > 15:
            st = "SELL"
    a = atr(ind[:19], 14); atrpct = (a / cp * 100) if a else None
    return {"type": st, "score": total, "rd": rd, "sd": sd, "vd": vd, "ma60": ma60, "cp": cp, "atrpct": atrpct, "cross": cross}


def run(raw, variant="complete", rebalancing=True, start_krw=START_KRW):
    bars = build_bars(raw, variant)
    t0, t1 = bars[0]["t"], bars[-1]["t"]
    idx = {b["t"]: i for i, b in enumerate(bars)}
    krw = start_krw; positions = []; closed = []
    last_trade = None; last_rb = None; consec = 0; day_equity = {}
    sig = None; i = -1
    stats = Counter(); sig_rows = Counter(); buy_eps = []
    fees = spread = moved = 0.0
    one_min = datetime.timedelta(minutes=1)

    def mid():
        return bars[i]["c"]

    def net_pct(p, m):
        cv = m * p["vol"]; return (cv * (1 - FEE) - p["amt"] - p["fee"]) / p["amt"] * 100

    def close_pos(p, reason, now):
        nonlocal krw, fees, spread, moved, consec
        bid = mid() - 0.5
        if STOP_AT_TRIGGER and reason == "STOP_LOSS":
            trig = p["amt"] * (1 + SL) / (p["vol"] * (1 - FEE)) + p["fee"] / (p["vol"] * (1 - FEE))
            bid = max(bid, trig - 0.5)
        proceeds = bid * p["vol"]; f = proceeds * FEE
        krw += proceeds - f; fees += f; spread += 0.5 * p["vol"]; moved += proceeds
        pnl = proceeds - f - p["amt"] - p["fee"]
        closed.append({"reason": reason, "pnl": pnl, "pnl_pct": pnl / p["amt"] * 100, "open": p["opened"], "close": now, "origin": p["origin"]})
        consec = consec + 1 if pnl < 0 else 0
        positions.remove(p)

    def open_pos(amount, origin, now):
        nonlocal krw, fees, spread, moved
        ask = mid() + 0.5; vol = amount / ask; f = amount * FEE
        krw -= amount + f; fees += f; spread += 0.5 * vol; moved += amount
        positions.append({"entry": ask, "vol": vol, "amt": ask * vol, "fee": f, "hwm": ask, "trail": None, "trail_on": False, "opened": now, "origin": origin})

    def trade_by_signal(now, m, day):
        nonlocal last_trade, last_rb
        if last_trade is not None and (now - last_trade).total_seconds() / 60 < COOLDOWN:
            return
        if sig["type"] == "BUY" and len(positions) < MAXPOS:
            stats["buy_attempt"] += 1
            realized_today = sum(c["pnl"] for c in closed if c["close"].date() == day)
            de = day_equity.get(day)
            if consec >= CB_LOSSES or (de and de > 0 and realized_today / de <= CB_DAILY):
                stats["cb_block"] += 1; return
            if krw < 5000:
                stats["nokrw"] += 1; return
            if any(m < p["entry"] for p in positions):
                stats["avgdown_block"] += 1; return
            coinv = sum(p["vol"] * m for p in positions); tot = krw + coinv
            if tot > 0 and coinv / tot >= EXPO_CAP:
                stats["expo_block"] += 1; return
            ap = sig["atrpct"]
            ratio = 0.25 if ap is None else (0.15 if ap >= 3 else (0.35 if ap <= 1 else 0.35 - ((ap - 1) / 2) * 0.20))
            amt = math.floor(math.floor(krw * ratio) * (1 - SLIP))
            if amt < 5000:
                stats["order_below_min"] += 1; return
            open_pos(amt, "SIGNAL", now); stats["signal_buy"] += 1; buy_eps.append(now); last_trade = now; last_rb = now
        elif sig["type"] == "SELL":
            strong = sig["score"] <= -60 or sig["rd"] == -1 or sig["sd"] == -1
            for p in list(positions):
                if (now - p["opened"]).total_seconds() / 60 < MINHOLD:
                    continue
                cons = m * (1 - SLIP); n = (cons * p["vol"] * (1 - FEE) - p["amt"] - p["fee"]) / p["amt"] * 100
                if strong:
                    if n >= STRONG_MAXLOSS * 100:
                        close_pos(p, "SIGNAL_STRONG", now); last_trade = now; last_rb = now
                elif n >= MINPROFIT * 100:
                    close_pos(p, "SIGNAL", now); last_trade = now; last_rb = now

    cur = t0
    while cur <= t1:
        if cur in idx:
            i = idx[cur]
            sig = signal(list(reversed(bars[max(0, i - 99):i + 1])))
            if sig:
                sig_rows[sig["type"]] += 1
        if i < 0 or sig is None:
            cur += one_min; continue
        m = mid(); now = cur; day = now.date()
        if day not in day_equity:
            day_equity[day] = krw + sum(p["vol"] * m for p in positions)
        # 2. 리스크 체크 (최우선)
        closed_this_tick = False
        for p in list(positions):
            if NO_RISK_ON_RB and p["origin"] == "REBALANCE":
                continue
            n = net_pct(p, m)
            if n <= SL * 100:
                close_pos(p, "STOP_LOSS", now); closed_this_tick = True; continue
            if m > p["hwm"]:
                p["hwm"] = m
            if not p["trail_on"] and n >= TRAIL_ACT * 100:
                p["trail_on"] = True; p["trail"] = math.floor(m * (1 - TRAIL))
            if p["trail_on"]:
                nt = max(math.floor(p["hwm"] * (1 - TRAIL)), math.ceil(p["entry"] * (1 + 2 * FEE)))
                if p["trail"] is None or nt > p["trail"]:
                    p["trail"] = nt
                if m <= p["trail"]:
                    close_pos(p, "TRAILING_STOP", now); closed_this_tick = True; continue
            if n >= TP * 100:
                close_pos(p, "TAKE_PROFIT", now); closed_this_tick = True; continue
            if (now - p["opened"]).total_seconds() / 60 >= MAXHOLD and n >= 0:
                close_pos(p, "TIME_EXIT", now); closed_this_tick = True; continue
        if closed_this_tick:
            cur += one_min; continue
        # 4. 강신호 우선
        if abs(sig["score"]) >= 60 and sig["type"] != "HOLD":
            stats["strong_signal"] += 1; trade_by_signal(now, m, day); cur += one_min; continue
        # 5. 리밸런싱
        if rebalancing and (last_rb is None or (now - last_rb).total_seconds() / 60 >= RB_CD):
            coinq = sum(p["vol"] for p in positions); coinv = coinq * m; tot = krw + coinv
            if tot > 0:
                ratio = coinv / tot
                target = RB_BULL if m > sig["ma60"] else (RB_BEAR if m < sig["ma60"] else RB_DEF)
                if abs(ratio - target) >= RB_DEV:
                    diff = target * tot - coinv
                    if abs(diff) >= RB_MIN:
                        if diff > 0:
                            open_pos(diff * (1 - SLIP), "REBALANCE", now); stats["rb_buy"] += 1; last_rb = now; last_trade = now
                            cur += one_min; continue
                        need = (-diff * (1 - SLIP)) / m; sold = 0.0
                        for p in sorted(list(positions), key=lambda p: p["opened"]):
                            if sold >= need:
                                break
                            if net_pct(p, m) < RB_MINPNL * 100:
                                continue
                            sold += p["vol"]; close_pos(p, "REBALANCE", now)
                        if sold > 0:
                            stats["rb_sell"] += 1; last_rb = now; last_trade = now; cur += one_min; continue
                        stats["rb_sell_skipped"] += 1
        # 6. 신호 매매
        trade_by_signal(now, m, day)
        cur += one_min
    m = bars[-1]["c"]; equity = krw + sum(p["vol"] * m for p in positions)
    days = (t1 - t0).total_seconds() / 86400
    by = defaultdict(list)
    for c in closed:
        by[c["reason"]].append(c)
    byo = defaultdict(list)
    for c in closed:
        byo[c["origin"]].append(c)
    mon = defaultdict(float)
    for c in closed:
        mon[c["close"].strftime("%Y-%m")] += c["pnl"]
    return {"variant": variant, "rebalancing": rebalancing, "stop_at_trigger": STOP_AT_TRIGGER, "no_risk_on_rb": NO_RISK_ON_RB,
            "days": round(days, 1), "start": str(t0), "end": str(t1), "final_equity": round(equity),
            "return_pct": round((equity / start_krw - 1) * 100, 2), "price_change_pct": round((bars[-1]["c"] / bars[0]["c"] - 1) * 100, 2),
            "signal_rows": dict(sig_rows), "stats": dict(stats), "closed": len(closed), "open_at_end": len(positions),
            "fees": round(fees), "spread_cost": round(spread), "krw_moved": round(moved), "turnover_x": round(moved / start_krw, 1),
            "buy_episodes": len(buy_eps),
            "by_reason": {k: {"n": len(v), "mean_pnl_pct": round(statistics.mean(x["pnl_pct"] for x in v), 2), "total_pnl": round(sum(x["pnl"] for x in v)),
                              "win_rate": round(100 * sum(1 for x in v if x["pnl"] > 0) / len(v)),
                              "mean_hold_min": round(statistics.mean((x["close"] - x["open"]).total_seconds() / 60 for x in v))} for k, v in by.items()},
            "by_origin": {k: {"n": len(v), "total_pnl": round(sum(x["pnl"] for x in v)), "mean_pnl_pct": round(statistics.mean(x["pnl_pct"] for x in v), 2)} for k, v in byo.items()},
            "monthly_realized_pnl": {k: round(v) for k, v in sorted(mon.items())}}


def summarize(r):
    br, bo = r["by_reason"], r["by_origin"]
    g = lambda k, f, d=0: br.get(k, {}).get(f, d)  # noqa: E731
    print(f"{r['variant']}/{'rb' if r['rebalancing'] else 'norb'} stopTrig={r['stop_at_trigger']} noRiskRb={r['no_risk_on_rb']} | "
          f"{r['days']}d {r['start'][:10]}~{r['end'][:10]} | return {r['return_pct']:+.2f}% (price {r['price_change_pct']:+.1f}%) | "
          f"signals BUY {r['signal_rows'].get('BUY', 0)} SELL {r['signal_rows'].get('SELL', 0)} episodes {r['buy_episodes']} | "
          f"rb_buy {r['stats'].get('rb_buy', 0)} rb_sell {r['stats'].get('rb_sell', 0)} | closed {r['closed']}: "
          f"SL {g('STOP_LOSS','n')}@{g('STOP_LOSS','mean_pnl_pct')}% TP {g('TAKE_PROFIT','n')}@{g('TAKE_PROFIT','mean_pnl_pct')}% "
          f"TRAIL {g('TRAILING_STOP','n')}@{g('TRAILING_STOP','mean_pnl_pct')}% TIME {g('TIME_EXIT','n')}@{g('TIME_EXIT','mean_pnl_pct')}% "
          f"SIGNAL {g('SIGNAL','n')+g('SIGNAL_STRONG','n')} | fees {r['fees']/START_KRW*100:.1f}% spread {r['spread_cost']/START_KRW*100:.1f}% "
          f"turnover {r['turnover_x']}x | " + ", ".join(f"{k} n={v['n']} pnl={v['total_pnl']/START_KRW*100:+.1f}%" for k, v in bo.items())
          + f" | monthly {r['monthly_realized_pnl']}")


if __name__ == "__main__":
    cmd = sys.argv[1] if len(sys.argv) > 1 else "run"
    if cmd == "fetch":
        fetch(int(sys.argv[2]) if len(sys.argv) > 2 else 165)
        sys.exit(0)
    raw = json.load(open(CACHE))
    which = sys.argv[2] if len(sys.argv) > 2 else "all"
    out = {}
    for variant, rb in [("complete", True), ("frozen", True), ("complete", False), ("frozen", False)]:
        key = f"{variant}/{'rb' if rb else 'norb'}"
        if which not in ("all", key):
            continue
        out[key] = run(raw, variant, rb); summarize(out[key])
    json.dump(out, open(os.path.join(HERE, "replay_results.json"), "w"), ensure_ascii=False, indent=1)
