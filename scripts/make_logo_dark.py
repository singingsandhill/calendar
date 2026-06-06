"""
DateDate 일정조율 앱 로고 - 다크모드 버전.

라이트 버전(logo.png)의 짝. 어두운 그라데이션 배경 + 카드 뒤 인디고 글로우 +
다크 서피스 달력 카드(엘리베이션 테두리) + 밝은 인디고 헤더 + 비비드 그린 체크.
다크 UI/테마에서 눈부심 없이 자연스럽게 보이도록 톤 조정.

출력: src/main/resources/static/images/logo_dark.png (600x600, PNG, 불투명)
"""
from PIL import Image, ImageDraw, ImageFilter
import os

S = 4
SIZE = 600
W = SIZE * S

# ---- palette (dark) ---------------------------------------------------------
BG_TOP    = (16, 18, 27)     # 배경 좌상 (near-black indigo)
BG_BOTTOM = (26, 29, 43)     # 배경 우하
GLOW      = (99, 91, 246)    # 카드 뒤 인디고 글로우
CARD_TOP  = (42, 46, 62)     # 다크 서피스 카드 상단
CARD_BOT  = (30, 33, 47)     # 카드 하단
CARD_EDGE = (74, 80, 104)    # 카드 테두리(엘리베이션)
HEADER_TOP= (109, 99, 255)   # 헤더 인디고(밝게)
HEADER_BOT= (139, 92, 246)   # 헤더 퍼플
RING      = (90, 98, 128)    # 링(다크 톤)
RING_HL   = (130, 138, 170)  # 링 하이라이트
DOT       = (165, 180, 252)  # 헤더 점(연 인디고)
CHECK     = (52, 211, 153)   # 그린 체크(비비드)


