"""
DateDate 일정조율 앱 로고 생성기.

컨셉: 인디고→퍼플 그라데이션 배경(다크모드 대응, 투명 없음) 위에
흰 달력 카드 + 그린 체크마크 → "함께 가능한 날짜를 정하고 확정" 직관 전달.
순수 기하 도형이라 저작권 이슈 없음. 4x 슈퍼샘플링으로 작은 화면에서도 선명.

출력: src/main/resources/static/images/logo.png (600x600, PNG, 불투명)
"""
from PIL import Image, ImageDraw
import os

S = 4                      # supersampling factor
SIZE = 600
W = SIZE * S               # working canvas size

# ---- palette ----------------------------------------------------------------
GRAD_TOP    = (99, 91, 246)    # 인디고
GRAD_BOTTOM = (149, 78, 232)   # 퍼플
CARD        = (248, 250, 252)  # 거의 흰색 카드
CARD_SHADOW = (60, 40, 120)    # 카드 그림자(그라데이션 위 톤)
HEADER      = (79, 70, 229)    # 카드 헤더 밴드 (인디고 딥)
RING        = (224, 231, 255)  # 바인더 링
DOT         = (199, 210, 254)  # 헤더 안 참가자 점(연 인디고)
CHECK       = (52, 211, 153)   # 그린 체크 (available / confirmed)


def lerp(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def make_gradient(w, h):
    """대각선 그라데이션을 저해상도로 만든 뒤 업스케일(부드러움 유지)."""
    g = 64
    small = Image.new("RGB", (g, g))
    px = small.load()
    for y in range(g):
        for x in range(g):
            # 좌상 -> 우하 대각선
            t = (x + y) / (2 * (g - 1))
            px[x, y] = lerp(GRAD_TOP, GRAD_BOTTOM, t)
    return small.resize((w, h), Image.BICUBIC)


def rounded_mask(w, h, radius):
    m = Image.new("L", (w, h), 0)
    d = ImageDraw.Draw(m)
    d.rounded_rectangle([0, 0, w - 1, h - 1], radius=radius, fill=255)
    return m


# ---- 1. 배경 그라데이션 (풀블리드, 불투명) -----------------------------------
# 정사각형 전체를 그라데이션으로 채움. 코너 라운딩은 플랫폼 아이콘 마스크에 위임.
base = make_gradient(W, W).convert("RGB")

draw = ImageDraw.Draw(base, "RGBA")

# ---- 2. 달력 카드 좌표(600 기준 → *S) ----------------------------------------
def r(*v):
    return [int(c * S) for c in v]

cx0, cy0, cx1, cy1 = 150, 196, 450, 470   # 카드 본체
card_radius = 34

# 그림자 (살짝 아래로)
shadow = Image.new("RGBA", (W, W), (0, 0, 0, 0))
sd = ImageDraw.Draw(shadow)
sd.rounded_rectangle(r(cx0, cy0 + 10, cx1, cy1 + 16),
                     radius=int(card_radius * S),
                     fill=(20, 12, 50, 110))
shadow = shadow.filter(__import__("PIL.ImageFilter", fromlist=["GaussianBlur"]).GaussianBlur(12 * S))
base.paste(Image.new("RGB", (W, W), (0, 0, 0)), (0, 0), shadow.split()[3])
# (위 paste는 검정으로 어둡게 → 그림자 효과)
draw = ImageDraw.Draw(base, "RGBA")

# ---- 3. 바인더 링(달력 상단 두 다리) -----------------------------------------
ring_w, ring_h, ring_r = 28, 56, 14
for rx in (210, 360):
    draw.rounded_rectangle(r(rx, 168, rx + ring_w, 168 + ring_h),
                           radius=int(ring_r * S), fill=RING)

# 카드 본체
draw.rounded_rectangle(r(cx0, cy0, cx1, cy1),
                       radius=int(card_radius * S), fill=CARD)

# ---- 4. 헤더 밴드 (상단 라운드만) --------------------------------------------
hdr_bottom = 262
header_layer = Image.new("RGBA", (W, W), (0, 0, 0, 0))
hd = ImageDraw.Draw(header_layer)
hd.rounded_rectangle(r(cx0, cy0, cx1, hdr_bottom),
                     radius=int(card_radius * S), fill=HEADER + (255,))
# 아래쪽 라운드 제거(직각으로)
hd.rectangle(r(cx0, hdr_bottom - card_radius, cx1, hdr_bottom), fill=HEADER + (255,))
base.paste(header_layer, (0, 0), header_layer)
draw = ImageDraw.Draw(base, "RGBA")

# 헤더 안 참가자 점 3개(그룹/여러 사람 암시)
dot_y = (cy0 + hdr_bottom) // 2 + 4
for i, dx in enumerate((222, 264, 306)):
    draw.ellipse(r(dx, dot_y - 11, dx + 22, dot_y + 11), fill=DOT)

# 링이 헤더를 관통하는 구멍 느낌
for rx in (210, 360):
    draw.rounded_rectangle(r(rx, 178, rx + ring_w, 178 + 34),
                           radius=int(ring_r * S), fill=HEADER)

# ---- 5. 그린 체크마크 (확정/가능) --------------------------------------------
# 굵은 폴리라인 체크. 둥근 끝 처리.
check_pts = [(228, 372), (282, 426), (388, 312)]
check_pts_s = [(int(x * S), int(y * S)) for x, y in check_pts]
lw = int(34 * S)
draw.line(check_pts_s, fill=CHECK, width=lw, joint="curve")
# 끝점 둥글게
for p in (check_pts_s[0], check_pts_s[-1]):
    draw.ellipse([p[0] - lw // 2, p[1] - lw // 2, p[0] + lw // 2, p[1] + lw // 2], fill=CHECK)

# ---- 6. 다운샘플 & 저장 ------------------------------------------------------
final = base.resize((SIZE, SIZE), Image.LANCZOS).convert("RGB")
out_dir = "src/main/resources/static/images"
os.makedirs(out_dir, exist_ok=True)
out = os.path.join(out_dir, "logo.png")
final.save(out, "PNG", optimize=True)

# 미리보기 작은 사이즈도 확인용 저장
final.resize((96, 96), Image.LANCZOS).save("scripts/logo_preview_96.png", "PNG")

print("saved:", out, final.size, "mode=", final.mode)
