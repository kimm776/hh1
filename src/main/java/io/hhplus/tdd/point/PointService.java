package io.hhplus.tdd.point;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor //생성자 주입
public class PointService {

    private final PointRepository pointRepository;
    private final Lock lock = new ReentrantLock(); //동시성 문제

    /**
     * 포인트를 조회하는 기능
     */
    public UserPoint getPoint(long id) {
        return pointRepository.findUserPointById(id);
    }

    /**
     * 포인트 내역을 조회하는 기능
     */
    public List<PointHistory> getHistory(long id) {
        return pointRepository.findPointHistoriesByUserId(id);
    }

    /**
     * 포인트를 충전하는 기능
     */
    public UserPoint chargePoint(long id, long amount) {
        lock.lock(); //동시성 문제 - lock 사용
        try {
            UserPoint currentUserPoint = pointRepository.findUserPointById(id);
            // 포인트 충전
            UserPoint updatedUserPoint = pointRepository.saveUserPoint(new UserPoint(id, currentUserPoint.point() + amount, System.currentTimeMillis()));
            // 포인트 충전 내역
            pointRepository.savePointHistory(new PointHistory(0, id, amount, TransactionType.CHARGE, System.currentTimeMillis()));
            return updatedUserPoint;
        } finally {
            lock.unlock(); //lock 해제
        }
    }

    /**
     * 포인트를 사용하는 기능
     */
    public UserPoint usePoint(long id, long amount) {
        lock.lock(); //동시성 문제 - lock 사용
        try {
            UserPoint currentUserPoint = pointRepository.findUserPointById(id);
            if (currentUserPoint.point() < amount) {
                throw new IllegalArgumentException("사용 포인트는 0보다 커야 합니다.");
            }
            // 포인트 사용
            UserPoint updatedUserPoint = pointRepository.saveUserPoint(new UserPoint(id, currentUserPoint.point() - amount, System.currentTimeMillis()));
            // 포인트 사용 내역
            pointRepository.savePointHistory(new PointHistory(0, id, amount, TransactionType.USE, System.currentTimeMillis()));
            return updatedUserPoint;
        } finally {
            lock.unlock(); //lock 해제
        }
    }
}
