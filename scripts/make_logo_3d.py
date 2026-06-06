"""
DateDate 일정조율 앱 로고 - 3D 아이콘 버전 (투명 배경).

컨셉: 입체로 돌출(extrude)된 달력 오브젝트. 흰 카드 + 인디고 헤더 +
그린 체크마크. 측면 두께 음영 / 상단 광택 하이라이트 / 바닥 소프트 섀도로
3차원 입체감 표현. 배경은 투명(RGBA).

출력: src/main/resources/static/images/logo_3d.png (600x600, PNG, 투명)
"""
from PIL import Image, ImageDraw, ImageFilter
import os

S = 4
SIZE = 600
W = SIZE * S

# ---- palette ----------------------------------------------------------------
CARD_TOP    = (255, 255, 255)
CARD_BOT    = (226, 231, 244)
HEADER_TOP  = (99, 91, 246)     # 인디고
HEADER_BOT  = (124, 58, 237)    # 퍼플
SIDE_LIGHT  = (124, 92, 230)    # 측면(돌출) 앞쪽 - 밝음
SIDE_DARK   = (60, 36, 130)     # 측면 뒤쪽 - 어두움
RING        = (236, 240, 252)
DOT         = (199, 210, 254)
CHECK_TOP   = (74, 222, 153)
CHECK_BOT   = (34, 197, 134)


