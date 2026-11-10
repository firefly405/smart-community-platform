package com.smartcommunity.controller;

import com.smartcommunity.common.exception.BusinessException;
import com.smartcommunity.common.result.Result;
import com.smartcommunity.dto.QaResponse;
import com.smartcommunity.security.UserContext;
import com.smartcommunity.service.ResidentQaService;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.concurrent.*;

@RestController
@RequestMapping("/api/v1/resident/qa")
@RequiredArgsConstructor
public class ResidentQaController {
    private final ResidentQaService qa;
    private final ExecutorService executor = new ThreadPoolExecutor(2, 2, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(8), runnable -> {
        Thread thread = new Thread(runnable, "resident-qa"); thread.setDaemon(true); return thread;
    });
    private final Map<String, Job> jobs = new HashMap<>();
    private record Job(Long userId, long createdAt, CompletableFuture<QaResponse> result) {}
    public record Question(String question) {}

    @GetMapping("/history")
    public Result<?> history(@RequestParam(defaultValue = "1") int page) { return Result.success(qa.history(UserContext.getUserId(), page)); }

    @PostMapping("/questions")
    public synchronized Result<Map<String, String>> ask(@RequestBody Question input) {
        Long userId = UserContext.getUserId();
        if (userId == null) throw new BusinessException(401, "请先登录");
        if (input.question() == null || input.question().isBlank() || input.question().length() > 2000) throw new BusinessException(400, "问题需为1到2000字");
        long now = System.currentTimeMillis();
        jobs.entrySet().removeIf(entry -> entry.getValue().result().isDone() && now - entry.getValue().createdAt() > 600000);
        if (jobs.size() >= 200 || jobs.values().stream().anyMatch(job -> job.userId().equals(userId) && (!job.result().isDone() || now - job.createdAt() < 3000))) throw new BusinessException(429, "请等待当前回答完成后再提问");
        String id = UUID.randomUUID().toString();
        try {
            CompletableFuture<QaResponse> result = CompletableFuture.supplyAsync(() -> qa.ask(input.question(), userId), executor);
            jobs.put(id, new Job(userId, now, result));
        } catch (RejectedExecutionException ex) { throw new BusinessException(429, "当前咨询较多，请稍后重试"); }
        return Result.success(Map.of("id", id));
    }
    @GetMapping("/questions/{id}")
    public synchronized Result<?> answer(@PathVariable String id) {
        Job job = jobs.get(id);
        if (job == null || !job.userId().equals(UserContext.getUserId())) throw new BusinessException(404, "问答不存在或已过期");
        if (!job.result().isDone()) return Result.success(Map.of("state", "pending"));
        try { return Result.success(Map.of("state", "completed", "response", job.result().join())); }
        catch (CompletionException ex) { return Result.success(Map.of("state", "failed", "message", "智能问答暂时不可用，请稍后重试")); }
    }
    @PreDestroy public void shutdown() { executor.shutdownNow(); }
}
