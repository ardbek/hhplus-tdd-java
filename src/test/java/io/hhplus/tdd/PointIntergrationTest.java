package io.hhplus.tdd;

import static io.hhplus.tdd.point.TransactionType.CHARGE;
import static io.hhplus.tdd.point.TransactionType.USE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.BDDAssertions.tuple;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.PointService;
import io.hhplus.tdd.point.UserPoint;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

@SpringBootTest
public class PointIntergrationTest {

    @Autowired
    private PointService pointService;

    @Autowired
    private UserPointTable userPointTable;

    @Autowired
    private PointHistoryTable pointHistoryTable;

    // 각 테스트마다 올바른 결과값을 위해 초기화
    @AfterEach
    void reset() {
        Map<?, ?> userPointData = (Map<?, ?>) ReflectionTestUtils.getField(userPointTable, "table");
        if (userPointData != null) {
            userPointData.clear();
        }

        List<?> pointHistoryData = (List<?>) ReflectionTestUtils.getField(pointHistoryTable, "table");
        if (pointHistoryData != null) {
            pointHistoryData.clear();
        }
    }

    @Test
    void 포인트_조회() {
        // given
        long userId = 1L;
        long amount = 1000L;

        userPointTable.insertOrUpdate(userId, amount);

        // when
        UserPoint userPoint = pointService.getUserPoint(userId);

        // then
        assertThat(userPoint.id()).isEqualTo(userId);
        assertThat(userPoint.point()).isEqualTo(amount);

    }

    @Test
    void 포인트_충전_이용_내역_조회() {
        // given
        long userId = 1L;
        long chargeAmount = 1000L;
        long useAmount = 300L;

        pointService.charge(userId, chargeAmount);
        pointService.use(userId, useAmount);

        // when
        List<PointHistory> userHistory = pointService.getUserHistory(userId);

        // then
        assertThat(userHistory.size()).isEqualTo(2);
        assertThat(userHistory).extracting("userId", "amount", "type")
                .containsExactlyInAnyOrder(
                        tuple(userId, chargeAmount, CHARGE),
                        tuple(userId, useAmount, USE)
                );

    }

    @Test
    void 포인트_충전() {
        // given
        long userId = 1L;
        long amount = 1000L;

        // when
        UserPoint result = pointService.charge(userId, amount);

        // then
        assertThat(result.id()).isEqualTo(userId);
        assertThat(result.point()).isEqualTo(amount);
    }


    @Test
    void 포인트_이용() {
        // given
        long userId = 1L;
        long defaultAmount = 1000L;
        long useAmount = 300L;

        pointService.charge(userId, defaultAmount);

        // when
        UserPoint result = pointService.use(userId, useAmount);

        // then
        assertThat(result.id()).isEqualTo(userId);
        assertThat(result.point()).isEqualTo(defaultAmount - useAmount);

    }

    /**
     * 동시성 테스트
     */
    @Test
    void 여러_건의_포인트_충전_이용_요청이_올_경우_순차적으로_처리되어야_한다() throws InterruptedException {
        // given
        long userId = 1L;
        long startAmount = 1000L; // 초기 충전 값
        long chargeAmount = 1000L;
        long useAmount = 500L;
        int threadCount = 10;

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch countDownLatch = new CountDownLatch(threadCount);

        pointService.charge(userId, startAmount);

        // when
        for (int i = 0; i < threadCount; i++) {
            int taskNum = i; // 포인트 충전, 사용 번갈아 요청
            executorService.submit(() -> {
                try {
                    if (taskNum % 2 == 0) {
                        pointService.charge(userId, chargeAmount);// 충전
                    } else {
                        pointService.use(userId, useAmount); // 사용
                    }
                } finally {
                    countDownLatch.countDown();
                }
            });
        }
        countDownLatch.await(); // 모든 스레드 종료 대기
        UserPoint result = pointService.getUserPoint(userId);

        // then
        assertThat(result.point()).isEqualTo(startAmount + (chargeAmount * threadCount / 2) - (useAmount * threadCount / 2));
    }

    @Test
    void 여러_건의_포인트_충전_요청이_올_경우_순차적으로_처리_되어야_한다() throws InterruptedException {
        // given
        long userId = 1L;
        long chargeAmount = 1000L;
        int threadCount = 10;

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch countDownLatch = new CountDownLatch(threadCount);

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    pointService.charge(userId, chargeAmount);// 충전
                } finally {
                    countDownLatch.countDown();
                }
            });
        }
        countDownLatch.await(); // 모든 스레드 종료 대기

        UserPoint result = pointService.getUserPoint(userId);

        // then
        assertThat(result.point()).isEqualTo(chargeAmount * threadCount);

    }

    @Test
    void 여러_건의_포인트_사용_요청이_올_경우_올바른_금액이_사용_되어야_한다() throws InterruptedException {
        // given
        long userId = 1L;
        long defaultAmount = 10000L;
        long useAmount = 1000L;
        int threadCount = 10;

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch countDownLatch = new CountDownLatch(threadCount);

        pointService.charge(userId, defaultAmount);

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    pointService.use(userId, useAmount);// 충전
                } finally {
                    countDownLatch.countDown();
                }
            });
        }
        countDownLatch.await(); // 모든 스레드 종료 대기

        UserPoint result = pointService.getUserPoint(userId);

        // then
        assertThat(result.point()).isEqualTo(defaultAmount - (useAmount * threadCount));

    }
}
