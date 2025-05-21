package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PointService {

    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    /**
     * 포인트 조회
     * @param id - 조회할 유저 번호
     */
    public UserPoint getUserPoint(long id) {
        return userPointTable.selectById(id);
    }

    /**
     * 충전 / 이용 내역 조회
     *
     * @param id - 조회할 유저 번호
     * @return
     */
    public List<PointHistory> getUserHistory(long id) {
        return pointHistoryTable.selectAllByUserId(id);
    }

    /**
     * 포인트 충전
     * @param id - 충전할 유저 번호
     * @param amount - 충전 금액
     * @return
     */
    public UserPoint charge(long id, long amount) {

        // 등록된 사용자가 있는지 조회
        UserPoint currentUserPoint = userPointTable.selectById(id);
        UserPoint validUserPoint = currentUserPoint.charge(amount);

        UserPoint result = userPointTable.insertOrUpdate(validUserPoint.id(), validUserPoint.point());
        pointHistoryTable.insert(id, amount, TransactionType.CHARGE, System.currentTimeMillis());

        return result;
    }

    /**
     * 포인트 사용
     * @param id - 포인트 사용할 유저 번호
     * @param usePoint - 사용 금액
     * @return
     */
    public UserPoint use(long id, long usePoint) {

        // 등록된 사용자가 있는지 조회
        UserPoint currentUserPoint = userPointTable.selectById(id);
        UserPoint validUserPoint = currentUserPoint.use(usePoint);

        UserPoint result = userPointTable.insertOrUpdate(validUserPoint.id(), validUserPoint.point());
        pointHistoryTable.insert(validUserPoint.id(), validUserPoint.point(), TransactionType.USE, System.currentTimeMillis());

        return result;
    }
}
