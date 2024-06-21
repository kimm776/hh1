package io.hhplus.tdd.point;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc // MockMvc 자동으로 구성
class PointControllerTest {

    @Autowired
    private MockMvc mockMvc; // MockMvc 인스턴스 주입

    @MockBean // Mock 객체로 등록
    private PointService pointService;

    @Test
    @DisplayName("포인트 조회")
    void point() throws Exception {
      // given
        long id = 1L; // 사용자 ID 설정
        UserPoint userPoint = new UserPoint(id, 100, System.currentTimeMillis()); // 사용자 포인트 설정
        when(pointService.getPoint(id)).thenReturn(userPoint); // getPoint 메서드가 userPoint를 반환하도록 모킹

      // when
        mockMvc.perform(get("/point/{id}", id) // GET 요청
                .contentType(MediaType.APPLICATION_JSON)) // 요청 Content-Type을 JSON으로 설정
                .andExpect(status().isOk()) // 응답 상태 200 ??
                .andExpect(content().contentType(MediaType.APPLICATION_JSON)) // 응답 Content-Type이 JSON 형식인지 검증
                .andExpect(jsonPath("$.id").value(userPoint.id())) // id 필드 값 검증
                .andExpect(jsonPath("$.point").value(userPoint.point())) // point 필드 값 검증
                .andExpect(jsonPath("$.updateMillis").isNumber()); // updateMillis 필드 값 검증

      //then
        verify(pointService).getPoint(id); // pointService.getPoint 메서드가 호출되었는지 확인
    }

    @Test
    @DisplayName("포인트 이력 조회 테스트")
    void history() throws Exception {
      // given
        long id = 1L; // 사용자 ID 설정

        List<PointHistory> histories = Arrays.asList(
                new PointHistory(1L, id, 100, TransactionType.CHARGE, System.currentTimeMillis()),
                new PointHistory(2L, id, 50, TransactionType.USE, System.currentTimeMillis())
        ); // 가상의 포인트 이력 데이터

        when(pointService.getHistory(id)).thenReturn(histories); // pointService.getHistory(id) 메서드가 histories를 반환하도록 설정

      // when
        mockMvc.perform(get("/point/{id}/histories", id)) // GET 요청
                .andExpect(status().isOk()) // 응답 상태 200 ??
                .andExpect(content().contentType(MediaType.APPLICATION_JSON)) // 응답이 JSON 형식인지 검증
                .andExpect(jsonPath("$[0].id").value(1L))         // 첫 번째 이력의 id 필드 검증
                .andExpect(jsonPath("$[0].userId").value(id))                  // 첫 번째 이력의 userId 필드 검증
                .andExpect(jsonPath("$[0].amount").value(100))    // 첫 번째 이력의 amount 필드 검증
                .andExpect(jsonPath("$[0].type").value("CHARGE")) // 첫 번째 이력의 type 필드 검증
                .andExpect(jsonPath("$[1].id").value(2L))         // 두 번째 이력의 id 필드 검증
                .andExpect(jsonPath("$[1].userId").value(id))                  // 두 번째 이력의 userId 필드 검증
                .andExpect(jsonPath("$[1].amount").value(50))     // 두 번째 이력의 amount 필드 검증
                .andExpect(jsonPath("$[1].type").value("USE"));   // 두 번째 이력의 type 필드 검증

      // then
        verify(pointService).getHistory(id); // pointService.getHistory(id) 메서드가 호출되었는지 검증
    }

    @Test
    @DisplayName("포인트 충전 테스트")
    void charge() throws Exception {
      // given
        long id = 1L; // 사용자 ID 설정
        long amount = 100L; // 충전할 포인트 설정
        UserPoint userPoint = new UserPoint(id, amount, System.currentTimeMillis());  // 충전 후 포인트 설정
        when(pointService.chargePoint(id, amount)).thenReturn(userPoint); // chargePoint 메서드의 Mock 설정

      // when
        mockMvc.perform(patch("/point/{id}/charge", id) // PATCH 요청 수행
                .content(String.valueOf(amount)) // 포인트 설정
                .contentType(MediaType.APPLICATION_JSON)) // 요청 Content-Type을 JSON으로 설정
                .andExpect(status().isOk()) // 응답 상태 200 ??
                .andExpect(jsonPath("$.id").value(id)) // id 필드 검증
                .andExpect(jsonPath("$.point").value(amount)) // point 필드 검증
                .andExpect(jsonPath("$.updateMillis").isNumber()); // updateMillis 필드가 숫자인지 검증

      // then
        verify(pointService).chargePoint(eq(id), eq(amount)); // chargePoint 메서드가 호출되었는지 확인
    }

    @Test
    @DisplayName("포인트 사용 컨트롤러 테스트")
    void use() throws Exception {
        // given
        long id = 1L; // 사용자 ID 설정
        long currentPoint = 100L; // 현재 포인트 설정
        long amount = 20L; // 사용할 포인트 설정
        UserPoint initUserPoint = new UserPoint(id, currentPoint, System.currentTimeMillis()); // 현재 사용자 포인트를 설정
        UserPoint useUserPoint = new UserPoint(id, currentPoint - amount, System.currentTimeMillis()); // 사용 후 포인트 설정

        when(pointService.getPoint(id)).thenReturn(initUserPoint); // getPoint 메서드가 초기 포인트를 반환하도록 설정
        when(pointService.usePoint(id, amount)).thenReturn(useUserPoint); // usePoint 메서드가 사용 후 포인트를 반환하도록 설정

        // when
        mockMvc.perform(patch("/point/{id}/use", id) // PATCH 요청 수행
                .content(String.valueOf(amount)) // 포인트 설정
                .contentType(MediaType.APPLICATION_JSON)) // 요청 Content-Type을 JSON으로 설정
                .andExpect(status().isOk()) // 응답 상태 200 ??
                .andExpect(jsonPath("$.id").value(id)) // id 필드의 값 검증
                .andExpect(jsonPath("$.point").value(currentPoint - amount)) // 포인트 필드의 값 검증
                .andExpect(jsonPath("$.updateMillis").isNumber()); // updateMillis 필드가 숫자인지 확인

        verify(pointService).usePoint(eq(id), eq(amount)); // usePoint 메서드가 특정 인자로 호출되었는지 검증

    }

}