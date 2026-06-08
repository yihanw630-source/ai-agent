package com.yupi.yuaiagent.rag;

import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgDistanceType.COSINE_DISTANCE;
import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgIndexType.HNSW;

@Configuration
@ConditionalOnProperty(prefix = "app.rag.pgvector", name = "enabled", havingValue = "true")
public class PgVectorVectorStoreConfig {

    private static final int DASH_SCOPE_MAX_EMBEDDING_BATCH_SIZE = 10;

    @Resource
    private LoveAppDocumentLoader loveAppDocumentLoader;

    @Value("${app.rag.initialize-on-startup:false}")
    private boolean initializeOnStartup;

    @Bean
    public VectorStore getVectorStore(JdbcTemplate jdbcTemplate, EmbeddingModel dashscopeEmbeddingModel) {
        VectorStore pgVectorStore = PgVectorStore.builder(jdbcTemplate, dashscopeEmbeddingModel)
                .distanceType(COSINE_DISTANCE)
                .indexType(HNSW)
                .initializeSchema(true)
                .schemaName("public")
                .vectorTableName("vector_store")
                .maxDocumentBatchSize(DASH_SCOPE_MAX_EMBEDDING_BATCH_SIZE)
                .build();

        if (!initializeOnStartup) {
            return pgVectorStore;
        }

        List<Document> documents = loveAppDocumentLoader.loadMarkdown();
        for (int i = 0; i < documents.size(); i += DASH_SCOPE_MAX_EMBEDDING_BATCH_SIZE) {
            int end = Math.min(i + DASH_SCOPE_MAX_EMBEDDING_BATCH_SIZE, documents.size());
            pgVectorStore.add(documents.subList(i, end));
        }
        return pgVectorStore;
    }
}
