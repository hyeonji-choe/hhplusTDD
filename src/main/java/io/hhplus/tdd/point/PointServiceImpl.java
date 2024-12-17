package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PointServiceImpl implements PointService {
    private static final Logger log = LoggerFactory.getLogger(PointServiceImpl.class);

    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;
    final long MAX_POINT = 1000000;

    @Override
    public UserPoint getUserPoint(long userId) {
        UserPoint userPoint = userPointTable.selectById(userId);
        return userPoint;
    }
    @Override
    public List<PointHistory> getUserPointHistory(long userId) {
        return pointHistoryTable.selectAllByUserId(userId);
    }
    @Override
    public UserPoint validPoint(long userId, long amount){
        UserPoint userPoint = getUserPoint(userId);
        long changePoint = userPoint.point()+amount;

        if(amount==0) throw new RuntimeException("충전/사용할 포인트가 너무 작습니다.");
        //충전 && MAXPOINT 초과
        if (amount>0 && changePoint> MAX_POINT) throw new RuntimeException("최대 충전가능한 포인트는 "+MAX_POINT+"입니다.");
        //사용 && 부족한 포인트
        if(changePoint<0) throw new RuntimeException("포인트가 부족합니다.");

        return userPointTable.insertOrUpdate(userId,changePoint);
    }

    @Override
    public UserPoint chargePoint(long userId, long amount){
        synchronized (this) {
            if(amount<0) throw new RuntimeException("충전/사용할 포인트가 너무 작습니다.");
            UserPoint userPoint = validPoint(userId, amount);
            pointHistoryTable.insert(userId, amount, TransactionType.CHARGE, userPoint.updateMillis());
            return userPoint;
        }
    }
    @Override
    public UserPoint usePoint(long userId, long amount){
        synchronized (this) {
            UserPoint userPoint = validPoint(userId, -1*amount);
            pointHistoryTable.insert(userId, amount, TransactionType.USE, userPoint.updateMillis());
            return userPoint;
        }
    }
}
