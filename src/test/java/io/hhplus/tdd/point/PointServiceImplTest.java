package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

//@SpringBootTest(classes = {PointService.class, UserPointTable.class, PointHistoryTable.class})
@ExtendWith(SpringExtension.class)
@Import({PointServiceImpl.class, UserPointTable.class, PointHistoryTable.class})
public class PointServiceImplTest {

    private static UserPointTable userPointTable;
    private static PointHistoryTable pointHistoryTable;
    private static PointServiceImpl pointService;

    @BeforeAll
    static void set(){
        userPointTable = new UserPointTable();
        pointHistoryTable = new PointHistoryTable();
        pointService = new PointServiceImpl(userPointTable,pointHistoryTable);
    }
    @BeforeEach
    void insertPoint(){
        UserPoint point = userPointTable.insertOrUpdate(1,10000);
        System.out.println("point : "+point.point()+", userId : "+point.id());
    }

    @Test
    @DisplayName("amount가 0보다 작으면 실패한다")
    public void failAmountTest(){
        assertThatThrownBy(()->{
                    pointService.chargePoint(1,-10);
                })
                .isInstanceOf(RuntimeException.class)
                .message();
    }
    //MAX_POINT = 1000000
    @Test
    @DisplayName("최대포인트 초과하면 실패한다")
    public void failMaxAmountTest(){
        assertThatThrownBy(()->{
            pointService.chargePoint(1,1000001);
        })
                .isInstanceOf(RuntimeException.class);
    }
    @Test
    @DisplayName("최대포인트 사용하면 실패한다")
    public void failMaxUseTest(){
        assertThatThrownBy(()->{
            pointService.usePoint(1,1000001);
        })
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    public void useTest() throws InterruptedException {
        Runnable usePoint = () -> pointService.usePoint(1,1);
        concurentTest(10, usePoint);

        UserPoint after = pointService.getUserPoint(1);
        System.out.println("=====after point : "+after.point());
        assertThat(after.point()).isEqualTo(9990);
    }

    @Test
    public void chargeTest() throws InterruptedException {
        Runnable usePoint = () -> pointService.chargePoint(1,1);
        concurentTest(10, usePoint);

        UserPoint after = pointService.getUserPoint(1);
        System.out.println("=====after point : "+after.point());
        assertThat(after.point()).isEqualTo(10010);
    }

    void concurentTest(int excuteCount, Runnable method) throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(32);//고정된 크기의 스레드풀 생성
        CountDownLatch countDownLatch = new CountDownLatch(excuteCount);
        for(int i =0; i<excuteCount ; i++){
            executorService.submit(() -> {
                method.run();
                countDownLatch.countDown();
            });
        }
        countDownLatch.await();
        System.out.println("========= 모든 작업 완료 =========");
    }
}
