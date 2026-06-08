package com.yupi.yuaiagent.controller;

import com.alibaba.cloud.ai.dashscope.spec.DashScopeModel;
import com.yupi.yuaiagent.agent.YuManus;
import com.yupi.yuaiagent.app.LoveApp;
import jakarta.annotation.Resource;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/ai")
public class AiController {

    @Resource
    private LoveApp loveApp;

    @Resource
    private ToolCallback[] allTools;

    @Resource
    private ChatModel dashscopeChatModel;
    /**
     *  同步调用AI恋爱大师应用
     * @param message
     * @param chatId
     * @return
     */
    @GetMapping("/love_app/chat/sync")
    public String doChatWithLoveAppSync(String message,String chatId){
        return loveApp.doChat(message,chatId);

    }
    /**
     * SSE流式调用AI恋爱大师应用
     * @param message
     * @param chatId
     * @return
     */
    @GetMapping(value = "/love_app/chat/sse",produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatWithLoveAppSSE(String message, String chatId){
        return loveApp.doChatByStream(message,chatId);
    }
    @GetMapping("/manus/chat")
    public SseEmitter doChatWithManus(String message){
        YuManus yuManus=new YuManus(allTools,dashscopeChatModel);

        return yuManus.run(message);

    }

}
