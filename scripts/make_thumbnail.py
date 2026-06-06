"""
DateDate 마케팅 썸네일(가로 배너) 생성기 - 1932x828.

좌측: 브랜드 워드마크 + 핵심 카피 + 보조 문구
우측: 미니 달력 일러스트 (참가자별 색 점이 겹치는 '되는 날' + 그린 체크 = 약속 확정)
브랜드 인디고->퍼플 그라데이션 배경. 2x 슈퍼샘플링으로 선명.

출력: src/main/resources/static/images/thumbnail.png (1932x828, PNG)
폰트: Pretendard (없으면 자동 다운로드). 라이선스 OFL.
"""
from PIL import Image, ImageDraw, ImageFilter, ImageFont
import os, zipfile, urllib.request

S = 2
TW, TH = 1932, 828
W, H = TW * S, TH * S

# ---- palette ----------------------------------------------------------------
BG_TL   = (99, 91, 246)
BG_BR   = (149, 78, 232)
WHITE   = (255, 255, 255)
CARD_TOP= (255, 255, 255)
CARD_BOT= (231, 235, 247)
HDR_TOP = (109, 99, 255)
HDR_BOT = (139, 92, 246)
CELL    = (238, 241, 249)
CHECK   = (52, 211, 153)
# 참가자 점 색(겹침 표현)
PCOL = [(96, 165, 250), (251, 146, 60), (244, 114, 182), (52, 211, 153)]


