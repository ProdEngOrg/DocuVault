package ro.unibuc.prodeng.controller;

import com.fasterxml.jackson.databind.ObjectMapper;

import ro.unibuc.prodeng.IntegrationTestBase;
import ro.unibuc.prodeng.model.DocumentEntity;
import ro.unibuc.prodeng.model.UserEntity;
import ro.unibuc.prodeng.repository.DocumentRepository;
import ro.unibuc.prodeng.repository.UserRepository;
import ro.unibuc.prodeng.request.DocumentAddViewerRequest;
import ro.unibuc.prodeng.request.DocumentCreateRequest;
import ro.unibuc.prodeng.request.DocumentUpdateRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("DocumentController Integration Tests")
class DocumentControllerIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanUp() {
        documentRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    /**
     * Creates a document via the API and returns its documentGroupId.
     * Mirrors the UserControllerIntegrationTest pattern of driving setup
     * through the real HTTP layer so the DB state is always consistent.
     */
    private String createDocument(String ownerId, String title, String content, String workspaceId) throws Exception {
        DocumentCreateRequest request = new DocumentCreateRequest(title, content, workspaceId, List.of(), List.of());

        String responseBody = mockMvc.perform(post("/api/documents")
                        .header("X-User-Id", ownerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value(title))
                .andExpect(jsonPath("$.ownerId").value(ownerId))
                .andExpect(jsonPath("$.documentGroupId").exists())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(responseBody).get("documentGroupId").asText();
    }

    // ── POST /api/documents ───────────────────────────────────────────────────

    @Test
    void testCreateDocument_validRequest_persistsDocumentInMongoDB() throws Exception {
        // Act
        String groupId = createDocument("owner-1", "My Doc", "Some content", "ws-1");

        // Assert – verify the document actually landed in MongoDB
        List<DocumentEntity> saved = documentRepository.findByDocumentGroupIdOrderByVersionDesc(groupId);
        assertFalse(saved.isEmpty(), "Document should be persisted in MongoDB");
        DocumentEntity entity = saved.get(0);
        assertEquals("My Doc", entity.title());
        assertEquals("Some content", entity.content());
        assertEquals("owner-1", entity.ownerId());
        assertEquals("ws-1", entity.workspaceId());
        assertEquals(1, entity.version());
    }

    @Test
    void testCreateDocument_nullViewersAndEditors_persistsEmptyListsInMongoDB() throws Exception {
        DocumentCreateRequest request = new DocumentCreateRequest("Title", "Content", "ws-1", null, null);

        String responseBody = mockMvc.perform(post("/api/documents")
                        .header("X-User-Id", "owner-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.viewers", hasSize(0)))
                .andExpect(jsonPath("$.editors", hasSize(0)))
                .andReturn().getResponse().getContentAsString();

        String groupId = objectMapper.readTree(responseBody).get("documentGroupId").asText();

        // Assert – MongoDB stores empty lists, not nulls
        DocumentEntity entity = documentRepository
                .findTopByDocumentGroupIdOrderByVersionDesc(groupId).orElseThrow();
        assertNotNull(entity.viewers());
        assertNotNull(entity.editors());
        assertTrue(entity.viewers().isEmpty());
        assertTrue(entity.editors().isEmpty());
    }

    // ── GET /api/documents/{groupId} ──────────────────────────────────────────

    @Test
    void testGetLatestByGroupId_existingDocument_returnsLatestVersionFromMongoDB() throws Exception {
        // Arrange – create two versions via the API
        String groupId = createDocument("owner-1", "v1 Title", "v1 content", "ws-1");
        DocumentUpdateRequest update = new DocumentUpdateRequest("v2 Title", "v2 content", null, null, null);
        mockMvc.perform(put("/api/documents/{groupId}", groupId)
                        .header("X-User-Id", "owner-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk());

        // Act & Assert – GET returns version 2
        mockMvc.perform(get("/api/documents/{groupId}", groupId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version", is(2)))
                .andExpect(jsonPath("$.title", is("v2 Title")));

        // Assert – MongoDB actually holds both versions (history preserved)
        List<DocumentEntity> allVersions = documentRepository.findByDocumentGroupIdOrderByVersionDesc(groupId);
        assertEquals(2, allVersions.size(), "Both versions should be persisted in MongoDB");
    }

    @Test
    void testGetLatestByGroupId_nonExistingDocument_returns404() throws Exception {
        mockMvc.perform(get("/api/documents/{groupId}", "non-existing-group"))
                .andExpect(status().isNotFound());
    }

    // ── PUT /api/documents/{groupId} ──────────────────────────────────────────

    @Test
    void testUpdateDocument_asOwner_insertsNewVersionInMongoDB() throws Exception {
        // Arrange
        String groupId = createDocument("owner-1", "Original", "Old content", "ws-1");

        // Act
        DocumentUpdateRequest update = new DocumentUpdateRequest("Updated", "New content", null, null, null);
        mockMvc.perform(put("/api/documents/{groupId}", groupId)
                        .header("X-User-Id", "owner-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version", is(2)))
                .andExpect(jsonPath("$.title", is("Updated")));

        // Assert – MongoDB has v1 (old, preserved) AND v2 (new)
        List<DocumentEntity> versions = documentRepository.findByDocumentGroupIdOrderByVersionDesc(groupId);
        assertEquals(2, versions.size(), "Old version must be preserved, new version inserted");
        assertEquals(2, versions.get(0).version());
        assertEquals("Updated", versions.get(0).title());
        assertEquals(1, versions.get(1).version());
        assertEquals("Original", versions.get(1).title());
    }

    @Test
    void testUpdateDocument_asEditor_insertsNewVersionInMongoDB() throws Exception {
        // Arrange – create doc with editor-1 in editors list
        DocumentCreateRequest req = new DocumentCreateRequest(
                "Original", "Content", "ws-1", List.of(), List.of("editor-1"));
        String responseBody = mockMvc.perform(post("/api/documents")
                        .header("X-User-Id", "owner-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String groupId = objectMapper.readTree(responseBody).get("documentGroupId").asText();

        // Act – editor-1 updates the document
        DocumentUpdateRequest update = new DocumentUpdateRequest("Editor Updated", null, null, null, null);
        mockMvc.perform(put("/api/documents/{groupId}", groupId)
                        .header("X-User-Id", "editor-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version", is(2)));

        // Assert – new version in DB, old content preserved (partial update)
        DocumentEntity latest = documentRepository
                .findTopByDocumentGroupIdOrderByVersionDesc(groupId).orElseThrow();
        assertEquals("Editor Updated", latest.title());
        assertEquals("Content", latest.content()); // unchanged field falls back
    }

    @Test
    void testUpdateDocument_asWorkspaceMember_insertsNewVersionInMongoDB() throws Exception {
        // Arrange – save a user who belongs to ws-1 (the document's workspace)
        userRepository.save(new UserEntity("member-1", "Charlie", "charlie@example.com", List.of("ws-1")));
        String groupId = createDocument("owner-1", "Original", "Content", "ws-1");

        // Act – workspace member updates the document
        DocumentUpdateRequest update = new DocumentUpdateRequest("Member Updated", null, null, null, null);
        mockMvc.perform(put("/api/documents/{groupId}", groupId)
                        .header("X-User-Id", "member-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version", is(2)));

        // Assert – version 2 is in MongoDB
        assertEquals(2, documentRepository.findByDocumentGroupIdOrderByVersionDesc(groupId).size());
    }

    @Test
    void testUpdateDocument_asUnauthorizedUser_returns403AndDoesNotModifyMongoDB() throws Exception {
        // Arrange – other-1 is in ws-other, document is ws-1
        userRepository.save(new UserEntity("other-1", "Bob", "bob@example.com", List.of("ws-other")));
        String groupId = createDocument("owner-1", "Original", "Content", "ws-1");

        // Act
        DocumentUpdateRequest update = new DocumentUpdateRequest("Hacked", null, null, null, null);
        mockMvc.perform(put("/api/documents/{groupId}", groupId)
                        .header("X-User-Id", "other-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isForbidden());

        // Assert – MongoDB is untouched; still only 1 version
        List<DocumentEntity> versions = documentRepository.findByDocumentGroupIdOrderByVersionDesc(groupId);
        assertEquals(1, versions.size(), "No new version should be created after a failed update");
        assertEquals("Original", versions.get(0).title());
    }

    @Test
    void testUpdateDocument_nonExistingDocument_returns404() throws Exception {
        DocumentUpdateRequest update = new DocumentUpdateRequest("Title", null, null, null, null);
        mockMvc.perform(put("/api/documents/{groupId}", "non-existing-group")
                        .header("X-User-Id", "owner-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isNotFound());
    }

    // ── PUT /api/documents/add-viewer ─────────────────────────────────────────

    @Test
    void testAddViewer_asOwner_persistsViewerInMongoDB() throws Exception {
        // Arrange
        String groupId = createDocument("owner-1", "Doc", "Content", "ws-1");
        userRepository.save(new UserEntity("viewer-1", "Dave", "dave@example.com", List.of()));
        DocumentAddViewerRequest request = new DocumentAddViewerRequest("viewer-1", groupId);

        // Act
        mockMvc.perform(put("/api/documents/add-viewer")
                        .header("X-User-Id", "owner-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.viewers", hasItem("viewer-1")));

        // Assert – viewer persisted in MongoDB
        DocumentEntity entity = documentRepository
                .findTopByDocumentGroupIdOrderByVersionDesc(groupId).orElseThrow();
        assertTrue(entity.viewers().contains("viewer-1"), "viewer-1 must be stored in MongoDB");
    }

    @Test
    void testAddViewer_asNotOwner_returns403AndDoesNotModifyMongoDB() throws Exception {
        // Arrange
        String groupId = createDocument("owner-1", "Doc", "Content", "ws-1");
        userRepository.save(new UserEntity("not-owner", "Eve", "eve@example.com", List.of()));
        userRepository.save(new UserEntity("viewer-1", "Dave", "dave@example.com", List.of()));
        DocumentAddViewerRequest request = new DocumentAddViewerRequest("viewer-1", groupId);

        // Act
        mockMvc.perform(put("/api/documents/add-viewer")
                        .header("X-User-Id", "not-owner")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        // Assert – viewers list unchanged in MongoDB
        DocumentEntity entity = documentRepository
                .findTopByDocumentGroupIdOrderByVersionDesc(groupId).orElseThrow();
        assertFalse(entity.viewers().contains("viewer-1"),
                "Viewer must not be added when requester is not the owner");
    }

    @Test
    void testAddViewer_viewerDoesNotExist_returns404AndDoesNotModifyMongoDB() throws Exception {
        // Arrange
        String groupId = createDocument("owner-1", "Doc", "Content", "ws-1");
        DocumentAddViewerRequest request = new DocumentAddViewerRequest("ghost-user", groupId);

        // Act
        mockMvc.perform(put("/api/documents/add-viewer")
                        .header("X-User-Id", "owner-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());

        // Assert – document viewers unchanged in MongoDB
        DocumentEntity entity = documentRepository
                .findTopByDocumentGroupIdOrderByVersionDesc(groupId).orElseThrow();
        assertTrue(entity.viewers().isEmpty(), "No viewer should be added for a non-existing user");
    }

    // ── DELETE /api/documents/{groupId} ───────────────────────────────────────

    @Test
    void testDeleteAllVersions_asOwner_removesAllVersionsFromMongoDB() throws Exception {
        // Arrange – create two versions
        String groupId = createDocument("owner-1", "v1", "Content", "ws-1");
        DocumentUpdateRequest update = new DocumentUpdateRequest("v2", null, null, null, null);
        mockMvc.perform(put("/api/documents/{groupId}", groupId)
                        .header("X-User-Id", "owner-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk());

        // Confirm both versions exist in MongoDB before deletion
        assertEquals(2, documentRepository.findByDocumentGroupIdOrderByVersionDesc(groupId).size());

        // Act
        mockMvc.perform(delete("/api/documents/{groupId}", groupId)
                        .header("X-User-Id", "owner-1"))
                .andExpect(status().isNoContent());

        // Assert – all versions removed from MongoDB
        List<DocumentEntity> remaining = documentRepository.findByDocumentGroupIdOrderByVersionDesc(groupId);
        assertTrue(remaining.isEmpty(), "All versions must be deleted from MongoDB");
    }

    @Test
    void testDeleteAllVersions_asNonOwner_returns403AndKeepsDocumentInMongoDB() throws Exception {
        // Arrange
        String groupId = createDocument("owner-1", "Doc", "Content", "ws-1");
        userRepository.save(new UserEntity("other-1", "Frank", "frank@example.com", List.of()));

        // Act
        mockMvc.perform(delete("/api/documents/{groupId}", groupId)
                        .header("X-User-Id", "other-1"))
                .andExpect(status().isForbidden());

        // Assert – document still in MongoDB
        assertFalse(documentRepository.findByDocumentGroupIdOrderByVersionDesc(groupId).isEmpty(),
                "Document must not be deleted when requester is not the owner");
    }

    @Test
    void testDeleteAllVersions_nonExistingDocument_returns404() throws Exception {
        mockMvc.perform(delete("/api/documents/{groupId}", "non-existing-group")
                        .header("X-User-Id", "owner-1"))
                .andExpect(status().isNotFound());
    }

    // ── GET /api/documents/workspace/{workspaceId} ────────────────────────────

    @Test
    void testGetByWorkspaceId_returnsOnlyDocumentsFromThatWorkspace() throws Exception {
        // Arrange – two docs in ws-1, one in ws-2
        createDocument("owner-1", "Doc A", "Content", "ws-1");
        createDocument("owner-1", "Doc B", "Content", "ws-1");
        createDocument("owner-1", "Doc C", "Content", "ws-2");

        // Assert via HTTP
        mockMvc.perform(get("/api/documents/workspace/{workspaceId}", "ws-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].workspaceId", everyItem(is("ws-1"))));

        // Assert via MongoDB
        List<DocumentEntity> ws1Docs = documentRepository.findByWorkspaceId("ws-1");
        assertEquals(2, ws1Docs.size());
    }

    @Test
    void testGetByWorkspaceId_noDocuments_returnsEmptyListAndMongoDBIsEmpty() throws Exception {
        mockMvc.perform(get("/api/documents/workspace/{workspaceId}", "ws-empty"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        assertTrue(documentRepository.findByWorkspaceId("ws-empty").isEmpty());
    }

    // ── GET /api/documents/owner/{ownerId} ────────────────────────────────────

    @Test
    void testGetByOwnerId_returnsOnlyDocumentsOfThatOwner() throws Exception {
        // Arrange – two docs for owner-1, one for owner-2
        createDocument("owner-1", "Doc A", "Content", "ws-1");
        createDocument("owner-1", "Doc B", "Content", "ws-1");
        createDocument("owner-2", "Doc C", "Content", "ws-1");

        // Assert via HTTP
        mockMvc.perform(get("/api/documents/owner/{ownerId}", "owner-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].ownerId", everyItem(is("owner-1"))));

        // Assert via MongoDB
        List<DocumentEntity> owner1Docs = documentRepository.findByOwnerId("owner-1");
        assertEquals(2, owner1Docs.size());
    }

    @Test
    void testGetByOwnerId_noDocuments_returnsEmptyListAndMongoDBIsEmpty() throws Exception {
        mockMvc.perform(get("/api/documents/owner/{ownerId}", "unknown-owner"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        assertTrue(documentRepository.findByOwnerId("unknown-owner").isEmpty());
    }
}
