package com.docqa.docqa.qa;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

@Service
public class QuestionService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    public QuestionService(ChatClient.Builder builder, VectorStore vectorStore) {
        this.chatClient = builder.build();
        this.vectorStore = vectorStore;
    }

    public AnswerResult ask(String question, String docId, int topK) {
        long startMs = System.currentTimeMillis();

        // Step 1: Retrieve relevant chunks from pgvector
        long retrievalStart = System.currentTimeMillis();
        List<Document> chunks = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(question)
                        .topK(topK)
                        .similarityThreshold(0.0)
                        .build()
        );
        long retrievalMs = System.currentTimeMillis() - retrievalStart;

        // Step 2: Build context string from chunks
        String context = chunks.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n---\n\n"));

        // Step 3: Build prompt manually with context injected
        String prompt = """
                Use the following context to answer the question.
                If the answer is not in the context, say exactly:
                "I don't know based on the provided documents."
                
                CONTEXT:
                %s
                
                QUESTION: %s
                """.formatted(context, question);

        // Step 4: Send to LLM
        long generationStart = System.currentTimeMillis();
        String answer = chatClient.prompt()
                .user(prompt)
                .call()
                .content();
        long generationMs = System.currentTimeMillis() - generationStart;

        long totalMs = System.currentTimeMillis() - startMs;

        return new AnswerResult(
                answer,
                chunks.size(),
                retrievalMs,
                generationMs,
                totalMs
        );
    }

    public record AnswerResult(
            String answer,
            int chunksRetrieved,
            long retrievalTimeMs,
            long generationTimeMs,
            long totalTimeMs
    ) {}
}