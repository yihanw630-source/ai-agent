package com.yupi.yuaiagent.agent;

import cn.hutool.core.util.StrUtil;
import com.yupi.yuaiagent.agent.model.AgentState;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Base class for step-based agents.
 */
@Data
@Slf4j
public abstract class BaseAgent {

    private String name;

    private String systemPrompt;
    private String nextStepPrompt;

    private AgentState state = AgentState.IDLE;

    private int currentStep = 0;
    private int maxSteps = 5;

    private ChatClient chatClient;

    private List<Message> messageList = new ArrayList<>();

    /**
     * Run the agent asynchronously and stream step results through SSE.
     *
     * @param userPrompt user input
     * @return SSE emitter
     */
    public SseEmitter run(String userPrompt) {
        SseEmitter sseEmitter = new SseEmitter(300000L);

        CompletableFuture.runAsync(() -> {
            try {
                if (this.state != AgentState.IDLE) {
                    sseEmitter.send("错误：无法从当前状态运行 Agent：" + this.state);
                    sseEmitter.complete();
                    return;
                }
                if (StrUtil.isBlank(userPrompt)) {
                    sseEmitter.send("错误：用户提示词不能为空");
                    sseEmitter.complete();
                    return;
                }

                this.state = AgentState.RUNNING;
                this.currentStep = 0;
                this.messageList.add(new UserMessage(userPrompt));

                for (int i = 0; i < maxSteps && state != AgentState.FINISHED; i++) {
                    int stepNumber = i + 1;
                    currentStep = stepNumber;
                    log.info("Executing step {}/{} for {}", stepNumber, maxSteps, name);

                    String stepResult = step();
                    log.info("Step {} result: {}", stepNumber, stepResult);
                }

                if (currentStep >= maxSteps && state != AgentState.FINISHED) {
                    state = AgentState.FINISHED;
                    sseEmitter.send("执行结束：达到最大步数 " + maxSteps);
                } else {
                    sseEmitter.send(getFinalResponse());
                }

                sseEmitter.complete();
            } catch (Exception e) {
                state = AgentState.ERROR;
                log.error("Error executing agent", e);
                sendError(sseEmitter, e);
            } finally {
                cleanUp();
            }
        });

        sseEmitter.onTimeout(() -> {
            this.state = AgentState.ERROR;
            this.cleanUp();
            log.warn("SSE connection timeout");
        });
        sseEmitter.onCompletion(() -> {
            if (this.state == AgentState.RUNNING) {
                this.state = AgentState.FINISHED;
            }
            this.cleanUp();
            log.info("SSE connection complete");
        });
        return sseEmitter;
    }

    private void sendError(SseEmitter sseEmitter, Exception e) {
        try {
            sseEmitter.send("执行错误：" + e.getMessage());
            sseEmitter.complete();
        } catch (IOException ex) {
            sseEmitter.completeWithError(ex);
        }
    }

    /**
     * Execute one agent step.
     *
     * @return step result
     */
    public abstract String step();

    /**
     * Get the final assistant response that should be sent to the user.
     *
     * @return final user-facing response
     */
    public String getFinalResponse() {
        for (int i = messageList.size() - 1; i >= 0; i--) {
            Message message = messageList.get(i);
            if (message instanceof AssistantMessage assistantMessage && StrUtil.isNotBlank(assistantMessage.getText())) {
                return assistantMessage.getText();
            }
        }
        return "任务已执行完成，但没有生成最终回复。";
    }

    /**
     * Release resources after execution.
     */
    public void cleanUp() {
        // Subclasses can override this method.
    }
}
