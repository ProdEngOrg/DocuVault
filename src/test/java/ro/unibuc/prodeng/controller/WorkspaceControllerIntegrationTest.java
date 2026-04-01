package ro.unibuc.prodeng.controller;

import com.fasterxml.jackson.databind.ObjectMapper;

import ro.unibuc.prodeng.IntegrationTestBase;
import ro.unibuc.prodeng.repository.UserRepository;
import ro.unibuc.prodeng.repository.WorkspaceRepository;
import ro.unibuc.prodeng.request.CreateUserRequest;
import ro.unibuc.prodeng.request.CreateWorkspaceRequest;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import java.util.ArrayList;
import java.util.List;

@DisplayName("WorkspaceController Integration Tests")
class WorkspaceControllerIntegrationTest extends IntegrationTestBase {

   @Autowired
   private MockMvc mockMvc;

   @Autowired
   private UserRepository userRepository;

   @Autowired
   private WorkspaceRepository workspaceRepository;

   @Autowired
   private ObjectMapper objectMapper;

   // clean database before each test
   @BeforeEach
   void cleanUp() {
      userRepository.deleteAll();
      workspaceRepository.deleteAll();
   }

   private String createUser(String name, String email, List<String> workspaces) throws Exception {
        CreateUserRequest userRequest = new CreateUserRequest(name, email, workspaces);
        String userResponse = mockMvc.perform(post("/api/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(userRequest)))
            .andReturn().getResponse().getContentAsString();
            
        return objectMapper.readTree(userResponse).get("id").asText();
   }
   
    private String createWorkspace(String name, String userId) throws Exception {
        CreateWorkspaceRequest request = new CreateWorkspaceRequest(name, userId);
        String workspaceResponse = mockMvc.perform(post("/api/workspaces")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.users", hasSize(1)))
                .andExpect(jsonPath("$.users[0]").value(userId))
                .andExpect(jsonPath("$.id").exists())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(workspaceResponse).get("id").asText();
    }

    @Test
    void testCreateAndGetWorkspace_validWorkspaceCreation_retrievesWorkspaceSuccessfully() throws Exception {
        // Arrange
        String userId = createUser("John Doe", "johndoe@example.com", new ArrayList<>());
        String workspaceId = createWorkspace("Test Workspace", userId);

        // Act & Assert
        mockMvc.perform(get("/api/workspaces/" + workspaceId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Test Workspace"))
            .andExpect(jsonPath("$.users", hasSize(1)))
            .andExpect(jsonPath("$.users[0]").value(userId));
    }
}