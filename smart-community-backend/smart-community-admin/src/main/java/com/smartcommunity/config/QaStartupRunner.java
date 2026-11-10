package com.smartcommunity.config;

import com.smartcommunity.service.KnowledgeDocService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class QaStartupRunner implements ApplicationRunner {

    private final KnowledgeDocService knowledgeDocService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("系统启动：加载AI知识库到向量存储...");
        try {
            knowledgeDocService.reloadVectorStore();
            log.info("AI知识库向量化加载成功");
        } catch (Exception e) {
            log.warn("AI知识库加载失败（可能因未配置API Key或数据库未就绪）：{}", e.getMessage());
        }
    }
}
