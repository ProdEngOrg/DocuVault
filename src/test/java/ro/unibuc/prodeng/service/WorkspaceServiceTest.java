package ro.unibuc.prodeng.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ro.unibuc.prodeng.model.DocumentEntity;
import ro.unibuc.prodeng.model.UserEntity;
import ro.unibuc.prodeng.model.WorkspaceEntity;
import ro.unibuc.prodeng.repository.DocumentRepository;
import ro.unibuc.prodeng.repository.UserRepository;
import ro.unibuc.prodeng.repository.WorkspaceRepository;
import ro.unibuc.prodeng.request.CreateUserRequest;
import ro.unibuc.prodeng.request.CreateWorkspaceRequest;
import ro.unibuc.prodeng.response.DocumentResponse;
import ro.unibuc.prodeng.response.UserResponse;
import ro.unibuc.prodeng.response.WorkspaceStatisticsResponse;
import ro.unibuc.prodeng.exception.EntityNotFoundException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
public class WorkspaceServiceTest {
    
    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private DocumentService documentService;

    @InjectMocks
    private WorkspaceService workspaceService;

    @Test
    void testGetWorkspaceStatistics_withMultipleUsersAndDocuments_returnsUsersAndDocumentsCount() throws EntityNotFoundException {
        // Arrange
        String workspaceId = "workspace-1";
        WorkspaceEntity workspace = new WorkspaceEntity(workspaceId, "Workspace Name", List.of("user-1", "user-2"));
        Instant instant = Instant.now();
        List<DocumentResponse> documents = new ArrayList<>(List.of(
            new DocumentResponse("document-1", "group-1", 1, "owner-1", "Title", "Content", workspaceId, List.of(), List.of(), instant),
            new DocumentResponse("document-2", "group-2", 1, "owner-1", "Title", "Content", workspaceId, List.of(), List.of(), instant),
            new DocumentResponse("document-3", "group-3", 1, "owner-2", "Title", "Content", workspaceId, List.of(), List.of(), instant)));
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));
        when(documentService.getByWorkspaceId(workspaceId)).thenReturn(documents);

        // Act
        WorkspaceStatisticsResponse result = workspaceService.getWorkspaceStatistics(workspaceId);

        // Assert
        assertEquals("2", result.users());
        assertEquals("3", result.documents());
    }
}
