package ro.unibuc.prodeng.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ro.unibuc.prodeng.exception.EntityNotFoundException;
import ro.unibuc.prodeng.model.DocumentEntity;
import ro.unibuc.prodeng.repository.DocumentRepository;
import ro.unibuc.prodeng.request.DocumentCreateRequest;
import ro.unibuc.prodeng.request.DocumentUpdateRequest;
import ro.unibuc.prodeng.response.DocumentResponse;

import java.util.List;
import java.util.UUID;

@Service
public class DocumentService {

    @Autowired
    private DocumentRepository documentRepository;

    // ── Create ────────────────────────────────────────────────────────────────

    /**
     * Creates the first version (version = 1) of a new logical document.
     * A fresh documentGroupId is generated as the shared key for all future versions.
     */
    public DocumentResponse createDocument(DocumentCreateRequest request) {
        DocumentEntity entity = new DocumentEntity(
                null,
                UUID.randomUUID().toString(),
                1,
                request.ownerId(),
                request.title(),
                request.content(),
                request.workspaceId(),
                request.viewers() != null ? request.viewers() : List.of(),
                request.editors() != null ? request.editors() : List.of(),
                null
        );

        return toResponse(documentRepository.save(entity));
    }

    // ── Read (latest version) ─────────────────────────────────────────────────

    /**
     * Returns the latest version of the logical document identified by documentGroupId.
     */
    public DocumentResponse getLatestByGroupId(String documentGroupId) {
        DocumentEntity entity = documentRepository
                .findTopByDocumentGroupIdOrderByVersionDesc(documentGroupId)
                .orElseThrow(() -> new EntityNotFoundException(documentGroupId));
        return toResponse(entity);
    }

    /**
     * Returns a specific version by its own MongoDB document id.
     */
    public DocumentResponse getById(String id) {
        DocumentEntity entity = documentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(id));
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
     */
    public DocumentResponse updateDocument(String documentGroupId, DocumentUpdateRequest request) {
        DocumentEntity latest = documentRepository
                .findTopByDocumentGroupIdOrderByVersionDesc(documentGroupId)
                .orElseThrow(() -> new EntityNotFoundException(documentGroupId));

        DocumentEntity newVersion = new DocumentEntity(
                null,
                latest.documentGroupId(),
                latest.version() + 1,
                latest.ownerId(),
                request.title()   != null ? request.title()   : latest.title(),
                request.content() != null ? request.content() : latest.content(),
                latest.workspaceId(),
                request.viewers() != null ? request.viewers() : latest.viewers(),
                request.editors() != null ? request.editors() : latest.editors(),
                null
        );

        return toResponse(documentRepository.save(newVersion));
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    /**
     * Deletes a specific version record by its MongoDB document id.
     */
    public void deleteById(String id) {
        if (!documentRepository.existsById(id)) {
            throw new EntityNotFoundException(id);
        }
        documentRepository.deleteById(id);
    }

    /**
     * Deletes ALL versions of a logical document identified by documentGroupId.
     */
    public void deleteAllVersions(String documentGroupId) {
        List<DocumentEntity> versions =
                documentRepository.findByDocumentGroupIdOrderByVersionDesc(documentGroupId);
        if (versions.isEmpty()) {
            throw new EntityNotFoundException(documentGroupId);
        }
        documentRepository.deleteAll(versions);
    }

    // ── Version history ───────────────────────────────────────────────────────

    /**
     * Returns all versions of a logical document, newest first.
     */
    public List<DocumentResponse> getHistory(String documentGroupId) {
        List<DocumentEntity> versions =
                documentRepository.findByDocumentGroupIdOrderByVersionDesc(documentGroupId);
        if (versions.isEmpty()) {
            throw new EntityNotFoundException(documentGroupId);
        }
        return versions.stream().map(this::toResponse).toList();
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
                entity.createdAt()
        );
    }
}
