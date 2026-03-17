package ro.unibuc.prodeng.request;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record DocumentCreateRequest(

                @NotBlank(message = "Title is required") String title,

                @NotBlank String content,

                @NotBlank String workspaceId,

                List<String> viewers,
                List<String> editors) {
}