def lerp(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def diag_gradient(w, h, tl, br):
    g = 96
    s = Image.new("RGB", (g, g))
    px = s.load()
    for y in range(g):
        for x in range(g):
            px[x, y] = lerp(tl, br, (x + y) / (2 * (g - 1)))
    return s.resize((w, h), Image.BICUBIC)


# ---- fonts ------------------------------------------------------------------
def find_pretendard():
    cands = [
        "/tmp/pretendard/public/static/alternative",
        "/tmp/pretendard/public/static",
    ]
    for d in cands:
        if os.path.isdir(d) and os.path.exists(os.path.join(d, "Pretendard-Bold.ttf")):
            return d
    # 자동 다운로드
    url = "https://github.com/orioncactus/pretendard/releases/download/v1.3.9/Pretendard-1.3.9.zip"
    zp = "/tmp/pretendard.zip"
    if not os.path.exists(zp):
        urllib.request.urlretrieve(url, zp)
    with zipfile.ZipFile(zp) as z:
        z.extractall("/tmp/pretendard")
    return "/tmp/pretendard/public/static/alternative"


FD = find_pretendard()
def font(weight, size):
    return ImageFont.truetype(os.path.join(FD, f"Pretendard-{weight}.ttf"), int(size * S))

f_brand   = font("ExtraBold", 40)
f_h1      = font("ExtraBold", 104)
f_sub     = font("SemiBold", 44)
f_badge   = font("Bold", 30)
f_cal_hdr = font("Bold", 34)
f_cal_wd  = font("SemiBold", 22)
f_cal_day = font("SemiBold", 26)


def r(*v):
    return [int(c * S) for c in v]


# ---- background -------------------------------------------------------------
img = diag_gradient(W, H, BG_TL, BG_BR).convert("RGBA")

# 우측 아이콘 뒤 부드러운 글로우
glow = Image.new("RGBA", (W, W), (0, 0, 0, 0))
gd = ImageDraw.Draw(glow)
gd.ellipse(r(1170, 60, 1880, 770), fill=(180, 150, 255, 70))
glow = glow.crop((0, 0, W, H)).filter(ImageFilter.GaussianBlur(70 * S))
img = Image.alpha_composite(img, glow)

# 좌하단 장식용 큰 반투명 동그라미들
deco = Image.new("RGBA", (W, H), (0, 0, 0, 0))
dd = ImageDraw.Draw(deco)
dd.ellipse(r(-120, 560, 220, 900), outline=(255, 255, 255, 28), width=int(2 * S))
dd.ellipse(r(1500, -160, 1900, 240), fill=(255, 255, 255, 16))
img = Image.alpha_composite(img, deco)

draw = ImageDraw.Draw(img, "RGBA")

# ---- LEFT: 텍스트 -----------------------------------------------------------
LX = 130

# 브랜드 워드마크 (작은 달력 아이콘 + DateDate)
by = 150
# 미니 로고 아이콘
draw.rounded_rectangle(r(LX, by, LX + 52, by + 52), radius=int(13 * S), fill=WHITE)
draw.rounded_rectangle(r(LX, by, LX + 52, by + 18), radius=int(13 * S), fill=HDR_TOP)
draw.rectangle(r(LX, by + 10, LX + 52, by + 18), fill=HDR_TOP)
draw.line([(LX + 14 * S, (by + 36) * S), (LX + 23 * S, (by + 44) * S),
           (LX + 40 * S, (by + 26) * S)], fill=CHECK, width=int(6 * S), joint="curve")
draw.text(r(LX + 70, by + 2), "DateDate", font=f_brand, fill=WHITE)

# 배지(무료 · 가입/설치 없이)
badge_txt = "무료 · 가입 없이 · 설치 없이"
bb = draw.textbbox((0, 0), badge_txt, font=f_badge)
bw, bh = bb[2] - bb[0], bb[3] - bb[1]
bx0, by0 = LX, 250
pad = 22
draw.rounded_rectangle(r(bx0, by0, bx0 + (bw // S) + pad * 2, by0 + (bh // S) + pad * 2 - 4),
                       radius=int(28 * S), fill=(255, 255, 255, 235))
draw.text((bx0 * S + pad * S, by0 * S + pad * S - bb[1]), badge_txt, font=f_badge, fill=(79, 70, 229))

# 헤드라인 (2줄)
draw.text(r(LX, 332), "약속 날짜,", font=f_h1, fill=WHITE)
draw.text(r(LX, 452), "링크 하나로 끝", font=f_h1, fill=WHITE)
# 강조: "링크" 밑줄 느낌(그린 라인)
draw.rounded_rectangle(r(LX, 588, LX + 224, 600), radius=int(6 * S), fill=CHECK)

# 보조 문구
draw.text(r(LX, 628), "여러 명이 모여 되는 날을 한눈에.", font=f_sub, fill=(255, 255, 255, 235))
draw.text(r(LX, 686), "날짜 · 장소 · 메뉴까지 한 번에 정해요.", font=f_sub, fill=(255, 255, 255, 205))

# ---- RIGHT: 미니 달력 카드 --------------------------------------------------
CX0, CY0, CX1, CY1 = 1240, 150, 1800, 680
CR = 36

# 카드 그림자
sh = Image.new("RGBA", (W, H), (0, 0, 0, 0))
ImageDraw.Draw(sh).rounded_rectangle(r(CX0, CY0 + 18, CX1, CY1 + 24), radius=int(CR * S),
                                     fill=(40, 24, 90, 130))
sh = sh.filter(ImageFilter.GaussianBlur(20 * S))
img = Image.alpha_composite(img, sh)
draw = ImageDraw.Draw(img, "RGBA")

# 카드 본체 (수직 그라데이션)
card_mask = Image.new("L", (W, H), 0)
ImageDraw.Draw(card_mask).rounded_rectangle(r(CX0, CY0, CX1, CY1), radius=int(CR * S), fill=255)
cg = Image.new("RGB", (W, H))
cgpx = cg.load()
for y in range(H):
    c = lerp(CARD_TOP, CARD_BOT, max(0, min(1, (y / S - CY0) / (CY1 - CY0))))
    for x in range(W):
        cgpx[x, y] = c
img.paste(cg.convert("RGBA"), (0, 0), card_mask)
draw = ImageDraw.Draw(img, "RGBA")

# 헤더 밴드
HB = CY0 + 84
hdr_mask = Image.new("L", (W, H), 0)
hm = ImageDraw.Draw(hdr_mask)
hm.rounded_rectangle(r(CX0, CY0, CX1, HB), radius=int(CR * S), fill=255)
hm.rectangle(r(CX0, HB - CR, CX1, HB), fill=255)
hdr_mask = Image.composite(hdr_mask, Image.new("L", (W, H), 0), card_mask)
hg = Image.new("RGB", (W, H))
hgpx = hg.load()
for y in range(H):
    c = lerp(HDR_TOP, HDR_BOT, max(0, min(1, (y / S - CY0) / (HB - CY0))))
    for x in range(W):
        hgpx[x, y] = c
img.paste(hg.convert("RGBA"), (0, 0), hdr_mask)
draw = ImageDraw.Draw(img, "RGBA")

# 헤더 텍스트
draw.text(r(CX0 + 34, CY0 + 22), "2026. 6", font=f_cal_hdr, fill=WHITE)
# 헤더 우측 점 3개(참가자)
for i, dx in enumerate((CX1 - 130, CX1 - 96, CX1 - 62)):
    draw.ellipse(r(dx, CY0 + 34, dx + 18, CY0 + 52), fill=PCOL[i] + (255,))

# 요일 행
wds = ["일", "월", "화", "수", "목", "금", "토"]
gx0 = CX0 + 36
inner = (CX1 - CX0) - 72
cols = 7
gap = 10
cell = (inner - gap * (cols - 1)) / cols
wy = HB + 18
for i, wd in enumerate(wds):
    cxp = gx0 + i * (cell + gap)
    col = (200, 80, 80) if i == 0 else ((80, 110, 200) if i == 6 else (120, 128, 150))
    tb = draw.textbbox((0, 0), wd, font=f_cal_wd)
    draw.text(((cxp + cell / 2) * S - (tb[2] - tb[0]) / 2, wy * S),
              wd, font=f_cal_wd, fill=col)

# 날짜 셀 4행 (간단 배치). 일부 셀에 참가자 점, '되는 날' 1칸은 그린+체크.
gy0 = wy + 44
rows = 4
ch = 78
rgap = 12
# 셀별 참가자 점 개수 맵 (행,열)->[색idx...]
marks = {
    (0, 2): [0], (0, 3): [0, 1], (0, 4): [1],
    (1, 1): [2], (1, 3): [0, 1, 2], (1, 5): [3],
    (2, 2): [0, 3], (2, 4): [0, 1, 2, 3], (2, 5): [1],
    (3, 1): [2], (3, 3): [1], (3, 4): [0, 2],
}
best = (2, 4)  # 모두 되는 날
day = 1
for rrow in range(rows):
    for c in range(cols):
        cxp = gx0 + c * (cell + gap)
        cyp = gy0 + rrow * (ch + rgap)
        is_best = (rrow, c) == best
        fill = CHECK if is_best else CELL
        draw.rounded_rectangle([cxp * S, cyp * S, (cxp + cell) * S, (cyp + ch) * S],
                               radius=int(14 * S), fill=fill)
        # 날짜 숫자
        dcol = WHITE if is_best else (120, 128, 150)
        draw.text((cxp * S + 10 * S, cyp * S + 6 * S), str(day), font=f_cal_day, fill=dcol)
        day += 1
        if is_best:
            # 그린 셀 안 체크마크
            p = [(cxp + cell * 0.30, cyp + ch * 0.55),
                 (cxp + cell * 0.45, cyp + ch * 0.72),
                 (cxp + cell * 0.74, cyp + ch * 0.34)]
            draw.line([(x * S, y * S) for x, y in p], fill=WHITE, width=int(7 * S), joint="curve")
        elif (rrow, c) in marks:
            dots = marks[(rrow, c)]
            n = len(dots)
            dot_r = 6
            total = n * (dot_r * 2) + (n - 1) * 4
            sx = cxp + cell / 2 - total / 2
            dyp = cyp + ch - 20
            for k, ci in enumerate(dots):
                ddx = sx + k * (dot_r * 2 + 4)
                draw.ellipse([ddx * S, dyp * S, (ddx + dot_r * 2) * S, (dyp + dot_r * 2) * S],
                             fill=PCOL[ci] + (255,))

# '모두 되는 날' 말풍선 표시 (그린 셀 위 작은 라벨)
bx = gx0 + best[1] * (cell + gap)
by = gy0 + best[0] * (ch + rgap)
lbl = "모두 OK"
lb = draw.textbbox((0, 0), lbl, font=f_cal_wd)
lw = (lb[2] - lb[0]) / S
draw.rounded_rectangle(r(bx + cell / 2 - lw / 2 - 12, by - 40, bx + cell / 2 + lw / 2 + 12, by - 6),
                       radius=int(15 * S), fill=(16, 122, 84, 255))
draw.text(((bx + cell / 2) * S - (lb[2] - lb[0]) / 2, (by - 36) * S), lbl,
          font=f_cal_wd, fill=WHITE)

# ---- 저장 -------------------------------------------------------------------
final = img.resize((TW, TH), Image.LANCZOS).convert("RGB")
out_dir = "src/main/resources/static/images"
os.makedirs(out_dir, exist_ok=True)
out = os.path.join(out_dir, "thumbnail.png")
final.save(out, "PNG", optimize=True)
print("saved:", out, final.size, "mode=", final.mode)
