package com.spring2025.vietchefs.configs;

import com.spring2025.vietchefs.utils.CustomDelimiterTextSplitter;
import org.apache.log4j.Logger;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.retry.support.RetryTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Configuration
public class RagConfiguration {

    private static Logger logger = Logger.getLogger(RagConfiguration.class);
    @Value("classpath:/docs/vietchef-vn.txt")
    private Resource vietchefVn;
    @Value("classpath:/docs/vietchef-en.txt")
    private Resource vietchefEn;

    @Value("vectorstore.json")
    private String vectorStoreName;

    @Bean
    public RetryTemplate retryTemplate() {
        return RetryTemplate.builder()
                .exponentialBackoff(1000, 1.5, 5000)
                .maxAttempts(5)
                .build();
    }

    @Bean
    @Primary
    VectorStore simpleVectorStore(EmbeddingModel embeddingModel, RetryTemplate retryTemplate) throws IOException {
        SimpleVectorStore simpleVectorStore = SimpleVectorStore.builder(embeddingModel).build();
        try {
            File vectorStoreFile = getVectorStoreFile();

            if (vectorStoreFile.exists()) {
                simpleVectorStore.load(vectorStoreFile);
                logger.info("Vector Store File Loaded.");

            } else {
                logger.info("Vector Store File Does Not Exist, loading documents");
                TextSplitter textSplitter = new CustomDelimiterTextSplitter("@@##");

                // Load tiếng Việt
                loadAndAddDocumentsWithRetry(vietchefVn, textSplitter, simpleVectorStore, retryTemplate);

                // Load tiếng Anh
                loadAndAddDocumentsWithRetry(vietchefEn, textSplitter, simpleVectorStore, retryTemplate);

                simpleVectorStore.save(vectorStoreFile);
            }
        } catch (Exception e) {
            logger.error("Error loading vector store", e);
        }
        return simpleVectorStore;
    }

    private void loadAndAddDocumentsWithRetry(Resource resource, TextSplitter splitter,
                                              SimpleVectorStore store, RetryTemplate retryTemplate) throws IOException, InterruptedException {

        TextReader reader = new TextReader(resource);
        reader.getCustomMetadata().put("filename", resource.getFilename());
        List<Document> docs = reader.get();
        List<Document> chunks = splitter.apply(docs);

        // In ra để kiểm tra
        logger.info("Tổng số chunk sau khi tách: " + chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            Document chunk = chunks.get(i);
            logger.info("Chunk " + (i + 1) + ": " + chunk.getText());
        }

        for (Document chunk : chunks) {
            retryTemplate.execute(context -> {
                try {
                    store.add(List.of(chunk)); // Gọi từng cái một
                    Thread.sleep(1000); // Delay nhỏ để tránh overload API
                } catch (Exception e) {
                    logger.warn("Thử lại vì lỗi: " + e.getMessage());
                    throw e; // Phải throw ra để RetryTemplate biết là lỗi
                }
                return null;
            });
        }
    }


    private File getVectorStoreFile() {
        Path path = Paths.get("src", "main", "resources", "data");
        String absolutePath = path.toFile().getAbsolutePath() + "/" + vectorStoreName;
        return new File(absolutePath);
    }
}