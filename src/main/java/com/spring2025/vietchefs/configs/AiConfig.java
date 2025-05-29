package com.spring2025.vietchefs.configs;

import com.google.cloud.aiplatform.v1.PredictionServiceSettings;
import com.google.cloud.vertexai.VertexAI;
import jakarta.annotation.PreDestroy;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vertexai.embedding.VertexAiEmbeddingConnectionDetails;
import org.springframework.ai.vertexai.embedding.text.VertexAiTextEmbeddingModel;
import org.springframework.ai.vertexai.embedding.text.VertexAiTextEmbeddingOptions;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class AiConfig {
    @Value("${spring.ai.vertex.ai.embedding.project-id}")
    private String projectId;

    @Value("${spring.ai.vertex.ai.embedding.location}")
    private String location;

    @Value("${spring.ai.vertex.ai.embedding.text.options.model}")
    private String embeddingModelName;

    @Value("${spring.ai.vertex.ai.gemini.model-name}")
    private String geminiModelName;

    private VertexAI vertexAIClient;


    @Bean
    public PredictionServiceSettings predictionServiceSettings() throws IOException {
        return PredictionServiceSettings.newBuilder()
                .setEndpoint("us-central1-aiplatform.googleapis.com:443")
                .build();
    }


    @Bean
    public VertexAI vertexAI() throws Exception {
        vertexAIClient = new VertexAI(projectId, location);
        return vertexAIClient;
    }

    @Bean
    public VertexAiGeminiChatOptions vertexAiGeminiChatOptions() {
        return VertexAiGeminiChatOptions.builder().model(geminiModelName).build();
    }

    @Bean
    public VertexAiGeminiChatModel vertexAiGeminiChatModel(VertexAI vertexAI, VertexAiGeminiChatOptions options) {
        return new VertexAiGeminiChatModel(vertexAI, options);
    }

    @Bean
    public ChatClient chatClient(VertexAiGeminiChatModel vertexAiChatModel) {
        return ChatClient.create(vertexAiChatModel);
    }

    @Bean
    public VertexAiTextEmbeddingOptions vertexAiTextEmbeddingOptions() {
        return VertexAiTextEmbeddingOptions.builder()
                .model(embeddingModelName)
                .build();
    }

    @Bean
    public EmbeddingModel embeddingModel(PredictionServiceSettings predictionServiceSettings) {
        VertexAiEmbeddingConnectionDetails connectionDetails =
                VertexAiEmbeddingConnectionDetails.builder()
                        .projectId(projectId)
                        .location(location)
                        .predictionServiceSettings(predictionServiceSettings)
                        .build();

        VertexAiTextEmbeddingOptions options = VertexAiTextEmbeddingOptions.builder()
                .model(embeddingModelName)
                .build();

        return new VertexAiTextEmbeddingModel(connectionDetails, options);
    }


    @PreDestroy
    public void shutdownVertexAI() {
        if (vertexAIClient != null) {
            vertexAIClient.close();
        }
    }
}