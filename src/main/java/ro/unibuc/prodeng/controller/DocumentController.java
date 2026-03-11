package ro.unibuc.prodeng.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.unibuc.prodeng.exception.EntityNotFoundException;
import ro.unibuc.prodeng.request.DocumentCreateRequest;
import ro.unibuc.prodeng.response.DocumentResponse;
import ro.unibuc.prodeng.service.DocumentService;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    @Autowired
    private DocumentService documentService;

    /**
     * Creates the first version of a new logical document.
     * Returns 201 Created with the full DocumentResponse.
     */
    @PostMapping
    public ResponseEntity<DocumentResponse> createDocument(
            @Valid @RequestBody DocumentCreateRequest request) {
        DocumentResponse response = documentService.createDocument(request);
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
}