def lerp(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def diag_gradient(w, top, bot):
    g = 64
    s = Image.new("RGB", (g, g))
    px = s.load()
    for y in range(g):
        for x in range(g):
            px[x, y] = lerp(top, bot, (x + y) / (2 * (g - 1)))
    return s.resize((w, w), Image.BICUBIC)


def vgrad(w, h, top, bot):
    img = Image.new("RGB", (w, h))
    px = img.load()
    for y in range(h):
        c = lerp(top, bot, y / max(1, h - 1))
        for x in range(w):
            px[x, y] = c
    return img


def r(*v):
    return [int(c * S) for c in v]


# ---- geometry (600 base) ----------------------------------------------------
CX0, CY0, CX1, CY1 = 150, 196, 450, 470
CARD_R = 34
HDR_BOTTOM = 262
RINGS = (210, 360)
RING_W, RING_H, RING_R = 28, 56, 14
RING_Y = 168

# ---- 1. 배경 (다크 그라데이션, 불투명) --------------------------------------
base = diag_gradient(W, BG_TOP, BG_BOTTOM).convert("RGBA")

# ---- 2. 카드 뒤 인디고 글로우 -----------------------------------------------
glow = Image.new("RGBA", (W, W), (0, 0, 0, 0))
gd = ImageDraw.Draw(glow)
ccx, ccy = (CX0 + CX1) // 2, (CY0 + CY1) // 2
gr = 210
gd.ellipse(r(ccx - gr, ccy - gr, ccx + gr, ccy + gr), fill=GLOW + (90,))
glow = glow.filter(ImageFilter.GaussianBlur(60 * S))
base = Image.alpha_composite(base, glow)

# ---- 3. 카드 드롭 섀도 ------------------------------------------------------
shadow = Image.new("RGBA", (W, W), (0, 0, 0, 0))
sd = ImageDraw.Draw(shadow)
sd.rounded_rectangle(r(CX0, CY0 + 14, CX1, CY1 + 20),
                     radius=int(CARD_R * S), fill=(0, 0, 0, 150))
shadow = shadow.filter(ImageFilter.GaussianBlur(16 * S))
base = Image.alpha_composite(base, shadow)

draw = ImageDraw.Draw(base, "RGBA")

# ---- 4. 바인더 링 -----------------------------------------------------------
for rx in RINGS:
    draw.rounded_rectangle(r(rx, RING_Y, rx + RING_W, RING_Y + RING_H),
                           radius=int(RING_R * S), fill=RING)
    draw.rounded_rectangle(r(rx + 4, RING_Y + 3, rx + RING_W - 4, RING_Y + 20),
                           radius=int(8 * S), fill=RING_HL)

# ---- 5. 카드 본체 (다크 서피스 + 엘리베이션 테두리) -------------------------
card_mask = Image.new("L", (W, W), 0)
ImageDraw.Draw(card_mask).rounded_rectangle(
    r(CX0, CY0, CX1, CY1), radius=int(CARD_R * S), fill=255)
base.paste(vgrad(W, W, CARD_TOP, CARD_BOT).convert("RGBA"), (0, 0), card_mask)
draw = ImageDraw.Draw(base, "RGBA")
# 테두리(상단 밝게 = 광원 위)
draw.rounded_rectangle(r(CX0, CY0, CX1, CY1), radius=int(CARD_R * S),
                       outline=CARD_EDGE, width=int(2 * S))

# ---- 6. 헤더 밴드 (상단만 라운드) -------------------------------------------
hdr = Image.new("RGBA", (W, W), (0, 0, 0, 0))
hdr_mask = Image.new("L", (W, W), 0)
hm = ImageDraw.Draw(hdr_mask)
hm.rounded_rectangle(r(CX0, CY0, CX1, HDR_BOTTOM), radius=int(CARD_R * S), fill=255)
hm.rectangle(r(CX0, HDR_BOTTOM - CARD_R, CX1, HDR_BOTTOM), fill=255)
hdr_mask = Image.composite(hdr_mask, Image.new("L", (W, W), 0), card_mask)
hdr.paste(vgrad(W, W, HEADER_TOP, HEADER_BOT).convert("RGBA"), (0, 0), hdr_mask)
base = Image.alpha_composite(base, hdr)
draw = ImageDraw.Draw(base, "RGBA")

# 헤더 참가자 점 3개 (그룹 암시)
dot_cy = (CY0 + HDR_BOTTOM) // 2 + 4
for dx in (222, 264, 306):
    draw.ellipse(r(dx, dot_cy - 11, dx + 22, dot_cy + 11), fill=DOT)

# 링이 헤더 통과하는 구멍
hole = lerp(HEADER_TOP, HEADER_BOT, 0.35)
for rx in RINGS:
    draw.rounded_rectangle(r(rx, RING_Y + 10, rx + RING_W, RING_Y + 44),
                           radius=int(RING_R * S), fill=hole)

# ---- 7. 그린 체크 (글로우 + 본체) -------------------------------------------
pts = [(228, 372), (282, 426), (388, 312)]
ps = [(int(x * S), int(y * S)) for x, y in pts]
lw = int(34 * S)

# 체크 글로우(다크모드 포인트)
gl = Image.new("RGBA", (W, W), (0, 0, 0, 0))
gld = ImageDraw.Draw(gl)
gld.line(ps, fill=CHECK + (160,), width=lw + int(10 * S), joint="curve")
gl = gl.filter(ImageFilter.GaussianBlur(10 * S))
base = Image.alpha_composite(base, gl)
draw = ImageDraw.Draw(base, "RGBA")

draw.line(ps, fill=CHECK, width=lw, joint="curve")
for p in (ps[0], ps[-1]):
    draw.ellipse([p[0] - lw // 2, p[1] - lw // 2, p[0] + lw // 2, p[1] + lw // 2], fill=CHECK)

# ---- 8. 다운샘플 & 저장 -----------------------------------------------------
final = base.resize((SIZE, SIZE), Image.LANCZOS).convert("RGB")
out_dir = "src/main/resources/static/images"
os.makedirs(out_dir, exist_ok=True)
out = os.path.join(out_dir, "logo_dark.png")
final.save(out, "PNG", optimize=True)
print("saved:", out, final.size, "mode=", final.mode)
