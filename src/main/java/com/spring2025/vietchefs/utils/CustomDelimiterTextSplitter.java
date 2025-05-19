package com.spring2025.vietchefs.utils;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TextSplitter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CustomDelimiterTextSplitter extends  TextSplitter {

    private final String delimiter;

    public CustomDelimiterTextSplitter(String delimiter) {
        this.delimiter = delimiter;
    }

    @Override
    public List<Document> apply(List<Document> docs) {
        List<Document> result = new ArrayList<>();
        for (Document doc : docs) {
            String text = doc.getText();
            String[] parts = text.split(delimiter);
            for (String part : parts) {
                part = part.trim();
                if (!part.isEmpty()) {
                    Document chunk = new Document(part);
                    chunk.getMetadata().putAll(doc.getMetadata());
                    result.add(chunk);
                }
            }
        }
        return result;
    }

    @Override
    protected List<String> splitText(String text) {
        // Tách text theo delimiter rồi trim
        return Arrays.stream(text.split(delimiter))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

}