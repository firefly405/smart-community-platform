package com.smartcommunity.controller;

import com.smartcommunity.common.result.Result;
import com.smartcommunity.dto.QaRequest;
import com.smartcommunity.dto.QaResponse;
import com.smartcommunity.security.UserContext;
import com.smartcommunity.service.QaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.CompletableFuture;

@Tag(name = "AI智能问答")
@RestController
@RequestMapping("/api/v1/qa")
@RequiredArgsConstructor
public class QaController {

    private final QaService qaService;

    @Operation(summary = "智能问答（流式SSE）")
    @PostMapping(value = "/ask", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter ask(@RequestBody QaRequest request) {
        SseEmitter emitter = new SseEmitter(300000L);

        Long userId = UserContext.getUserId();
        String userName = UserContext.getUsername();

        CompletableFuture.runAsync(() -> {
            try {
                emitter.send(SseEmitter.event().name("start").data("正在分析您的问题..."));
                QaResponse response = qaService.askSync(request.getQuestion(), userId, userName);
                if (response.getAnswer() != null) {
                    String answer = response.getAnswer();
                    int chunkSize = 5;
                    for (int i = 0; i < answer.length(); i += chunkSize) {
                        String chunk = answer.substring(i, Math.min(i + chunkSize, answer.length()));
                        emitter.send(SseEmitter.event().name("message").data(chunk));
                        Thread.sleep(30);
                    }
                }
                emitter.send(SseEmitter.event().name("done").data("complete"));
                emitter.complete();
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event().name("error").data("AI服务异常：" + e.getMessage()));
                    emitter.complete();
                } catch (Exception ex) {
                    emitter.completeWithError(ex);
                }
            }
        });

        return emitter;
    }

    @Operation(summary = "智能问答（同步）")
    @PostMapping("/ask-sync")
    public Result<QaResponse> askSync(@RequestBody QaRequest request) {
        Long userId = UserContext.getUserId();
        String userName = UserContext.getUsername();
        return Result.success(qaService.askSync(request.getQuestion(), userId, userName));
    }
}
