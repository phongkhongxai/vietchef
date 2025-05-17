package com.spring2025.vietchefs.services.impl;

import com.spring2025.vietchefs.models.payload.responseModel.ChatboxResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ChatbotService {

    private final VectorStore vectorStore;
    private final ChatClient chatClient;

    @Value("classpath:/prompts/rag-prompt-template.st")
    private Resource ragPromptTemplate;

    @Autowired
    public ChatbotService(VectorStore vectorStore, ChatClient chatClient) {
        this.vectorStore = vectorStore;
        this.chatClient = chatClient;
    }
    private boolean isNegativeFeedback(String message) {
        List<String> negativeKeywords = List.of(
                //Vietnamese
                "không đúng", "không phải", "sai rồi", "không chính xác", "không hài lòng",
                "muốn gặp tư vấn viên", "muốn nói chuyện với người thật", "hỏi người thật",
                "hỏi nhân viên", "không ổn", "trả lời không đúng", "không thỏa đáng",
                "không hợp lý", "chưa đúng", "không giúp được", "không hiểu",
                "trả lời vòng vo", "không giải quyết được", "chưa đúng ý",
                "không đúng ý", "không liên quan", "không hữu ích", "không rõ ràng",
                "trả lời không như mong đợi", "muốn nói chuyện với người hỗ trợ",
                "nói chuyện với người thật", "muốn gặp người thật", "muốn gặp nhân viên",
                "nói chuyện với nhân viên", "trả lời không hợp lý",
                //Engrish
                "not correct", "wrong", "incorrect", "not satisfied", "not helpful",
                "talk to a human", "contact support", "need human support",
                "this is wrong", "this is not accurate", "talk to admin",
                "customer support", "real person", "unsatisfied", "not clear",
                "doesn't help", "unrelated", "want human", "need help",
                "i want to talk to a person", "agent please", "this is confusing",
                "this isn't working", "not what i meant", "don't understand",
                "i need support", "connect me to a real person",
                "speak to someone", "i want real help"
        );
        return negativeKeywords.stream()
                .anyMatch(word -> message.toLowerCase().contains(word));
    }
    private boolean isEnglish(String message) {
        List<String> englishKeywords = List.of(
                "not correct", "wrong", "incorrect", "not satisfied", "not helpful",
                "talk to a human", "contact support", "need human support",
                "this is wrong", "this is not accurate", "talk to admin",
                "customer support", "real person", "unsatisfied", "not clear",
                "doesn't help", "unrelated", "want human", "need help",
                "i want to talk to a person", "agent please", "this is confusing",
                "this isn't working", "not what i meant", "don't understand",
                "i need support", "connect me to a real person",
                "speak to someone", "i want real help"
        );
        String lower = message.toLowerCase();
        return englishKeywords.stream().anyMatch(lower::contains);
    }


    public ChatboxResponse processMessage(String message) {
        if (isNegativeFeedback(message)) {
            String reply;
            if (isEnglish(message)) {
                reply = "The answer might not fully meet your expectations. Would you like to chat with a support agent for further assistance?";
            } else {
                reply = "Thông tin vừa rồi có thể chưa hoàn toàn đúng ý bạn. Bạn có muốn trò chuyện với tư vấn viên để được hỗ trợ thêm không?";
            }
            return new ChatboxResponse(reply, true);
        }

        List<Document> similarDocuments = vectorStore.similaritySearch(
                SearchRequest.builder().query(message).topK(2).build());

        String context = similarDocuments.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n"));

        PromptTemplate promptTemplate = new PromptTemplate(ragPromptTemplate);
        Map<String, Object> promptParameters = new HashMap<>();
        promptParameters.put("input", message);
        promptParameters.put("documents", context);
        Prompt prompt = promptTemplate.create(promptParameters);

        String result = chatClient.prompt(prompt).call().content();
        return new ChatboxResponse(result, false);
    }
}