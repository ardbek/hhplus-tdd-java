package io.hhplus.tdd.point;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/point")
public class PointController {

    private static final Logger log = LoggerFactory.getLogger(PointController.class);

    private final PointService pointService;

    /**
     * 유저 포인트 조회
     * @param id - 조회할 유저 번호
     * @return UserPoint
     */
    @GetMapping("{id}")
    public UserPoint point(
            @PathVariable long id
    ) {
        return pointService.getUserPoint(id);
    }

    /**
     * 포인트 충전/이용 내역 조회
     * @param id - 조회할 유저 번호
     * @return List<PointHistory>
     */
    @GetMapping("{id}/histories")
    public List<PointHistory> history(
            @PathVariable long id
    ) {
        return pointService.getUserHistory(id);
    }

    /**
     * 포인트 충전
     * @param id - 포인트를 충전 할 유저 번호
     * @param amount - 충전 금액
     * @return UserPoint
     */
    @PatchMapping("{id}/charge")
    public UserPoint charge(
            @PathVariable long id,
            @RequestBody long amount
    ) {
        return pointService.charge(id, amount);
    }

    /**
     * 포인트 사용
     * @param id - 포인트 사용할 유저 번호
     * @param amount - 사용 금액
     * @return UserPoint
     */
    @PatchMapping("{id}/use")
    public UserPoint use(
            @PathVariable long id,
            @RequestBody long amount
    ) {
        return pointService.use(id, amount);
    }
}
