package com.yupi.yuaiagent.rag;

import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

/*
创建自定义的RAG检索增强顾问的工厂
 */
public class LoveAppRagCustomAdvisorFactory {
    /**
     * 创建自定义的RAG检索增强顾问
     * @param vectorStore
     * @param status
     * @return
     */
    public static Advisor createLoveAppRagCustomAdvisor(VectorStore vectorStore,String status) {
//        过滤特定状态的文档
        Filter.Expression expression = new FilterExpressionBuilder()
                .eq("status", status)
                .build();

        DocumentRetriever documentRetriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .filterExpression(expression)  //过滤条件
                .similarityThreshold(0.5)   //相似度阈值
                .topK(3)    //返回文档数量
                .build();

        return RetrievalAugmentationAdvisor.builder()
//                文档检索器，自定义文档过滤条件
                .documentRetriever(documentRetriever)
//                文档增强器
               .queryAugmenter(LoveAppContextualQueryAugmenterFactory.createInstance())
                .build();
    }
}
