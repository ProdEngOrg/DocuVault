package ro.unibuc.prodeng.controller;

import com.fasterxml.jackson.databind.ObjectMapper;

import ro.unibuc.prodeng.IntegrationTestBase;
import ro.unibuc.prodeng.repository.UserRepository;
import ro.unibuc.prodeng.request.CreateUserRequest;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

@DisplayName("UserController Integration Tests")
class UserControllerIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    // clean database before each test
    @BeforeEach
    void cleanUp() {
        userRepository.deleteAll();
    }

    // tests go here

    private String createUser(String name, String email) throws Exception {
        CreateUserRequest request = new CreateUserRequest(name, email, List.of());

        String response = mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.id").exists())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("id").asText();
    }

    @Test
    void testCreateAndGetUser_validUserCreation_retrievesUserSuccessfully() throws Exception {
        // Arrange
        String userId = createUser("Alice", "alice@example.com");

        // Act & Assert
        mockMvc.perform(get("/api/users/" + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alice"))
                .andExpect(jsonPath("$.email").value("alice@example.com"));
    }

    @Test
    void testUpdateUserWithPut_validUserData_updatesUserSuccessfully() throws Exception {
        // Arrange
        String userId = createUser("Alice", "alice@example.com");

        // Act & Assert
        mockMvc.perform(put("/api/users/" + userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Alicia\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alicia"))
                .andExpect(jsonPath("$.email").value("alice@example.com"));
    }

    @Test
    void testGetAllUsers_multipleUsersExist_returnsAllUsers() throws Exception {
        // Arrange
        createUser("Alice", "alice@example.com");
        createUser("Bob", "bob@example.com");

        // Act & Assert
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void testChangeNameWithPatch_validNameChange_updatesNameSuccessfully() throws Exception {
        // Arrange
        String userId = createUser("Alice", "alice@example.com");

        // Act & Assert
        mockMvc.perform(patch("/api/users/" + userId + "/name")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Alicia\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alicia"))
                .andExpect(jsonPath("$.email").value("alice@example.com"));
    }

    @Test
    void testDeleteUser_existingUser_deletesSuccessfully() throws Exception {
        // Arrange
        String userId = createUser("Alice", "alice@example.com");

        // Act & Assert
        mockMvc.perform(delete("/api/users/" + userId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void testCreateUser_duplicateEmail_returnsBadRequest() throws Exception {
        // Arrange
        createUser("Alice", "alice@example.com");

        // Act & Assert
        CreateUserRequest duplicateRequest = new CreateUserRequest("Bob", "alice@example.com", List.of());
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Email already exists: alice@example.com"));
    }

}