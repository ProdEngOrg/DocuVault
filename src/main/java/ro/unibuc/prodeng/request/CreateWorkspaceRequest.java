package ro.unibuc.prodeng.request;

import jakarta.validation.constraints.NotBlank;

public record CreateWorkspaceRequest(
    @NotBlank(message = "Name is required")
    String name,
    String userId
) {}
