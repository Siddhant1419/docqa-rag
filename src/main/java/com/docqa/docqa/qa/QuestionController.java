package com.docqa.docqa.qa;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/ask")
public class QuestionController {

    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @PostMapping
    public QuestionService.AnswerResult ask(
            @RequestBody @Valid AskRequest request) {
        int topK = request.topK() == null ? 5 : request.topK();
        return questionService.ask(request.question(), request.docId(), topK);
    }

    public record AskRequest(
            @NotBlank @Size(min = 3, max = 500) String question,
            String docId,
            @Min(1) @Max(20) Integer topK
    ) {}
}