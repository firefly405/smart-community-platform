package com.smartcommunity.service.impl;

import com.smartcommunity.dto.QaResponse;
import com.smartcommunity.entity.SysQaLog;
import com.smartcommunity.mapper.SysQaLogMapper;
import com.smartcommunity.service.QaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class QaServiceImpl implements QaService {

    private final ObjectProvider<ChatModel> chatModelProvider;
    private final ObjectProvider<SimpleVectorStore> vectorStoreProvider;
    private final SysQaLogMapper qaLogMapper;

    private static final String SYSTEM_PROMPT = """
            你是"阳光花园智慧社区AI助手"，请根据以下规则回答居民问题：
            1. 优先调用系统提供的函数工具查询实时数据（如车位、账单、报修、活动等），再根据查询结果回答
            2. 如果工具查询不到或问题属于社区规则类，则参考知识库信息回答
            3. 回答要简洁、准确、友好，使用中文
            4. 如果信息不足或不确定，请如实告知居民并建议联系物业客服 010-88881001
            5. 不要编造数据，所有数据必须从函数调用或知识库获取
            """;

    public QaServiceImpl(ObjectProvider<ChatModel> chatModelProvider,
                         ObjectProvider<SimpleVectorStore> vectorStoreProvider,
                         SysQaLogMapper qaLogMapper) {
        this.chatModelProvider = chatModelProvider;
        this.vectorStoreProvider = vectorStoreProvider;
        this.qaLogMapper = qaLogMapper;
    }

    @Override
    public List<String> askStream(String question, Long userId, String userName) {
        return null;
    }

    @Override
    public QaResponse askSync(String question, Long userId, String userName) {
        ChatModel chatModel = chatModelProvider.getIfAvailable();
        if (chatModel == null) {
            log.info("AI问答未启用：未创建 ChatModel");
            return unavailableResponse(question, userId, userName,
                    "AI服务未配置，请在系统管理中填写兼容 OpenAI 的 API 地址和 API Key，保存后重启服务。");
        }

        try {
            ChatClient.Builder clientBuilder = ChatClient.builder(chatModel)
                    .defaultSystem(SYSTEM_PROMPT)
                    .defaultFunctions(
                            "parkingQueryTool",
                            "billQueryTool",
                            "repairQueryTool",
                            "complaintQueryTool",
                            "noticeQueryTool",
                            "activityQueryTool",
                            "expressQueryTool",
                            "houseInfoQueryTool",
                            "visitorQueryTool",
                            "accessLogQueryTool",
                            "postQueryTool",
                            "secondhandQueryTool",
                            "voteQueryTool",
                            "equipmentQueryTool",
                            "alarmQueryTool",
                            "inspectionQueryTool",
                            "serviceListQueryTool",
                            "phoneQueryTool",
                            "productQueryTool",
                            "rentalQueryTool"
                    );
            SimpleVectorStore vectorStore = vectorStoreProvider.getIfAvailable();
            if (vectorStore != null) {
                clientBuilder.defaultAdvisors(new QuestionAnswerAdvisor(vectorStore));
            }
            String answer = clientBuilder.build()
                    .prompt()
                    .user(q -> q.text("用户（{user}）问：{question}")
                            .param("user", userName != null ? userName : "匿名居民")
                            .param("question", question))
                    .call()
                    .content();

            if (answer == null || answer.isBlank()) {
                answer = "抱歉，我暂时无法回答您的问题，建议联系物业客服 010-88881001 获取帮助。";
            }

            SysQaLog log = new SysQaLog();
            log.setQuestion(question);
            log.setAnswer(answer);
            log.setUserId(userId);
            log.setUserName(userName);
            qaLogMapper.insert(log);

            QaResponse resp = new QaResponse();
            resp.setId(log.getId());
            resp.setQuestion(question);
            resp.setAnswer(answer);
            resp.setUserId(userId);
            resp.setUserName(userName);
            resp.setCreateTime(log.getCreateTime());
            return resp;
        } catch (Exception e) {
            log.error("AI问答异常", e);
            String fallback = "抱歉，AI服务暂时不可用，请稍后再试或联系物业客服。错误：" + e.getMessage();
            return unavailableResponse(question, userId, userName, fallback);
        }
    }

    private QaResponse unavailableResponse(String question, Long userId, String userName, String answer) {
        QaResponse resp = new QaResponse();
        resp.setQuestion(question);
        resp.setAnswer(answer);
        resp.setUserId(userId);
        resp.setUserName(userName);
        return resp;
    }
}
