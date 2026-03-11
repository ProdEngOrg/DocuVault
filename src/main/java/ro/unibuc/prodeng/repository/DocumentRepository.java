package ro.unibuc.prodeng.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import ro.unibuc.prodeng.model.DocumentEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends MongoRepository<DocumentEntity, String> {

    // ── Versioning ───────────────────────────────────────────────────────────

    /**
     * Returns all versions of a logical document, newest first.
     * Used by the /history endpoint.
     */
    List<DocumentEntity> findByDocumentGroupIdOrderByVersionDesc(String documentGroupId);

    /**
     * Returns the single latest version of a logical document.
     * Used by GET /documents/{groupId} and as the base for a new version on UPDATE.
     */
    Optional<DocumentEntity> findTopByDocumentGroupIdOrderByVersionDesc(String documentGroupId);

    // ── Ownership & workspace ────────────────────────────────────────────────

    /** All documents owned by a user (latest and historical versions). */
    List<DocumentEntity> findByOwnerId(String ownerId);

    /** All documents in a workspace. */
    List<DocumentEntity> findByWorkspaceId(String workspaceId);

    /** All documents in a workspace owned by a specific user. */
    List<DocumentEntity> findByWorkspaceIdAndOwnerId(String workspaceId, String ownerId);

    // ── Viewer / editor access ───────────────────────────────────────────────

    /** Documents a given user has been granted viewer access to. */
    List<DocumentEntity> findByViewersContaining(String userId);

    /** Documents a given user has been granted editor access to. */
    List<DocumentEntity> findByEditorsContaining(String userId);
}
