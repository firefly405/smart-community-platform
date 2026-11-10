package com.smartcommunity.controller;

import com.smartcommunity.common.exception.BusinessException;
import com.smartcommunity.dto.QaResponse;
import com.smartcommunity.security.UserContext;
import com.smartcommunity.service.ResidentQaService;
import org.junit.jupiter.api.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ResidentQaJobTest {
    private final ResidentQaService service = mock(ResidentQaService.class);
    private final ResidentQaController controller = new ResidentQaController(service);
    @AfterEach void cleanup() { controller.shutdown(); UserContext.clear(); }
    @Test void otherUserCannotReadAnswerOrSubmitConcurrentQuestions() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        when(service.ask(anyString(), eq(13L))).thenAnswer(invocation -> { release.await(3, TimeUnit.SECONDS); return new QaResponse(); });
        UserContext.setUserId(13L);
        String job = controller.ask(new ResidentQaController.Question("My bill?")).getData().get("id");
        try {
            assertThrows(BusinessException.class, () -> controller.ask(new ResidentQaController.Question("Another?")));
            UserContext.setUserId(14L);
            assertEquals(404, assertThrows(BusinessException.class, () -> controller.answer(job)).getCode());
        } finally { release.countDown(); }
    }
    @Test void blankQuestionIsRejectedBeforeModelCall() {
        UserContext.setUserId(13L); assertThrows(BusinessException.class, () -> controller.ask(new ResidentQaController.Question(" "))); verifyNoInteractions(service);
    }
}
