package io.hhplus.tdd.point;

public record UserPoint(
        long id,
        long point,
        long updateMillis
) {

    private static final long MAX_POINT = 100_000;
    private static final long MIN_USE_CHARGE_POINT = 1L;

    public static UserPoint empty(long id) {
        return new UserPoint(id, 0, System.currentTimeMillis());
    }

    /**
     * 포인트 사용 시 유효한 금액인지 검증
     * @param amount - 사용할 금액
     * @return
     */
    public UserPoint use(long amount) {
        if (amount < MIN_USE_CHARGE_POINT) {
            throw new IllegalArgumentException("포인트 사용은 1 Point 이상 사용 가능합니다.");
        }else if (amount > this.point) {
            throw new IllegalArgumentException("사용할 포인트가 보유중인 포인트보다 많습니다.");
        }

        return new UserPoint(this.id, this.point - amount, System.currentTimeMillis());
    }

    /**
     * 포인트 충전 시 유효한 금액인지 검증
     * @param amount - 충전할 금액
     * @return
     */
    public UserPoint charge(long amount) {
        if (amount < MIN_USE_CHARGE_POINT) {
            throw new IllegalArgumentException("포인트 충전은 1 Point 이상 충전 가능합니다.");
        }
        else if(MAX_POINT <= this.point + amount) {
            throw new IllegalArgumentException("보유할 수 있는 최대 포인트는 100,000 포인트 입니다.");
        }
        return new UserPoint(this.id, this.point + amount, System.currentTimeMillis());
    }


}
