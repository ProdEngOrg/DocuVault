package ro.unibuc.prodeng.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ro.unibuc.prodeng.IntegrationTestBase;
import ro.unibuc.prodeng.repository.DocumentRepository;
import ro.unibuc.prodeng.request.DocumentCreateRequest;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("DocumentController Integration Tests")
class DocumentControllerIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanUp() {
        documentRepository.deleteAll();
    }

    private String createDocument(String title, String content, String workspaceId, String ownerId) throws Exception {
        DocumentCreateRequest request = new DocumentCreateRequest(title, content, workspaceId, ownerId, List.of(), List.of());

        String response = mockMvc.perform(post("/api/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value(title))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.documentGroupId").exists())
                .andExpect(jsonPath("$.id").exists())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("documentGroupId").asText();
    }

    @Test
    void testCreateDocument_validRequest_returnsCreatedDocument() throws Exception {
        // Arrange
        DocumentCreateRequest request = new DocumentCreateRequest(
                "My Report", "Some content", "ws-1", "user-1", List.of(), List.of());

        // Act & Assert
        mockMvc.perform(post("/api/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("My Report"))
                .andExpect(jsonPath("$.content").value("Some content"))
                .andExpect(jsonPath("$.ownerId").value("user-1"))
                .andExpect(jsonPath("$.workspaceId").value("ws-1"))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.documentGroupId").exists())
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void testCreateDocument_missingTitle_returnsBadRequest() throws Exception {
        // Arrange — title is blank, which violates @NotBlank
        DocumentCreateRequest request = new DocumentCreateRequest(
                "", "Some content", "ws-1", "user-1", List.of(), List.of());

        // Act & Assert
        mockMvc.perform(post("/api/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetLatestByGroupId_existingDocument_returnsLatestVersion() throws Exception {
        // Arrange
        String groupId = createDocument("Doc Title", "Initial content", "ws-1", "user-1");

        // Act & Assert
        mockMvc.perform(get("/api/documents/" + groupId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Doc Title"))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.documentGroupId").value(groupId));
    }

    @Test
    void testGetLatestByGroupId_nonExistentGroupId_returnsNotFound() throws Exception {
        // Arrange
        String nonExistentGroupId = "non-existent-group-id";

        // Act & Assert
        mockMvc.perform(get("/api/documents/" + nonExistentGroupId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }
}
