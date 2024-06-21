package io.hhplus.tdd.point;

import java.util.List;

public interface PointRepository {

    UserPoint findUserPointById(long id);

    List<PointHistory> findPointHistoriesByUserId(long userId);

    UserPoint saveUserPoint(UserPoint userPoint);

    void savePointHistory(PointHistory pointHistory);

}
