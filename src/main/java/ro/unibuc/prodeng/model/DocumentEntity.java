package ro.unibuc.prodeng.model;

import jakarta.validation.constraints.NotBlank;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Document(collection = "files")
public record DocumentEntity(
        @Id String id,
        @NotBlank String documentGroupId,
        int version,
        @NotBlank String ownerId,
        @NotBlank String title,
        String content,
        @NotBlank String workspaceId,
        List<String> viewers,
        List<String> editors,
        @CreatedDate Instant createdAt) {
}
