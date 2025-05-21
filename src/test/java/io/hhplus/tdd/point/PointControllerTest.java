package io.hhplus.tdd.point;

import static io.hhplus.tdd.point.TransactionType.CHARGE;
import static io.hhplus.tdd.point.TransactionType.USE;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = PointController.class)
class PointControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PointService pointService;

    @Test
    @DisplayName("유저 포인트 조회 API")
    void getUserPointTest() throws Exception {

        //given
        long userId = 1L;
        long amount = 1000L;

        UserPoint userPoint = new UserPoint(userId, amount, System.currentTimeMillis());
        given(pointService.getUserPoint(userId)).willReturn(userPoint);

        //when & then
        mockMvc.perform(get("/point/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("id").value(userPoint.id()))
                .andExpect(jsonPath("point").value(userPoint.point()))
                .andExpect(jsonPath("updateMillis").value(userPoint.updateMillis()));

    }

    @Test
    @DisplayName("유저의 포인트 충전/이용 내역 조회 API")
    void getHistoriesTest() throws Exception {
        //given
        long userId = 1L;
        long amount1 = 5000L;
        long amount2 = 3000L;

        PointHistory pointHistory1 = new PointHistory(1L, userId, amount1, CHARGE, System.currentTimeMillis());
        PointHistory pointHistory2 = new PointHistory(2L, userId, amount2, USE, System.currentTimeMillis());

        List<PointHistory> pointHistoryList = List.of(pointHistory1, pointHistory2);
        given(pointService.getUserHistory(userId)).willReturn(pointHistoryList);

        // when & then
        mockMvc.perform(get("/point/{id}/histories", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(pointHistoryList.size()))
                .andExpect(jsonPath("$[0].id").value(pointHistory1.id()))
                .andExpect(jsonPath("$[0].userId").value(pointHistory1.userId()))
                .andExpect(jsonPath("$[0].amount").value(pointHistory1.amount()))
                .andExpect(jsonPath("$[0].type").value(pointHistory1.type().name()))
                .andExpect(jsonPath("$[0].updateMillis").value(pointHistory1.updateMillis()))
                .andExpect(jsonPath("$[1].id").value(pointHistory2.id()))
                .andExpect(jsonPath("$[1].userId").value(pointHistory2.userId()))
                .andExpect(jsonPath("$[1].amount").value(pointHistory2.amount()))
                .andExpect(jsonPath("$[1].type").value(pointHistory2.type().name()))
                .andExpect(jsonPath("$[1].updateMillis").value(pointHistory2.updateMillis()));

    }

    @Test
    @DisplayName("포인트 충전 API")
    void chargeTest() throws Exception {
        // given
        long userId = 1L;
        long amount = 1000L;
        String s_amount = "1000";

        UserPoint chargedUserPoint = new UserPoint(userId, amount, System.currentTimeMillis());
        given(pointService.charge(userId, amount)).willReturn(chargedUserPoint);

        // when & then
        mockMvc.perform(
                    patch("/point/{id}/charge", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(s_amount)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("id").value(chargedUserPoint.id()))
                .andExpect(jsonPath("point").value(chargedUserPoint.point()));
    }

    @Test
    @DisplayName("포인트 사용 API")
    void useTest() throws Exception {
        // given
        long userId = 1L;
        long amount = 5000L;
        long usePoint = 1000L;
        String s_usePoint = "1000";

        UserPoint result = new UserPoint(userId, amount - usePoint, System.currentTimeMillis());

        given(pointService.use(userId, usePoint)).willReturn(result);

        // when & then
        mockMvc.perform(
                    patch("/point/{id}/use", userId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(s_usePoint)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("id").value(result.id()))
                .andExpect(jsonPath("point").value(result.point()));

    }

}