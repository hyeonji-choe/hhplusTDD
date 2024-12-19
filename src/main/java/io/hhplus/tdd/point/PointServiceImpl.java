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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
public class PointServiceImpl implements PointService {
    private static final Logger log = LoggerFactory.getLogger(PointServiceImpl.class);
    //각 user마다 lock 생성
    private final ConcurrentHashMap<String, ReentrantLock> userLocks = new ConcurrentHashMap<String, ReentrantLock>();
    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;
    final long MAX_POINT = 1000000;

    @Override
    public UserPoint getUserPoint(long userId) {
        return userPointTable.selectById(userId);
    }
    @Override
    public List<PointHistory> getUserPointHistory(long userId) {
        return pointHistoryTable.selectAllByUserId(userId);
    }

    public UserPoint validPoint(long userId, long amount, TransactionType type){
        UserPoint userPoint = getUserPoint(userId);
        long changePoint = userPoint.point()+(type==TransactionType.USE ? amount*(-1) : amount);

        if(amount==0 || amount<0) throw new RuntimeException("충전/사용할 포인트가 너무 작습니다.");
        //충전 && MAXPOINT 초과
        if (changePoint> MAX_POINT) throw new RuntimeException("최대 충전가능한 포인트는 "+MAX_POINT+"입니다.");
        //사용 && 부족한 포인트
        if(changePoint<0) throw new RuntimeException("포인트가 부족합니다.");

        return new UserPoint(userId,changePoint,System.currentTimeMillis());
    }

    @Override
    public UserPoint chargePoint(long userId, long amount){
        //각 user에 대한 Lock 생성
        ReentrantLock lock = userLocks.computeIfAbsent(String.valueOf(userId), id->new ReentrantLock());
        lock.lock();
        UserPoint userPoint;
        try{
            userPoint = validPoint(userId, amount, TransactionType.CHARGE);
            userPoint = userPointTable.insertOrUpdate(userId,userPoint.point());
            pointHistoryTable.insert(userId, amount, TransactionType.CHARGE, userPoint.updateMillis());
        } finally {
            lock.unlock();
            //사용하지 않는 lock cleanup
            userLocks.computeIfPresent(String.valueOf(userId),(id, existingLock)
                    ->existingLock.hasQueuedThreads() ? existingLock : null);
        }
        return userPoint;
    }
    @Override
    public UserPoint usePoint(long userId, long amount){
        //각 user에 대한 Lock 생성
        ReentrantLock lock = userLocks.computeIfAbsent(String.valueOf(userId), id->new ReentrantLock());
        lock.lock();
        UserPoint userPoint;
        try{
            userPoint = validPoint(userId, amount,TransactionType.USE);
            userPoint = userPointTable.insertOrUpdate(userId,userPoint.point());
            pointHistoryTable.insert(userId, amount, TransactionType.USE, userPoint.updateMillis());
        } finally {
            lock.unlock();
            //사용하지 않는 lock cleanup
            userLocks.computeIfPresent(String.valueOf(userId),(id, existingLock)
                    ->existingLock.hasQueuedThreads() ? existingLock : null);
        }
        return userPoint;
    }
}
