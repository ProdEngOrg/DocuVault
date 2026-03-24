package ro.unibuc.prodeng.controller;

import com.fasterxml.jackson.databind.ObjectMapper;

import ro.unibuc.prodeng.exception.EntityNotFoundException;
import ro.unibuc.prodeng.request.DocumentAddViewerRequest;
import ro.unibuc.prodeng.request.DocumentCreateRequest;
import ro.unibuc.prodeng.request.DocumentUpdateRequest;
import ro.unibuc.prodeng.response.DocumentResponse;
import ro.unibuc.prodeng.service.DocumentService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@ExtendWith(SpringExtension.class)
class DocumentControllerTest {

        @Mock
        private DocumentService documentService;

        @InjectMocks
        private DocumentController documentController;

        private MockMvc mockMvc;

        private ObjectMapper objectMapper = new ObjectMapper();

        private DocumentResponse testDoc1 = new DocumentResponse("doc-1", "group-1", 1, "owner-1",
                        "Project Plan", "Content of project plan", "workspace-1",
                        List.of("viewer-1"), List.of("editor-1"), Instant.parse("2026-01-01T00:00:00Z"));
        private DocumentResponse testDoc2 = new DocumentResponse("doc-2", "group-2", 1, "owner-1",
                        "Design Document", "Content of design document", "workspace-1",
                        List.of("viewer-2"), List.of("editor-2"), Instant.parse("2026-01-02T00:00:00Z"));
        private DocumentCreateRequest createRequest = new DocumentCreateRequest("Project Plan",
                        "Content of project plan", "workspace-1", List.of("viewer-1"), List.of("editor-1"));
        private DocumentUpdateRequest updateRequest = new DocumentUpdateRequest("Updated Title",
                        "Updated content", "workspace-1", List.of("viewer-1"), List.of("editor-1"));
        private DocumentAddViewerRequest addViewerRequest = new DocumentAddViewerRequest("viewer-2", "group-1");

        @BeforeEach
        void setUp() {
                mockMvc = MockMvcBuilders.standaloneSetup(documentController).build();
        }

        @Test
        void testCreateDocument_validRequestProvided_createsAndReturnsDocument() throws Exception {
                // Arrange
                when(documentService.createDocument(eq("owner-1"), any(DocumentCreateRequest.class)))
                                .thenReturn(testDoc1);

                // Act & Assert
                mockMvc.perform(post("/api/documents")
                                .header("X-User-Id", "owner-1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(createRequest)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id", is("doc-1")))
                                .andExpect(jsonPath("$.documentGroupId", is("group-1")))
                                .andExpect(jsonPath("$.version", is(1)))
                                .andExpect(jsonPath("$.ownerId", is("owner-1")))
                                .andExpect(jsonPath("$.title", is("Project Plan")))
                                .andExpect(jsonPath("$.content", is("Content of project plan")))
                                .andExpect(jsonPath("$.workspaceId", is("workspace-1")));

                verify(documentService, times(1)).createDocument(eq("owner-1"), any(DocumentCreateRequest.class));
        }

        @Test
        void testCreateDocument_nonExistingOwner_returnsNotFound() throws Exception {
                // Arrange
                when(documentService.createDocument(eq("non-existing"), any(DocumentCreateRequest.class)))
                                .thenThrow(new EntityNotFoundException("User"));

                // Act & Assert
                mockMvc.perform(post("/api/documents")
                                .header("X-User-Id", "non-existing")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(createRequest)))
                                .andExpect(status().isNotFound());

