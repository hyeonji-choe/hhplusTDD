package io.hhplus.tdd.point;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PointController.class)
public class PointControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    PointService pointService;

    @Test
    @DisplayName("유저의 포인트 조회 테스트")
    void pointTest() throws Exception {
        given(pointService.getUserPoint(1)).willReturn(
                new UserPoint(1,1000,0));
        long userId = 1;
        mockMvc.perform(
                get("/point/"+userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.point").exists())
                .andDo(print());

        verify(pointService).getUserPoint(1);
    }

}
