package com.ryan.media.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateMediaTaskRequest(
        @NotBlank(message = "title must not be blank")
        @Size(max = 100, message = "title length must be <= 100")
        String title,

        @NotBlank(message = "mediaType must not be blank")
        @Size(max = 30, message = "mediaType length must be <= 30")
        String mediaType
) {
}
