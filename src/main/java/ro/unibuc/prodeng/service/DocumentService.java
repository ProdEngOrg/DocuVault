package ro.unibuc.prodeng.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ro.unibuc.prodeng.exception.AccessDeniedException;
import ro.unibuc.prodeng.exception.EntityNotFoundException;
import ro.unibuc.prodeng.model.DocumentEntity;
import ro.unibuc.prodeng.model.UserEntity;
import ro.unibuc.prodeng.repository.DocumentRepository;
import ro.unibuc.prodeng.repository.UserRepository;
import ro.unibuc.prodeng.request.DocumentCreateRequest;
import ro.unibuc.prodeng.request.DocumentUpdateRequest;
import ro.unibuc.prodeng.response.DocumentResponse;

import java.util.List;
import java.util.UUID;

@Service
public class DocumentService {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private UserRepository userRepository;

    // ── Create ────────────────────────────────────────────────────────────────

    /**
     * Creates the first version (version = 1) of a new logical document.
     * A fresh documentGroupId is generated as the shared key for all future
     * versions.
     * The currentUserId is automatically set as the document owner.
     */
    public DocumentResponse createDocument(String currentUserId, DocumentCreateRequest request) {
        DocumentEntity entity = new DocumentEntity(
                null,
                UUID.randomUUID().toString(),
                1,
                currentUserId,
                request.title(),
                request.content(),
                request.workspaceId(),
                request.viewers() != null ? request.viewers() : List.of(),
                request.editors() != null ? request.editors() : List.of(),
                null);

        return toResponse(documentRepository.save(entity));
    }

    // ── Read (latest version) ─────────────────────────────────────────────────

    /**
     * Returns the latest version of the logical document identified by
     * documentGroupId.
     */
    public DocumentResponse getLatestByGroupId(String documentGroupId) {
        DocumentEntity entity = documentRepository
                .findTopByDocumentGroupIdOrderByVersionDesc(documentGroupId)
                .orElseThrow(() -> new EntityNotFoundException(documentGroupId));
        return toResponse(entity);
    }

    /**
     * Returns all documents owned by a given user (all versions included).
     */
    public List<DocumentResponse> getByOwnerId(String ownerId) {
        return documentRepository.findByOwnerId(ownerId).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Returns all documents in a workspace (all versions included).
     */
    public List<DocumentResponse> getByWorkspaceId(String workspaceId) {
        return documentRepository.findByWorkspaceId(workspaceId).stream()
                .map(this::toResponse)
                .toList();
    }

    // ── Update (new version) ──────────────────────────────────────────────────

    /**
     * Creates a new version of the document identified by documentGroupId.
     * The old version record is preserved intact; only a new record is inserted.
     *
     * Permission check (OR logic):
     * 1. currentUserId == document.ownerId
     * 2. document.editors contains currentUserId
     * 3. currentUser belongs to the same workspace as the document
     */
    public DocumentResponse updateDocument(String documentGroupId,
            DocumentUpdateRequest request,
            String currentUserId) {
        DocumentEntity latest = documentRepository
                .findTopByDocumentGroupIdOrderByVersionDesc(documentGroupId)
                .orElseThrow(() -> new EntityNotFoundException(documentGroupId));

        checkEditPermission(currentUserId, latest);

        DocumentEntity newVersion = new DocumentEntity(
                null,
                latest.documentGroupId(),
                latest.version() + 1,
                latest.ownerId(),
                request.title() != null ? request.title() : latest.title(),
                request.content() != null ? request.content() : latest.content(),
                latest.workspaceId(),
                request.viewers() != null ? request.viewers() : latest.viewers(),
                request.editors() != null ? request.editors() : latest.editors(),
                null);

        return toResponse(documentRepository.save(newVersion));
    }

    // ── Permission check ──────────────────────────────────────────────────────

    /**
     * Verifies that the given user has permission to edit the document.
     * Throws AccessDeniedException if none of the OR conditions are met.
     */
    private void checkEditPermission(String currentUserId, DocumentEntity document) {
        // 1. Is Owner
        if (currentUserId.equals(document.ownerId())) {
            return;
        }

        // 2. Is Editor
        if (document.editors() != null && document.editors().contains(currentUserId)) {
            return;
        }

        // 3. Same Workspace — user's workspace list contains the document's workspaceId
        UserEntity user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new EntityNotFoundException(currentUserId));
        if (user.workspaces() != null && user.workspaces().contains(document.workspaceId())) {
            return;
        }

        throw new AccessDeniedException(currentUserId, document.documentGroupId());
    }

    // ── Delete ────────────────────────────────────────────────────────────────
    /**
     * Deletes ALL versions of a logical document identified by documentGroupId.
     * Only the document owner is allowed to delete.
     */
    public void deleteAllVersions(String documentGroupId, String currentUserId) {
        List<DocumentEntity> versions = documentRepository.findByDocumentGroupIdOrderByVersionDesc(documentGroupId);
        if (versions.isEmpty()) {
            throw new EntityNotFoundException(documentGroupId);
        }

        // Only owner can delete
        DocumentEntity latest = versions.get(0);
        if (!currentUserId.equals(latest.ownerId())) {
            throw new AccessDeniedException(currentUserId, documentGroupId);
        }

        documentRepository.deleteAll(versions);
    }
    // ── Mapper ────────────────────────────────────────────────────────────────

    private DocumentResponse toResponse(DocumentEntity entity) {
        return new DocumentResponse(
                entity.id(),
                entity.documentGroupId(),
                entity.version(),
                entity.ownerId(),
                entity.title(),
                entity.content(),
                entity.workspaceId(),
                entity.viewers(),
                entity.editors(),
                entity.createdAt());
    }
}
