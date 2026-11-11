package me.singingsandhill.calendar.trading.replay;

/**
 * 분봉 안에서 가격을 읽는 방식.
 * <ul>
 *   <li>HIGH_LOW — 봉의 고가·저가로 판정(연속 감시 가정). 같은 봉에서 손절·익절이 둘 다 걸리면 손절 먼저(보수적).
 *       새로 올린 트레일링 스탑은 다음 봉부터 적용(봉 안 순서를 모르므로 미래를 보지 않는다).</li>
 *   <li>CLOSE — 매분 한 번 종가로 판정. 봇이 1분마다 현재가를 한 번 읽는 루프와 같은 형태.</li>
 * </ul>
 */
enum PriceModel { HIGH_LOW, CLOSE }
