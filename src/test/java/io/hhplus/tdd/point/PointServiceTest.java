package io.hhplus.tdd.point;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PointServiceTest {

    private PointRepository pointRepository;
    private PointService pointService;

    @BeforeEach
    void setUp() {
        pointRepository = new PointRepositoryImpl(); // 실제 구현체 사용
        pointService = new PointService(pointRepository);
    }

    @Test
    @DisplayName("포인트 조회 테스트")
    void 포인트_조회_테스트() {
        // given
        long id = 1L;
        UserPoint expectedUserPoint = new UserPoint(id, 0, System.currentTimeMillis()); // 초기 포인트 0으로 설정

        // when
        UserPoint userPoint = pointService.getPoint(id);

        // then
        assertEquals(expectedUserPoint, userPoint); // 비교 검증
    }

    @Test
    @DisplayName("포인트 충전 테스트")
    void 포인트_충전_테스트() {
        // given
        long id = 1L;
        long amount = 1000L; // 충전할 포인트

        // when
        UserPoint userPoint = pointService.chargePoint(id, amount);

        // then
        assertEquals(amount, userPoint.point()); // 비교 검증
    }

    @Test
    @DisplayName("포인트 이력 조회 테스트")
    void 포인트_이력_조회_테스트() {
        // given
        long id = 1L;
        List<PointHistory> expectedHistories = List.of(
                new PointHistory(1L, id, 1000, TransactionType.CHARGE, System.currentTimeMillis()),
                new PointHistory(2L, id, 200, TransactionType.USE, System.currentTimeMillis())
            ); // 포인트 이력 List 생성
        for (PointHistory history : expectedHistories) {
            pointRepository.savePointHistory(history);
        } // 포인트 이력 List 저장

        // when
        List<PointHistory> actualHistories = pointService.getHistory(id); // 실제 사용자 포인트 내역 조회

        // then
        assertEquals(expectedHistories.size(), actualHistories.size());
        assertEquals(expectedHistories.get(0), actualHistories.get(0));
        assertEquals(expectedHistories.get(1), actualHistories.get(1));
    }

    @Test
    @DisplayName("포인트 사용 테스트")
    void usePointTest() {
        // given
        long userId = 1L;
        long initPoint = 1000L; // 초기 포인트
        long amount = 200L; // 사용할 포인트
        UserPoint currentUserPoint = new UserPoint(userId, initPoint, System.currentTimeMillis()); // 사용자 생성
        pointRepository.saveUserPoint(currentUserPoint); // 초기 포인트 저장

        // when
        if (currentUserPoint.point() >= amount) { // 기존 포인트가 사용할 포인트보다 많아야 함
            UserPoint updatedUserPoint = pointService.usePoint(userId, amount); // 포인트 사용
            // then
            assertEquals(currentUserPoint.point() - amount, updatedUserPoint.point()); // 비교 검증
        } else {
            assertThrows(IllegalArgumentException.class, () -> pointService.usePoint(userId, amount)); // 포인트 부족
        }
    }

    @Test
    @DisplayName("동시성_테스트_충전")
    void 동시성_테스트_충전() throws InterruptedException {
        // given
        long userId = 1L;
        long initPoint = 1000L; // 초기 포인트
        long amount1 = 200L; // 첫 번째 충전할 포인트
        long amount2 = 300L; // 두 번째 충전할 포인트

        UserPoint currentUserPoint = new UserPoint(userId, initPoint, System.currentTimeMillis());
        pointRepository.saveUserPoint(currentUserPoint); // 초기 포인트 저장

        // when
        ExecutorService executor = Executors.newFixedThreadPool(2); //  스레드 풀 2개 생성

        // 첫 번째 충전 요청
        executor.submit(() -> pointService.chargePoint(userId, amount1));
        // 두 번째 충전 요청
        executor.submit(() -> pointService.chargePoint(userId, amount2));

        executor.shutdown(); // 모든 작업 처리 후 스레드 풀 종료
        executor.awaitTermination(5, TimeUnit.SECONDS); // 최대 5초 동안 스레드 풀 종료를 대기

        // then
        // 최종적으로 사용자의 포인트 검증
        assertEquals(initPoint + amount1 + amount2, pointService.getPoint(userId).point());
    }

    @Test
    @DisplayName("포인트 충전/사용 동시성 테스트")
    void 포인트_충전_사용_동시성_테스트() throws InterruptedException {
        int numberOfThreads = 4; // 동시성을 테스트하기 위한 스레드 수
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads); // 고정된 스레드 풀 생성

        Runnable task = () -> {
            pointService.chargePoint(1L, 1); // 1포인트 충전
            pointService.usePoint(1L, 1); // 1포인트 사용
        };

        for (int i = 0; i < numberOfThreads; i++) {
            executorService.submit(task); // 스레드 풀에 작업 제출
        }

        executorService.shutdown(); // 모든 작업 처리 후 스레드 풀 종료
        executorService.awaitTermination(1, TimeUnit.MINUTES); // 스레드 풀 종료 대기

        // 최종 사용자 포인트 검증
        long expectedPoints = 0; // 전체 스레드에서 충전과 사용이 일치하면 0이 되어야 함
        long actualPoints = pointService.getPoint(1L).point();
        assertEquals(expectedPoints, actualPoints, "예상 포인트와 실제 포인트 값이 다릅니다.");
    }
}