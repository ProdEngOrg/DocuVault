package ro.unibuc.prodeng.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.unibuc.prodeng.exception.EntityNotFoundException;
import ro.unibuc.prodeng.request.DocumentCreateRequest;
import ro.unibuc.prodeng.request.DocumentUpdateRequest;
import ro.unibuc.prodeng.response.DocumentResponse;
import ro.unibuc.prodeng.service.DocumentService;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    @Autowired
    private DocumentService documentService;

    /**
     * Creates the first version of a new logical document.
     * The requesting user (X-User-Id) is automatically assigned as the owner.
     * Returns 201 Created with the full DocumentResponse.
     */
    @PostMapping
    public ResponseEntity<DocumentResponse> createDocument(
            @RequestHeader("X-User-Id") String currentUserId,
            @Valid @RequestBody DocumentCreateRequest request) throws EntityNotFoundException {
        DocumentResponse response = documentService.createDocument(currentUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Returns the latest version of a logical document by its documentGroupId.
     */
    @GetMapping("/{groupId}")
    public ResponseEntity<DocumentResponse> getLatestByGroupId(
            @PathVariable String groupId) throws EntityNotFoundException {
        return ResponseEntity.ok(documentService.getLatestByGroupId(groupId));
    }

    /**
     * Creates a new version of the document identified by documentGroupId.
     * The previous version is preserved; only a new record is inserted.
     * Requires X-User-Id header for permission check.
     */
    @PutMapping("/{groupId}")
    public ResponseEntity<DocumentResponse> updateDocument(
            @PathVariable String groupId,
            @RequestHeader("X-User-Id") String currentUserId,
            @Valid @RequestBody DocumentUpdateRequest request) throws EntityNotFoundException {
        DocumentResponse response = documentService.updateDocument(groupId, request, currentUserId);
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes ALL versions of a logical document identified by documentGroupId.
     * Only the document owner (X-User-Id) is allowed to delete.
     */
    @DeleteMapping("/{groupId}")
    public ResponseEntity<Void> deleteAllVersions(
            @PathVariable String groupId,
            @RequestHeader("X-User-Id") String currentUserId) throws EntityNotFoundException {
        documentService.deleteAllVersions(groupId, currentUserId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Returns all documents belonging to a given workspace.
     */
    @GetMapping({ "/workspace/{workspaceId}" })
    public ResponseEntity<List<DocumentResponse>> getByWorkspaceId(
            @PathVariable String workspaceId) throws EntityNotFoundException {
        return ResponseEntity.ok(documentService.getByWorkspaceId(workspaceId));
    }

    /**
     * Returns all documents owned by a given user.
     */
    @GetMapping({ "/owner/{ownerId}" })
    public ResponseEntity<List<DocumentResponse>> getByOwnerId(
            @PathVariable String ownerId) throws EntityNotFoundException {
        return ResponseEntity.ok(documentService.getByOwnerId(ownerId));
    }

}