package ro.unibuc.prodeng.model;

import jakarta.validation.constraints.NotBlank;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Document(collection = "files")
public class DocumentEntity {

    @Id
    private String id;

    /**
     * Groups all versions of the same logical document together.
     * All versions share the same documentGroupId; each has a unique id.
     */
    @NotBlank
    private String documentGroupId;

    /** Monotonically increasing version counter: 1 → 2 → 3 … */
    private int version;

    /** The user who owns (created) this document. */
    @NotBlank
    private String ownerId;

    @NotBlank
    private String title;

    /** Raw text or base64-encoded file content. */
    private String content;

    @NotBlank
    private String workspaceId;

    /** User IDs that may read this document. */
    private List<String> viewers;

    /** User IDs that may update this document (creating a new version). */
    private List<String> editors;

    /** Set automatically by Spring Data MongoDB auditing on insert. */
    @CreatedDate
    private Instant createdAt;

    // ── Constructors ─────────────────────────────────────────────────────────

    public DocumentEntity() {
    }

    public DocumentEntity(String id, String documentGroupId, int version, String ownerId,
            String title, String content, String workspaceId,
            List<String> viewers, List<String> editors, Instant createdAt) {
        this.id = id;
        this.documentGroupId = documentGroupId;
        this.version = version;
        this.ownerId = ownerId;
        this.title = title;
        this.content = content;
        this.workspaceId = workspaceId;
        this.viewers = viewers;
        this.editors = editors;
        this.createdAt = createdAt;
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDocumentGroupId() {
        return documentGroupId;
    }

    public void setDocumentGroupId(String documentGroupId) {
        this.documentGroupId = documentGroupId;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
    }

    public List<String> getViewers() {
        return viewers;
    }

    public void setViewers(List<String> viewers) {
        this.viewers = viewers;
    }

    public List<String> getEditors() {
        return editors;
    }

    public void setEditors(List<String> editors) {
        this.editors = editors;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    // ── equals / hashCode / toString ─────────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof DocumentEntity that))
            return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "DocumentEntity{" +
                "id='" + id + '\'' +
                ", documentGroupId='" + documentGroupId + '\'' +
                ", version=" + version +
                ", ownerId='" + ownerId + '\'' +
                ", title='" + title + '\'' +
                ", workspaceId='" + workspaceId + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