def lerp(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


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


# ---- 전면(front face) 실루엣 좌표 (600 기준) ---------------------------------
CX0, CY0, CX1, CY1 = 150, 206, 430, 458      # 카드 본체
CARD_R = 38
HDR_BOTTOM = 260                              # 헤더 밴드 하단
RINGS = (200, 340)                            # 링 x 시작
RING_W, RING_H, RING_R = 30, 58, 14
RING_Y = 176

DEPTH = 46          # 돌출 두께(px, 600기준)
UX, UY = 0.62, 0.62 # 돌출 방향(우하향 등각)


def silhouette_mask():
    """전면 형상(카드+링)의 통합 실루엣 마스크(L)."""
    m = Image.new("L", (W, W), 0)
    d = ImageDraw.Draw(m)
    for rx in RINGS:
        d.rounded_rectangle(r(rx, RING_Y, rx + RING_W, RING_Y + RING_H),
                            radius=int(RING_R * S), fill=255)
    d.rounded_rectangle(r(CX0, CY0, CX1, CY1), radius=int(CARD_R * S), fill=255)
    return m


canvas = Image.new("RGBA", (W, W), (0, 0, 0, 0))
sil = silhouette_mask()

# ---- 1. 바닥 소프트 섀도 -----------------------------------------------------
shadow = Image.new("RGBA", (W, W), (0, 0, 0, 0))
sd = ImageDraw.Draw(shadow)
off = int((DEPTH + 26) * S)
# 실루엣을 어둡게 깔고 블러
sh_src = Image.new("RGBA", (W, W), (0, 0, 0, 0))
sh_src.paste(Image.new("RGBA", (W, W), (28, 18, 60, 150)), (off, off), sil)
sh_src = sh_src.filter(ImageFilter.GaussianBlur(20 * S))
canvas = Image.alpha_composite(canvas, sh_src)

# ---- 2. 돌출(extrusion) 측면 ------------------------------------------------
# 뒤(큰 offset, 어두움) -> 앞(작은 offset, 밝음) 순으로 스탬프
body = Image.new("RGBA", (W, W), (0, 0, 0, 0))
for i in range(DEPTH, 0, -1):
    ox, oy = int(i * UX * S), int(i * UY * S)
    col = lerp(SIDE_LIGHT, SIDE_DARK, i / DEPTH)
    layer = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    layer.paste(Image.new("RGBA", (W, W), col + (255,)), (ox, oy), sil)
    body = Image.alpha_composite(body, layer)
canvas = Image.alpha_composite(canvas, body)

# ---- 3. 전면 페이스 ---------------------------------------------------------
face = Image.new("RGBA", (W, W), (0, 0, 0, 0))
fd = ImageDraw.Draw(face)

# 3a. 링 (흰색 + 상단 하이라이트)
for rx in RINGS:
    fd.rounded_rectangle(r(rx, RING_Y, rx + RING_W, RING_Y + RING_H),
                         radius=int(RING_R * S), fill=RING + (255,))
    fd.rounded_rectangle(r(rx + 4, RING_Y + 4, rx + RING_W - 4, RING_Y + 22),
                         radius=int(9 * S), fill=(255, 255, 255, 200))

# 3b. 카드 본체 (수직 그라데이션)
card_mask = Image.new("L", (W, W), 0)
ImageDraw.Draw(card_mask).rounded_rectangle(
    r(CX0, CY0, CX1, CY1), radius=int(CARD_R * S), fill=255)
card_grad = vgrad(W, W, CARD_TOP, CARD_BOT).convert("RGBA")
face.paste(card_grad, (0, 0), card_mask)

# 3c. 헤더 밴드 (상단만 라운드)
hdr = Image.new("RGBA", (W, W), (0, 0, 0, 0))
hdr_grad = vgrad(W, W, HEADER_TOP, HEADER_BOT).convert("RGBA")
hdr_mask = Image.new("L", (W, W), 0)
hm = ImageDraw.Draw(hdr_mask)
hm.rounded_rectangle(r(CX0, CY0, CX1, HDR_BOTTOM), radius=int(CARD_R * S), fill=255)
hm.rectangle(r(CX0, HDR_BOTTOM - CARD_R, CX1, HDR_BOTTOM), fill=255)
# 카드 영역으로 클립
hdr_mask = Image.composite(hdr_mask, Image.new("L", (W, W), 0), card_mask)
hdr.paste(hdr_grad, (0, 0), hdr_mask)
face = Image.alpha_composite(face, hdr)
fd = ImageDraw.Draw(face)

# 헤더 하단 음영 라인(입체감)
fd.rectangle(r(CX0, HDR_BOTTOM, CX1, HDR_BOTTOM + 3), fill=(0, 0, 0, 30))

# 3d. 헤더 참가자 점 3개
dot_cy = (CY0 + HDR_BOTTOM) // 2 + 2
for dx in (236, 276, 316):
    fd.ellipse(r(dx, dot_cy - 11, dx + 22, dot_cy + 11), fill=DOT + (255,))

# 링이 헤더 통과하는 느낌(헤더색 구멍)
hole = lerp(HEADER_TOP, HEADER_BOT, 0.3)
for rx in RINGS:
    fd.rounded_rectangle(r(rx, RING_Y + 12, rx + RING_W, RING_Y + 40),
                         radius=int(RING_R * S), fill=hole + (255,))

# 3e. 그린 체크마크 (입체: 그림자 -> 본체 -> 하이라이트)
pts = [(205, 360), (262, 416), (376, 298)]
ps = [(int(x * S), int(y * S)) for x, y in pts]
lw = int(32 * S)


def stroke(points, color, width, dy=0):
    pp = [(x, y + dy) for x, y in points]
    fd.line(pp, fill=color, width=width, joint="curve")
    for p in (pp[0], pp[-1]):
        fd.ellipse([p[0] - width // 2, p[1] - width // 2,
                    p[0] + width // 2, p[1] + width // 2], fill=color)


stroke(ps, (20, 120, 80, 90), lw + int(4 * S), dy=int(5 * S))   # 살짝 아래 그림자
stroke(ps, CHECK_BOT + (255,), lw)                              # 본체
# 상단 하이라이트(얇게)
hl = [(x, y - int(7 * S)) for x, y in ps]
fd.line(hl, fill=(180, 255, 220, 150), width=int(8 * S), joint="curve")

# 3f. 카드 상단 광택(가벼운 흰 하이라이트)
gloss = Image.new("RGBA", (W, W), (0, 0, 0, 0))
gd = ImageDraw.Draw(gloss)
gd.ellipse(r(CX0 - 30, CY0 - 80, CX1 + 30, CY0 + 120), fill=(255, 255, 255, 38))
gloss = Image.composite(gloss, Image.new("RGBA", (W, W), (0, 0, 0, 0)), card_mask)
face = Image.alpha_composite(face, gloss)

# 전면을 캔버스에 합성 (offset 0 = 가장 앞)
canvas = Image.alpha_composite(canvas, face)

# ---- 4. 다운샘플 & 저장 (투명 유지) -----------------------------------------
final = canvas.resize((SIZE, SIZE), Image.LANCZOS)
out_dir = "src/main/resources/static/images"
os.makedirs(out_dir, exist_ok=True)
out = os.path.join(out_dir, "logo_3d.png")
final.save(out, "PNG")
print("saved:", out, final.size, "mode=", final.mode)
