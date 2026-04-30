package com.gfi.backend.models.dtos.evaluation;

import lombok.Data;

@Data
public class AiGenerateCommentResponse {
    @com.fasterxml.jackson.annotation.JsonProperty("comment_text")
    private String comment;
}
