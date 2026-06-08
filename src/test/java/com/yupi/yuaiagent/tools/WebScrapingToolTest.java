package com.yupi.yuaiagent.tools;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WebScrapingToolTest {

    @Test
    void searchWeb() {
        WebScrapingTool webScrapingTool=new WebScrapingTool();
        String url="https://www.codefather.cn/";
        String result=webScrapingTool.scrapWebPage(url);
        Assertions.assertNotNull(result);
    }
}