                verify(documentService, times(1)).createDocument(eq("non-existing"), any(DocumentCreateRequest.class));
        }

        @Test
        void testGetLatestByGroupId_existingDocument_returnsDocument() throws Exception {
                // Arrange
                String groupId = "group-1";
                when(documentService.getLatestByGroupId(groupId)).thenReturn(testDoc1);

                // Act & Assert
                mockMvc.perform(get("/api/documents/{groupId}", groupId)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id", is("doc-1")))
                                .andExpect(jsonPath("$.documentGroupId", is("group-1")))
                                .andExpect(jsonPath("$.version", is(1)))
                                .andExpect(jsonPath("$.ownerId", is("owner-1")))
                                .andExpect(jsonPath("$.title", is("Project Plan")))
                                .andExpect(jsonPath("$.content", is("Content of project plan")))
                                .andExpect(jsonPath("$.workspaceId", is("workspace-1")));

                verify(documentService, times(1)).getLatestByGroupId(groupId);
        }

        @Test
        void testGetLatestByGroupId_nonExistingDocument_returnsNotFound() throws Exception {
                // Arrange
                String groupId = "non-existing";
                when(documentService.getLatestByGroupId(groupId))
                                .thenThrow(new EntityNotFoundException("Document"));

                // Act & Assert
                mockMvc.perform(get("/api/documents/{groupId}", groupId)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isNotFound());

                verify(documentService, times(1)).getLatestByGroupId(groupId);
        }

        @Test
        void testUpdateDocument_validRequestProvided_updatesAndReturnsDocument() throws Exception {
                // Arrange
                String groupId = "group-1";
                DocumentResponse updatedDoc = new DocumentResponse("doc-3", "group-1", 2, "owner-1",
                                "Updated Title", "Updated content", "workspace-1",
                                List.of("viewer-1"), List.of("editor-1"), Instant.parse("2026-01-03T00:00:00Z"));
                when(documentService.updateDocument(eq(groupId), any(DocumentUpdateRequest.class), eq("owner-1")))
                                .thenReturn(updatedDoc);

                // Act & Assert
                mockMvc.perform(put("/api/documents/{groupId}", groupId)
                                .header("X-User-Id", "owner-1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateRequest)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id", is("doc-3")))
                                .andExpect(jsonPath("$.documentGroupId", is("group-1")))
                                .andExpect(jsonPath("$.version", is(2)))
                                .andExpect(jsonPath("$.title", is("Updated Title")))
                                .andExpect(jsonPath("$.content", is("Updated content")));

                verify(documentService, times(1)).updateDocument(eq(groupId), any(DocumentUpdateRequest.class),
                                eq("owner-1"));
        }

        @Test
        void testUpdateDocument_nonExistingDocument_returnsNotFound() throws Exception {
                // Arrange
                String groupId = "non-existing";
                when(documentService.updateDocument(eq(groupId), any(DocumentUpdateRequest.class), anyString()))
                                .thenThrow(new EntityNotFoundException("Document"));

                // Act & Assert
                mockMvc.perform(put("/api/documents/{groupId}", groupId)
                                .header("X-User-Id", "owner-1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateRequest)))
                                .andExpect(status().isNotFound());

                verify(documentService, times(1)).updateDocument(eq(groupId), any(DocumentUpdateRequest.class),
                                eq("owner-1"));
        }

        @Test
        void testDeleteAllVersions_existingDocument_returnsNoContent() throws Exception {
                // Arrange
                String groupId = "group-1";
                doNothing().when(documentService).deleteAllVersions(groupId, "owner-1");

                // Act & Assert
                mockMvc.perform(delete("/api/documents/{groupId}", groupId)
                                .header("X-User-Id", "owner-1")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isNoContent());

                verify(documentService, times(1)).deleteAllVersions(groupId, "owner-1");
        }

        @Test
        void testDeleteAllVersions_nonExistingDocument_returnsNotFound() throws Exception {
                // Arrange
                String groupId = "non-existing";
                doThrow(new EntityNotFoundException("Document"))
                                .when(documentService).deleteAllVersions(groupId, "owner-1");

                // Act & Assert
                mockMvc.perform(delete("/api/documents/{groupId}", groupId)
                                .header("X-User-Id", "owner-1")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isNotFound());

                verify(documentService, times(1)).deleteAllVersions(groupId, "owner-1");
        }

        @Test
        void testGetByWorkspaceId_withMultipleDocuments_returnsList() throws Exception {
                // Arrange
                String workspaceId = "workspace-1";
                List<DocumentResponse> docs = Arrays.asList(testDoc1, testDoc2);
                when(documentService.getByWorkspaceId(workspaceId)).thenReturn(docs);

                // Act & Assert
                mockMvc.perform(get("/api/documents/workspace/{workspaceId}", workspaceId)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(2)))
                                .andExpect(jsonPath("$[0].id", is("doc-1")))
                                .andExpect(jsonPath("$[0].title", is("Project Plan")))
                                .andExpect(jsonPath("$[0].workspaceId", is("workspace-1")))
                                .andExpect(jsonPath("$[1].id", is("doc-2")))
                                .andExpect(jsonPath("$[1].title", is("Design Document")))
                                .andExpect(jsonPath("$[1].workspaceId", is("workspace-1")));

                verify(documentService, times(1)).getByWorkspaceId(workspaceId);
        }

        @Test
        void testGetByWorkspaceId_withNoDocuments_returnsEmptyList() throws Exception {
                // Arrange
                String workspaceId = "workspace-1";
                when(documentService.getByWorkspaceId(workspaceId)).thenReturn(Arrays.asList());

                // Act & Assert
                mockMvc.perform(get("/api/documents/workspace/{workspaceId}", workspaceId)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(0)));

                verify(documentService, times(1)).getByWorkspaceId(workspaceId);
        }

        @Test
        void testGetByOwnerId_withMultipleDocuments_returnsList() throws Exception {
                // Arrange
                String ownerId = "owner-1";
                List<DocumentResponse> docs = Arrays.asList(testDoc1, testDoc2);
                when(documentService.getByOwnerId(ownerId)).thenReturn(docs);

                // Act & Assert
                mockMvc.perform(get("/api/documents/owner/{ownerId}", ownerId)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(2)))
                                .andExpect(jsonPath("$[0].id", is("doc-1")))
                                .andExpect(jsonPath("$[0].title", is("Project Plan")))
                                .andExpect(jsonPath("$[0].ownerId", is("owner-1")))
                                .andExpect(jsonPath("$[1].id", is("doc-2")))
                                .andExpect(jsonPath("$[1].title", is("Design Document")))
                                .andExpect(jsonPath("$[1].ownerId", is("owner-1")));

                verify(documentService, times(1)).getByOwnerId(ownerId);
        }

        @Test
        void testGetByOwnerId_withNoDocuments_returnsEmptyList() throws Exception {
                // Arrange
                String ownerId = "owner-1";
                when(documentService.getByOwnerId(ownerId)).thenReturn(Arrays.asList());

                // Act & Assert
                mockMvc.perform(get("/api/documents/owner/{ownerId}", ownerId)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(0)));

                verify(documentService, times(1)).getByOwnerId(ownerId);
        }

}