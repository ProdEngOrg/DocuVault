package ro.unibuc.prodeng.request;

import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateUserRequest(
    @NotBlank(message = "Name is required")
    String name,

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    String email,

    List<String> workspaces
) {}
