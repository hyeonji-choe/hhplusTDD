package io.hhplus.tdd.point;


import java.util.List;

public interface PointService {

    UserPoint getUserPoint(long userId);
    List<PointHistory> getUserPointHistory(long userId);
    UserPoint chargePoint(long userId, long amount);
    UserPoint usePoint(long userId, long amount);
}
