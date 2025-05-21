package io.hhplus.tdd.point;

import static io.hhplus.tdd.point.TransactionType.CHARGE;
import static io.hhplus.tdd.point.TransactionType.USE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import java.util.List;
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

        PointHistory history1 = new PointHistory(1L,userId, amount1, CHARGE, System.currentTimeMillis());
        PointHistory history2 = new PointHistory(2L, userId, amount2, USE, System.currentTimeMillis());

        List<PointHistory> historyList = List.of(history1, history2);
        given(pointHistoryTable.selectAllByUserId(userId)).willReturn(historyList);

        //when
        List<PointHistory> resultList = pointService.getUserHistory(userId);

        //then
        assertThat(resultList)
                .hasSize(2)
                .extracting("id","userId", "amount", "type")
                .containsExactlyInAnyOrder(
                        tuple(1L,userId, amount1, CHARGE),
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
        long userId = 1L;
        long amount = 1000L;

        UserPoint beforeCharge = new UserPoint(userId, amount, System.currentTimeMillis());
        UserPoint afterCharge = new UserPoint(userId, beforeCharge.point() + amount, System.currentTimeMillis());

        given(userPointTable.selectById(anyLong())).willReturn(beforeCharge);
        given(userPointTable.insertOrUpdate(anyLong(), anyLong())).willReturn(afterCharge);

        //when
        UserPoint result = pointService.charge(userId, amount);

        //then
        assertThat(result.id()).isEqualTo(userId);
        assertThat(result.point()).isEqualTo(beforeCharge.point() + amount);

    }

    /**
     * 포인트 사용 단위 테스트
     */
    @Test
    @DisplayName("유저가 포인트를 사용한다.")
    void use() {
        // given
        long userId = 1L;
        long amount = 5000L;
        long usePoint = 1000L;

        UserPoint beforeUse = new UserPoint(userId, amount, System.currentTimeMillis());// 사용 전 유저 정보
        UserPoint afterUse = new UserPoint(userId, amount - usePoint, System.currentTimeMillis());// 사용 후 유저 정보

        given(userPointTable.selectById(anyLong())).willReturn(beforeUse);
        given(userPointTable.insertOrUpdate(anyLong(), anyLong())).willReturn(afterUse);

        // when
        UserPoint usedUserPoint = pointService.use(beforeUse.id(), usePoint);

        // then
        assertThat(usedUserPoint.point()).isEqualTo(afterUse.point());

    }
}
