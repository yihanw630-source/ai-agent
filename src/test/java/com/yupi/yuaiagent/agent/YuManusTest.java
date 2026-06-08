package com.yupi.yuaiagent.agent;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@SpringBootTest(properties = {
        "spring.ai.mcp.client.enabled=false",
        "spring.main.lazy-initialization=true"
})
class YuManusTest {

    @Resource
    private YuManus yuManus;

    @Test
    void run() {
        String userPrompt= """
                我的另一半居住在上海，静安区，请帮我找到5公里内合适的约会地点，并结合一些网络图片，制定一份详细的约会计划，
                并以PDF的格式输出
                """;
        SseEmitter emitter = yuManus.run("");
        Assertions.assertNotNull(emitter);
    }
}
