/*package com.yupi.yuaiagent.rag;

import dev.langchain4j.data.document.Document;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class LoveAppDocumentLoader {

    private final ResourcePatternResolver resourcePatternResolver;

    public LoveAppDocumentLoader(ResourcePatternResolver resourcePatternResolver) {
        this.resourcePatternResolver = resourcePatternResolver;
    }

    public List<Document> loadMarkdown() {
        List<Document> allDocuments = new ArrayList<>();

        try {
            Resource[] resources =
                    resourcePatternResolver.getResources("classpath:document/*.md");

            for (Resource resource : resources) {
                String filename = resource.getFilename();

                MarkdownDocumentReaderConfig config =
                        MarkdownDocumentReaderConfig.builder()
                                .withHorizontalRuleCreateDocument(true)
                                .withIncludeCodeBlock(false)
                                .withIncludeBlockquote(false)
                                .withAdditionalMetadata("filename", "code.md")
                                .build();
            }

        } catch (IOException e) {
            log.error("Markdown文档加载失败",e);
        }

        return allDocuments;
    }
}
*/
package com.yupi.yuaiagent.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class LoveAppDocumentLoader {

    private final ResourcePatternResolver resourcePatternResolver;

    public LoveAppDocumentLoader(ResourcePatternResolver resourcePatternResolver) {
        this.resourcePatternResolver = resourcePatternResolver;
    }

    public List<Document> loadMarkdown() {
        List<Document> allDocuments = new ArrayList<>();

        try {
            Resource[] resources = resourcePatternResolver.getResources("classpath:document/*.md");

            for (Resource resource : resources) {
                String filename = resource.getFilename();
                String status=filename.substring(filename.length()-6,filename.length()-4);
                MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                        .withHorizontalRuleCreateDocument(true)
                        .withIncludeCodeBlock(false)
                        .withIncludeBlockquote(false)
                        .withAdditionalMetadata("filename", filename)
                        .withAdditionalMetadata("status",status)
                        .build();

                MarkdownDocumentReader reader = new MarkdownDocumentReader(resource, config);
                List<Document> documents = reader.get();

                allDocuments.addAll(documents);
            }

            return allDocuments;
        }
        catch (IOException e) {
            throw new RuntimeException("加载 Markdown 文档失败", e);
        }
    }
}