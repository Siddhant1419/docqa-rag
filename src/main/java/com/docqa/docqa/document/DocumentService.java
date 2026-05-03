package com.docqa.docqa.document;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentService {

    private final VectorStore vectorStore;

    public DocumentService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public IngestResult ingest(MultipartFile file) throws IOException {
        long startMs = System.currentTimeMillis();
        String docId = UUID.randomUUID().toString();

        // Wrap uploaded bytes into a Spring Resource
        var resource = new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename();
            }
        };

        // Read PDF page by page
        var config = PdfDocumentReaderConfig.builder()
                .withPagesPerDocument(1)
                .build();

        var reader = new PagePdfDocumentReader(resource, config);
        List<Document> pages = reader.get();

        // Tag every page with docId so we can filter later
        pages.forEach(p -> {
            p.getMetadata().put("docId", docId);
            p.getMetadata().put("filename", file.getOriginalFilename());
        });

        // Split pages into smaller token-aware chunks
        var splitter = new TokenTextSplitter();
        List<Document> chunks = splitter.apply(pages);

        // Embed each chunk and store in pgvector
        vectorStore.add(chunks);

        long elapsedMs = System.currentTimeMillis() - startMs;

        return new IngestResult(
                docId,
                file.getOriginalFilename(),
                pages.size(),
                chunks.size(),
                elapsedMs
        );
    }

    // Record = C# record — immutable data carrier, auto-generates
    // constructor, getters, equals, hashCode, toString
    public record IngestResult(
            String docId,
            String filename,
            int pageCount,
            int chunkCount,
            long ingestionTimeMs
    ) {}
}