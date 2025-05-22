package io.hhplus.tdd.point;

import static io.hhplus.tdd.point.TransactionType.CHARGE;
import static io.hhplus.tdd.point.TransactionType.USE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PointServiceTest {

    @InjectMocks
    private PointService pointService;

    @Mock
    UserPointTable userPointTable;

    @Mock
    PointHistoryTable pointHistoryTable;

    @Mock
    UserPointLockManager lockManager;

    /**
     * 유저 포인트 조회 단위 테스트
     */
    @Test
    @DisplayName("유저의 포인트를 조회한다.")
    void getUserPoint() {
        //given
        final long userId = 1L;
        final long amount = 1000L;

        UserPoint userPoint = new UserPoint(userId, amount, System.currentTimeMillis());
        given(userPointTable.selectById(anyLong())).willReturn(userPoint);

        //when
        UserPoint result = pointService.getUserPoint(userId);

        //then
        assertThat(result.id()).isEqualTo(userPoint.id());
        assertThat(result.point()).isEqualTo(userPoint.point());

    }

    /**
     * 유저 포인트 충전/이용 내역 단위 테스트
     */
    @Test
    @DisplayName("유저의 포인트 충전/이용 내역을 조회한다.")
    void getUserHistory() {
        //given
        final long userId = 1L;
        final long amount1 = 1000L;
        final long amount2 = 2000L;

        PointHistory history1 = new PointHistory(1L, userId, amount1, CHARGE,
            System.currentTimeMillis());
        PointHistory history2 = new PointHistory(2L, userId, amount2, USE,
            System.currentTimeMillis());

        List<PointHistory> historyList = List.of(history1, history2);
        given(pointHistoryTable.selectAllByUserId(userId)).willReturn(historyList);

        //when
        List<PointHistory> resultList = pointService.getUserHistory(userId);

        //then
        assertThat(resultList)
            .hasSize(2)
            .extracting("id", "userId", "amount", "type")
            .containsExactlyInAnyOrder(
                tuple(1L, userId, amount1, CHARGE),
                tuple(2L, userId, amount2, USE)
            );

    }

    /**
     * 포인트 충전 단위 테스트
     */
    @Test
    @DisplayName("유저가 포인트를 충전한다.")
    void charge() {
        //given
        final long userId = 1L;
        final long amount = 1000L;

        UserPoint beforeCharge = new UserPoint(userId, amount, System.currentTimeMillis());
        UserPoint afterCharge = new UserPoint(userId, beforeCharge.point() + amount,
            System.currentTimeMillis());

        given(userPointTable.selectById(anyLong())).willReturn(beforeCharge);
        given(userPointTable.insertOrUpdate(anyLong(), anyLong())).willReturn(afterCharge);
        given(lockManager.getLock(anyLong())).willReturn(new ReentrantLock(true));

        //when
        UserPoint result = pointService.charge(userId, amount);

        //then
        assertThat(result.id()).isEqualTo(userId);
        assertThat(result.point()).isEqualTo(beforeCharge.point() + amount);

    }

    @Test
    @DisplayName("100,000 포인트 이상 충전하면 예외가 발생한다.")
    void charge_throwsException_whenExceedsMaxPoint() {
        // given
        final long userId = 1L;
        final long amount = 100_000L;
        final long chargePoint = 1000L;

        UserPoint userPoint = new UserPoint(userId, amount, System.currentTimeMillis());
        given(userPointTable.selectById(anyLong())).willReturn(userPoint);
        given(lockManager.getLock(anyLong())).willReturn(new ReentrantLock(true));

        // when
        Throwable throwable = catchThrowable(() -> pointService.charge(userId, chargePoint));

        // then
        assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("보유할 수 있는 최대 포인트는 100,000 포인트 입니다.");
    }

    @Test
    @DisplayName("1 포인트 미만 금액을 충전하면 예외가 발생한다")
    void charge_throwsException_whenExceedsMinPoint() {
        // given
        final long userId = 1L;
        final long amount = 1000L;
        final long chargePoint = 0;

        UserPoint userPoint = new UserPoint(userId, amount, System.currentTimeMillis());
        given(userPointTable.selectById(anyLong())).willReturn(userPoint);
        given(lockManager.getLock(anyLong())).willReturn(new ReentrantLock(true));

        // when
        Throwable throwable = catchThrowable(() -> pointService.charge(userId, chargePoint));

        // then
        assertThat(throwable).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("포인트 충전은 1 Point 이상 충전 가능합니다.");

    }

    /**
     * 포인트 사용 단위 테스트
     */
    @Test
    @DisplayName("유저가 포인트를 사용한다.")
    void use() {
        // given
        final long userId = 1L;
        final long amount = 5000L;
        final long usePoint = 1000L;

        UserPoint beforeUse = new UserPoint(userId, amount,
            System.currentTimeMillis());// 사용 전 유저 정보
        UserPoint afterUse = new UserPoint(userId, amount - usePoint,
            System.currentTimeMillis());// 사용 후 유저 정보

        given(userPointTable.selectById(anyLong())).willReturn(beforeUse);
        given(userPointTable.insertOrUpdate(anyLong(), anyLong())).willReturn(afterUse);
        given(lockManager.getLock(anyLong())).willReturn(new ReentrantLock(true));

        // when
        UserPoint usedUserPoint = pointService.use(beforeUse.id(), usePoint);

        // then
        assertThat(usedUserPoint.point()).isEqualTo(afterUse.point());

    }

    @Test
    @DisplayName("사용 포인트가 보유중인 포인트보다 많으면 예외가 발생한다.")
    void use_throwsException_whenAmountExceedsBalance() {
        // given
        final long userId = 1L;
        final long amount = 1000L;
        final long usePoint = 3000L;

        UserPoint currentUserPoint = new UserPoint(userId, amount, System.currentTimeMillis());
        given(userPointTable.selectById(anyLong())).willReturn(currentUserPoint);
        given(lockManager.getLock(anyLong())).willReturn(new ReentrantLock(true));

        // when
        Throwable throwable = catchThrowable(() -> pointService.use(userId, usePoint));

        // then
        assertThat(throwable).isInstanceOf(Exception.class)
            .hasMessageContaining("사용할 포인트가 보유중인 포인트보다 많습니다.");

    }

    @Test
    @DisplayName("1포인트 미만 금액을 충전하면 예외가 발생한다.")
    void use_throwsException_whenBalanceIsZero() {
        // given
        final long userId = 1L;
        final long amount = 3000;
        final long usePoint = 0L;

        UserPoint currentUserPoint = new UserPoint(userId, amount, System.currentTimeMillis());
        given(userPointTable.selectById(anyLong())).willReturn(currentUserPoint);
        given(lockManager.getLock(anyLong())).willReturn(new ReentrantLock(true));

        // when
        Throwable throwable = catchThrowable(() -> pointService.use(userId, usePoint));

        // then
        assertThat(throwable).isInstanceOf(Exception.class)
            .hasMessageContaining("포인트 사용은 1 Point 이상 사용 가능합니다.");

    }

}
