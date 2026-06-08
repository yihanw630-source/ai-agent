package com.yupi.yuaiagent.app;

import com.yupi.yuaiagent.advisor.MyLoggerAdvisor;
import com.yupi.yuaiagent.rag.QueryRewriter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

@Component
@Slf4j
public class LoveApp {

    private static final String SYSTEM_PROMPT =
            "你是一名恋爱心理顾问。先向用户说明身份，再围绕单身、恋爱、已婚等情感问题提供建议。";

    private final ChatClient chatClient;

    @Autowired(required = false)
    @Qualifier("getVectorStore")
    private VectorStore pgVectorVectorStore;

    @Resource
    private QueryRewriter queryRewriter;

    @Resource
    private VectorStore loveAppVectorStore;

    @Resource
    private ToolCallback[] allTools;

    @Autowired(required = false)
    private ToolCallbackProvider toolCallbackProvider;

    public LoveApp(ChatModel dashscopeChatModel) {
        MessageWindowChatMemory memory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(10)
                .build();

        this.chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(memory).build(),
                        new MyLoggerAdvisor()
                )
                .build();
    }

    public String doChat(String message, String chatId) {
        ChatResponse chatResponse = chatClient.prompt()
                .user(message)
                .advisors(spec -> spec.param(CONVERSATION_ID, chatId))
                .call()
                .chatResponse();
        String text = null;
        if (chatResponse != null) {
            text = chatResponse.getResult().getOutput().getText();
        }
        log.info("context:{}", text);
        return text;
    }

    /**
     * AI基础对话（支持多轮对话记忆，SSE流式传输）
     * @param message
     * @param chatId
     * @return
     */
    public Flux<String> doChatByStream(String message, String chatId) {
        return chatClient.prompt()
                .user(message)
                .advisors(spec -> spec.param(CONVERSATION_ID, chatId))
                .stream()
                .content();


    }

    record LoveReport(String title, List<String> suggestions) {
    }

    public LoveReport doChatWithReport(String message, String chatId) {
        String rewrittenMessage = queryRewriter.doQueryRewrite(message);
        LoveReport loveReport = chatClient
                .prompt()
                .user(rewrittenMessage)
                .system(SYSTEM_PROMPT + " 每次对话后都输出恋爱建议报告，包含标题和建议列表。")
                .user(message)
                .advisors(spec -> spec.param(CONVERSATION_ID, chatId))
                .call()
                .entity(LoveReport.class);

        log.info("loveReport:{}", loveReport);
        return loveReport;
    }

    public String doChatWithRag(String message, String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, chatId))
                .user(message)
                .call()
                .chatResponse();

        String text = null;
        if (chatResponse != null) {
            text = chatResponse.getResult().getOutput().getText();
        }

        log.info("context:{}", text);
        return text;
    }

    public String doChatWithTools(String message, String chatId) {
        ChatResponse response = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CONVERSATION_ID, chatId))
                .advisors(new MyLoggerAdvisor())
                .toolCallbacks(allTools)
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    public String doChatWithMcp(String message, String chatId) {
        if (toolCallbackProvider == null) {
            throw new IllegalStateException("MCP client is disabled. Enable spring.ai.mcp.client.enabled and start the MCP server before calling this method.");
        }
        ChatResponse response = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CONVERSATION_ID, chatId))
                .advisors(new MyLoggerAdvisor())
                .toolCallbacks(toolCallbackProvider)
                .call()
                .chatResponse();
        String content = response.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }
}
