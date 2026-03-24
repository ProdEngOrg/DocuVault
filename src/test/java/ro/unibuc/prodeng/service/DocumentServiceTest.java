package ro.unibuc.prodeng.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ro.unibuc.prodeng.exception.AccessDeniedException;
import ro.unibuc.prodeng.exception.EntityNotFoundException;
import ro.unibuc.prodeng.model.DocumentEntity;
import ro.unibuc.prodeng.model.UserEntity;
import ro.unibuc.prodeng.repository.DocumentRepository;
import ro.unibuc.prodeng.repository.UserRepository;
import ro.unibuc.prodeng.request.DocumentAddViewerRequest;
import ro.unibuc.prodeng.request.DocumentCreateRequest;
import ro.unibuc.prodeng.request.DocumentUpdateRequest;
import ro.unibuc.prodeng.response.DocumentResponse;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class DocumentServiceTest {

        @Mock
        private DocumentRepository documentRepository;

        @Mock
        private UserRepository userRepository;

        @InjectMocks
        private DocumentService documentService;

        @Test
        void testCreateDocument_validRequest_createsAndReturnsDocument() {
                // Arrange
                DocumentCreateRequest request = new DocumentCreateRequest("Project Plan",
                                "Some content", "workspace-1", List.of("viewer-1"), List.of("editor-1"));
                when(documentRepository.save(any(DocumentEntity.class))).thenAnswer(invocation -> {
                        DocumentEntity entity = invocation.getArgument(0);
                        return new DocumentEntity("generated-id", entity.documentGroupId(), entity.version(),
                                        entity.ownerId(), entity.title(), entity.content(), entity.workspaceId(),
                                        entity.viewers(), entity.editors(), Instant.now());
                });

                // Act
                DocumentResponse result = documentService.createDocument("owner-1", request);

                // Assert
                assertNotNull(result);
                assertEquals("generated-id", result.id());
                assertNotNull(result.documentGroupId());
                assertEquals(1, result.version());
                assertEquals("owner-1", result.ownerId());
                assertEquals("Project Plan", result.title());
                assertEquals("Some content", result.content());
                assertEquals("workspace-1", result.workspaceId());
                verify(documentRepository, times(1)).save(any(DocumentEntity.class));
        }

        @Test
        void testCreateDocument_nullViewersAndEditors_defaultsToEmptyLists() {
                // Arrange
                DocumentCreateRequest request = new DocumentCreateRequest("Title", "Content", "ws-1", null, null);
                when(documentRepository.save(any(DocumentEntity.class))).thenAnswer(invocation -> {
                        DocumentEntity entity = invocation.getArgument(0);
                        return new DocumentEntity("generated-id", entity.documentGroupId(), entity.version(),
                                        entity.ownerId(), entity.title(), entity.content(), entity.workspaceId(),
                                        entity.viewers(), entity.editors(), Instant.now());
                });

                // Act
                DocumentResponse result = documentService.createDocument("owner-1", request);

                // Assert
                assertNotNull(result);
                assertEquals(List.of(), result.viewers());
                assertEquals(List.of(), result.editors());
                verify(documentRepository, times(1)).save(any(DocumentEntity.class));
        }

        @Test
        void testGetLatestByGroupId_existingDocument_returnsDocument() {
                // Arrange
                DocumentEntity entity = new DocumentEntity("doc-1", "group-1", 2, "owner-1",
                                "Project Plan", "Content", "workspace-1",
                                List.of("viewer-1"), List.of("editor-1"), Instant.now());
                when(documentRepository.findTopByDocumentGroupIdOrderByVersionDesc("group-1"))
                                .thenReturn(Optional.of(entity));

                // Act
                DocumentResponse result = documentService.getLatestByGroupId("group-1");

                // Assert
                assertNotNull(result);
                assertEquals("doc-1", result.id());
                assertEquals("group-1", result.documentGroupId());
                assertEquals(2, result.version());
                assertEquals("Project Plan", result.title());
        }

        @Test
        void testGetLatestByGroupId_nonExistingDocument_throwsEntityNotFoundException() {
                // Arrange
                when(documentRepository.findTopByDocumentGroupIdOrderByVersionDesc("non-existing"))
                                .thenReturn(Optional.empty());

                // Act & Assert
                assertThrows(EntityNotFoundException.class, () -> documentService.getLatestByGroupId("non-existing"));
        }

        @Test
        void testGetByOwnerId_withMultipleDocuments_returnsAll() {
                // Arrange
                List<DocumentEntity> docs = Arrays.asList(
                                new DocumentEntity("doc-1", "group-1", 1, "owner-1", "Doc A", "Content A",
                                                "ws-1", List.of(), List.of(), Instant.now()),
                                new DocumentEntity("doc-2", "group-2", 1, "owner-1", "Doc B", "Content B",
                                                "ws-1", List.of(), List.of(), Instant.now()));
                when(documentRepository.findByOwnerId("owner-1")).thenReturn(docs);

                // Act
                List<DocumentResponse> result = documentService.getByOwnerId("owner-1");

                // Assert
                assertEquals(2, result.size());
                assertEquals("Doc A", result.get(0).title());
                assertEquals("Doc B", result.get(1).title());
        }

        @Test
        void testGetByOwnerId_withNoDocuments_returnsEmptyList() {
                // Arrange
                when(documentRepository.findByOwnerId("owner-1")).thenReturn(List.of());

                // Act
                List<DocumentResponse> result = documentService.getByOwnerId("owner-1");

                // Assert
                assertEquals(0, result.size());
        }

        @Test
        void testGetByWorkspaceId_withMultipleDocuments_returnsAll() {
                // Arrange
                List<DocumentEntity> docs = Arrays.asList(
                                new DocumentEntity("doc-1", "group-1", 1, "owner-1", "Doc A", "Content A",
                                                "ws-1", List.of(), List.of(), Instant.now()),
                                new DocumentEntity("doc-2", "group-2", 1, "owner-2", "Doc B", "Content B",
                                                "ws-1", List.of(), List.of(), Instant.now()));
                when(documentRepository.findByWorkspaceId("ws-1")).thenReturn(docs);

                // Act
                List<DocumentResponse> result = documentService.getByWorkspaceId("ws-1");

                // Assert
                assertEquals(2, result.size());
                assertEquals("Doc A", result.get(0).title());
                assertEquals("Doc B", result.get(1).title());
        }

        @Test
        void testGetByWorkspaceId_withNoDocuments_returnsEmptyList() {
                // Arrange
                when(documentRepository.findByWorkspaceId("ws-1")).thenReturn(List.of());

                // Act
                List<DocumentResponse> result = documentService.getByWorkspaceId("ws-1");

                // Assert
                assertEquals(0, result.size());
        }

        @Test
        void testUpdateDocument_asOwner_createsNewVersionSuccessfully() {
                // Arrange
                DocumentEntity existing = new DocumentEntity("doc-1", "group-1", 1, "owner-1",
                                "Old Title", "Old Content", "ws-1",
                                List.of("viewer-1"), List.of("editor-1"), Instant.now());
                when(documentRepository.findTopByDocumentGroupIdOrderByVersionDesc("group-1"))
                                .thenReturn(Optional.of(existing));
                when(documentRepository.save(any(DocumentEntity.class))).thenAnswer(invocation -> {
                        DocumentEntity entity = invocation.getArgument(0);
                        return new DocumentEntity("doc-2", entity.documentGroupId(), entity.version(),
                                        entity.ownerId(), entity.title(), entity.content(), entity.workspaceId(),
                                        entity.viewers(), entity.editors(), Instant.now());
                });

                DocumentUpdateRequest request = new DocumentUpdateRequest("New Title", "New Content",
                                "ws-1", List.of("viewer-1"), List.of("editor-1"));

                // Act
                DocumentResponse result = documentService.updateDocument("group-1", request, "owner-1");

                // Assert
                assertNotNull(result);
                assertEquals("doc-2", result.id());
                assertEquals("group-1", result.documentGroupId());
                assertEquals(2, result.version());
                assertEquals("New Title", result.title());
                assertEquals("New Content", result.content());
                verify(documentRepository, times(1)).save(any(DocumentEntity.class));
        }

        @Test
        void testUpdateDocument_asEditor_createsNewVersionSuccessfully() {
                // Arrange
                DocumentEntity existing = new DocumentEntity("doc-1", "group-1", 1, "owner-1",
                                "Old Title", "Old Content", "ws-1",
                                List.of(), List.of("editor-1"), Instant.now());
                when(documentRepository.findTopByDocumentGroupIdOrderByVersionDesc("group-1"))
                                .thenReturn(Optional.of(existing));
                when(documentRepository.save(any(DocumentEntity.class))).thenAnswer(invocation -> {
                        DocumentEntity entity = invocation.getArgument(0);
                        return new DocumentEntity("doc-2", entity.documentGroupId(), entity.version(),
                                        entity.ownerId(), entity.title(), entity.content(), entity.workspaceId(),
                                        entity.viewers(), entity.editors(), Instant.now());
                });

                DocumentUpdateRequest request = new DocumentUpdateRequest("Updated Title", null,
                                null, null, null);

                // Act
                DocumentResponse result = documentService.updateDocument("group-1", request, "editor-1");

                // Assert
                assertNotNull(result);
                assertEquals(2, result.version());
                assertEquals("Updated Title", result.title());
                assertEquals("Old Content", result.content());
        }

        @Test
        void testUpdateDocument_asWorkspaceMember_createsNewVersionSuccessfully() {
                // Arrange
                DocumentEntity existing = new DocumentEntity("doc-1", "group-1", 1, "owner-1",
                                "Title", "Content", "ws-1",
                                List.of(), List.of(), Instant.now());
                UserEntity workspaceMember = new UserEntity("user-2", "Member", "member@example.com",
                                List.of("ws-1"));
                when(documentRepository.findTopByDocumentGroupIdOrderByVersionDesc("group-1"))
                                .thenReturn(Optional.of(existing));
                when(userRepository.findById("user-2")).thenReturn(Optional.of(workspaceMember));
                when(documentRepository.save(any(DocumentEntity.class))).thenAnswer(invocation -> {
                        DocumentEntity entity = invocation.getArgument(0);
                        return new DocumentEntity("doc-2", entity.documentGroupId(), entity.version(),
                                        entity.ownerId(), entity.title(), entity.content(), entity.workspaceId(),
                                        entity.viewers(), entity.editors(), Instant.now());
                });

                DocumentUpdateRequest request = new DocumentUpdateRequest("New Title", "New Content",
                                null, null, null);

                // Act
                DocumentResponse result = documentService.updateDocument("group-1", request, "user-2");

                // Assert
                assertNotNull(result);
                assertEquals(2, result.version());
                assertEquals("New Title", result.title());
        }

        @Test
        void testUpdateDocument_unauthorizedUser_throwsAccessDeniedException() {
                // Arrange
                DocumentEntity existing = new DocumentEntity("doc-1", "group-1", 1, "owner-1",
                                "Title", "Content", "ws-1",
                                List.of(), List.of(), Instant.now());
                UserEntity outsider = new UserEntity("outsider", "Outsider", "outsider@example.com",
                                List.of("ws-other"));
                when(documentRepository.findTopByDocumentGroupIdOrderByVersionDesc("group-1"))
                                .thenReturn(Optional.of(existing));
                when(userRepository.findById("outsider")).thenReturn(Optional.of(outsider));

                DocumentUpdateRequest request = new DocumentUpdateRequest("Hack", null, null, null, null);

                // Act & Assert
                assertThrows(AccessDeniedException.class,
                                () -> documentService.updateDocument("group-1", request, "outsider"));
        }

        @Test
        void testUpdateDocument_nonExistingDocument_throwsEntityNotFoundException() {
                // Arrange
                when(documentRepository.findTopByDocumentGroupIdOrderByVersionDesc("non-existing"))
                                .thenReturn(Optional.empty());

                DocumentUpdateRequest request = new DocumentUpdateRequest("Title", null, null, null, null);

                // Act & Assert
                assertThrows(EntityNotFoundException.class,
                                () -> documentService.updateDocument("non-existing", request, "owner-1"));
        }

        @Test
        void testDeleteAllVersions_asOwner_deletesSuccessfully() {
                // Arrange
                List<DocumentEntity> versions = Arrays.asList(
                                new DocumentEntity("doc-2", "group-1", 2, "owner-1", "Title v2", "Content v2",
                                                "ws-1", List.of(), List.of(), Instant.now()),
                                new DocumentEntity("doc-1", "group-1", 1, "owner-1", "Title v1", "Content v1",
                                                "ws-1", List.of(), List.of(), Instant.now()));
                when(documentRepository.findByDocumentGroupIdOrderByVersionDesc("group-1"))
                                .thenReturn(versions);

                // Act
                documentService.deleteAllVersions("group-1", "owner-1");

                // Assert
                verify(documentRepository, times(1)).deleteAll(versions);
        }

        @Test
        void testDeleteAllVersions_nonOwner_throwsAccessDeniedException() {
                // Arrange
                List<DocumentEntity> versions = List.of(
                                new DocumentEntity("doc-1", "group-1", 1, "owner-1", "Title", "Content",
                                                "ws-1", List.of(), List.of(), Instant.now()));
                when(documentRepository.findByDocumentGroupIdOrderByVersionDesc("group-1"))
                                .thenReturn(versions);

                // Act & Assert
                assertThrows(AccessDeniedException.class,
                                () -> documentService.deleteAllVersions("group-1", "not-owner"));
        }

        @Test
        void testDeleteAllVersions_nonExistingDocument_throwsEntityNotFoundException() {
                // Arrange
                when(documentRepository.findByDocumentGroupIdOrderByVersionDesc("non-existing"))
                                .thenReturn(List.of());

                // Act & Assert
                assertThrows(EntityNotFoundException.class,
                                () -> documentService.deleteAllVersions("non-existing", "owner-1"));
        }

        @Test
        void testGetHistory_existingDocument_returnsAllVersions() {
                // Arrange
                List<DocumentEntity> versions = Arrays.asList(
                                new DocumentEntity("doc-2", "group-1", 2, "owner-1", "Title v2", "Content v2",
                                                "ws-1", List.of(), List.of(), Instant.now()),
                                new DocumentEntity("doc-1", "group-1", 1, "owner-1", "Title v1", "Content v1",
                                                "ws-1", List.of(), List.of(), Instant.now()));
                when(documentRepository.findByDocumentGroupIdOrderByVersionDesc("group-1"))
                                .thenReturn(versions);

                // Act
                List<DocumentResponse> result = documentService.getHistory("group-1");

                // Assert
                assertEquals(2, result.size());
                assertEquals(2, result.get(0).version());
                assertEquals(1, result.get(1).version());
        }

        @Test
        void testGetHistory_nonExistingDocument_throwsEntityNotFoundException() {
                // Arrange
                when(documentRepository.findByDocumentGroupIdOrderByVersionDesc("non-existing"))
                                .thenReturn(List.of());

                // Act & Assert
                assertThrows(EntityNotFoundException.class,
                                () -> documentService.getHistory("non-existing"));
        }

}
