package ro.unibuc.prodeng.controller;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.cucumber.java.BeforeAll;
import ro.unibuc.prodeng.exception.EntityNotFoundException;
import ro.unibuc.prodeng.request.ChangeNameRequest;
import ro.unibuc.prodeng.request.CreateUserRequest;
import ro.unibuc.prodeng.response.UserResponse;
import ro.unibuc.prodeng.response.WorkspaceStatisticsResponse;
import ro.unibuc.prodeng.service.DocumentService;
import ro.unibuc.prodeng.service.UserService;
import ro.unibuc.prodeng.service.WorkspaceService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
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
public class WorkspaceControllerTest {

    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private DocumentService documentService;

    @InjectMocks
    private WorkspaceController workspaceController;

    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(workspaceController).build();
    }

    @Test
    void testGetWorkspaceStatistics_withMultipleUsersAndDocuments_returnsUsersAndDocumentsCount() throws Exception {
        // Arrange
        String workspaceId = "workspace-1";
        when(workspaceService.getWorkspaceStatistics(workspaceId)).thenReturn(new WorkspaceStatisticsResponse("2", "3"));

        // Act & Assert
        mockMvc.perform(get("/api/workspaces/statistics/{id}", workspaceId)
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.users", is("2")))
            .andExpect(jsonPath("$.documents", is("3")));

            verify(workspaceService, times(1)).getWorkspaceStatistics(workspaceId);
    }
}
