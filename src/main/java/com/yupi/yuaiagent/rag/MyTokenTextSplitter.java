package com.yupi.yuaiagent.rag;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Component;

import java.util.List;

//自定义基于token的切词器

@Component
class MyTokenTextSplitter {
    public List<Document> splitDocuments(List<Document> documents) {
        TokenTextSplitter splitter = new TokenTextSplitter();
        return splitter.apply(documents);
    }

    public List<Document> splitCustomized(List<Document> documents) {
        TokenTextSplitter splitter = new TokenTextSplitter(
                200,   // chunkSize
                100,   // minChunkSizeChars
                10,    // minChunkLengthToEmbed
                5000,  // maxNumChunks
                true,  // keepSeparator
                List.of('.', '?', '!', '\n', '。', '；', '：')
        );
        return splitter.apply(documents);
    }
}
