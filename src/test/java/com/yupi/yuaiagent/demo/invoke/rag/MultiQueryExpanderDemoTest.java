package com.yupi.yuaiagent.demo.invoke.rag;

import com.yupi.yuaiagent.YuAiAgentApplication;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.rag.Query;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest(classes = YuAiAgentApplication.class)
class MultiQueryExpanderDemoTest {

    @Resource
    private MultiQueryExpanderDemo multiQueryExpanderDemo;

    @Test
    void expand() {
        List<Query> queries = multiQueryExpanderDemo.expand("什么是程序员鱼皮？请回答我");
        Assertions.assertNotNull(queries);
    }
}
