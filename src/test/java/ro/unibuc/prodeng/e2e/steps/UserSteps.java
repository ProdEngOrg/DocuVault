package ro.unibuc.prodeng.e2e.steps;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import ro.unibuc.prodeng.request.ChangeNameRequest;
import ro.unibuc.prodeng.request.CreateUserRequest;
import ro.unibuc.prodeng.response.UserResponse;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;

public class UserSteps {

    private static final String BASE_URL = "http://localhost:8080";

    private final RestTemplate restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory());
    private final ObjectMapper objectMapper = new ObjectMapper();

    private ResponseEntity<String> latestResponse;
    private final List<String> createdUserIds = new ArrayList<>();
    private String lastCreatedUserId;
    private UserResponse lastRetrievedUser;

    @After
    public void cleanup() {
        // Clean up users created during tests
        for (String userId : createdUserIds) {
            try {
                restTemplate.delete(BASE_URL + "/api/users/" + userId);
            } catch (Exception e) {
                // Ignore errors during cleanup
            }
        }
        createdUserIds.clear();
    }

    @Given("a user named {string} with email {word} exists")
    public void createUserForTest(String name, String email) throws Exception {
        CreateUserRequest request = new CreateUserRequest(name, email, List.of());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CreateUserRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(BASE_URL + "/api/users", entity, String.class);
        UserResponse user = objectMapper.readValue(response.getBody(), UserResponse.class);
        createdUserIds.add(user.id());
        lastCreatedUserId = user.id();
    }

    @When("the client creates a user named {string} with email {word}")
    public void createUser(String name, String email) throws Exception {
        CreateUserRequest request = new CreateUserRequest(name, email, List.of());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CreateUserRequest> entity = new HttpEntity<>(request, headers);

        latestResponse = restTemplate.postForEntity(BASE_URL + "/api/users", entity, String.class);
        UserResponse user = objectMapper.readValue(latestResponse.getBody(), UserResponse.class);
        createdUserIds.add(user.id());
        lastCreatedUserId = user.id();
    }

    @When("the client tries to create a user named {string} with email {word}")
    public void tryCreateUser(String name, String email) {
        CreateUserRequest request = new CreateUserRequest(name, email, List.of());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CreateUserRequest> entity = new HttpEntity<>(request, headers);

        try {
            latestResponse = restTemplate.postForEntity(BASE_URL + "/api/users", entity, String.class);
        } catch (HttpClientErrorException e) {
            latestResponse = ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        }
    }

    @When("the client retrieves all users")
    public void retrieveAllUsers() {
        latestResponse = restTemplate.getForEntity(BASE_URL + "/api/users", String.class);
    }

    @When("the client retrieves the user by id")
    public void retrieveUserById() throws Exception {
        latestResponse = restTemplate.getForEntity(BASE_URL + "/api/users/" + lastCreatedUserId, String.class);
        lastRetrievedUser = objectMapper.readValue(latestResponse.getBody(), UserResponse.class);
    }

    @When("the client retrieves the user by email {word}")
    public void retrieveUserByEmail(String email) throws Exception {
        latestResponse = restTemplate.getForEntity(BASE_URL + "/api/users/by-email?email=" + email, String.class);
        lastRetrievedUser = objectMapper.readValue(latestResponse.getBody(), UserResponse.class);
    }

    @When("the client changes the user name to {string}")
    public void changeUserName(String newName) throws Exception {
        ChangeNameRequest request = new ChangeNameRequest(newName);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<ChangeNameRequest> entity = new HttpEntity<>(request, headers);

        latestResponse = restTemplate.exchange(BASE_URL + "/api/users/" + lastCreatedUserId + "/name",
                HttpMethod.PATCH, entity, String.class);
        lastRetrievedUser = objectMapper.readValue(latestResponse.getBody(), UserResponse.class);
    }

    @When("the client deletes the user")
    public void deleteUser() {
        latestResponse = restTemplate.exchange(BASE_URL + "/api/users/" + lastCreatedUserId,
                HttpMethod.DELETE, null, String.class);
    }

    @When("the client tries to retrieve the deleted user by id")
    public void tryRetrieveDeletedUser() {
        try {
            latestResponse = restTemplate.getForEntity(BASE_URL + "/api/users/" + lastCreatedUserId, String.class);
        } catch (HttpClientErrorException e) {
            latestResponse = ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        }
    }

    @Then("the user response status code is {int}")
    public void verifyUserResponseStatusCode(int statusCode) {
        assertThat("status code is incorrect", latestResponse.getStatusCode().value(), is(statusCode));
    }

    @Then("the client can see at least {int} users")
    public void verifyUserCount(int minCount) throws Exception {
        List<UserResponse> users = objectMapper.readValue(latestResponse.getBody(),
                new TypeReference<List<UserResponse>>() {
                });
        assertThat("user count is less than expected", users.size(), greaterThanOrEqualTo(minCount));
    }

    @Then("the user has name {string} and email {word}")
    public void verifyUserDetails(String name, String email) {
        assertThat("user name is incorrect", lastRetrievedUser.name(), is(name));
        assertThat("user email is incorrect", lastRetrievedUser.email(), is(email));
    }
}