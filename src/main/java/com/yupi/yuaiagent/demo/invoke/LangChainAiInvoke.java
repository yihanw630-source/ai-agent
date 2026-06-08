package com.yupi.yuaiagent.demo.invoke;

import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;

public class LangChainAiInvoke {
    public static void main(String[] args) {
        ChatLanguageModel qwenChatModel = QwenChatModel.builder().apiKey(TestApiKey.API_KEY).modelName("qwen-max").build();
        String answer = qwenChatModel.chat("我是一个程序员，现在在进行一个恋爱智能体的项目。");
        System.out.println(answer);

    }
}
