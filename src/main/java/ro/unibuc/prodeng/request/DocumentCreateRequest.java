package ro.unibuc.prodeng.request;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record DocumentCreateRequest(

        @NotBlank(message = "Title is required")
        String title,

        /** Raw text content of the document. May be empty but not null. */
        String content,

        @NotBlank(message = "Workspace ID is required")
        String workspaceId,

        @NotBlank(message = "Owner ID is required")
        String ownerId,

        /** Optional list of user IDs granted read access. */
        List<String> viewers,

        /** Optional list of user IDs granted write access. */
        List<String> editors
) {}
