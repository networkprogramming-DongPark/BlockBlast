package blockmatch.common;

/**
 * 블록을 한 번 놓은 결과. 지워진 칸은 보드에서 사라지므로, 지우기 직전의 위치와 색을 따로 남긴다.
 *
 * @param gained    이번 배치로 얻은 점수 (놓은 칸 점수 + 줄 점수)
 * @param fullRows  지운 가로줄. r번째 비트가 1이면 r행을 지웠다.
 * @param fullCols  지운 세로줄. c번째 비트가 1이면 c열을 지웠다.
 * @param cleared   지운 칸만 지우기 전 색상 번호를 갖고, 나머지는 빈 칸인 보드.
 *                  가로줄과 세로줄이 겹치는 칸도 한 칸으로만 남는다.
 */
public record PlaceResult(int gained, int fullRows, int fullCols, Board cleared) {

    /** 지운 줄 수. 가로와 세로를 각각 센다. */
    public int lineCount() {
        return Integer.bitCount(fullRows) + Integer.bitCount(fullCols);
    }
}
