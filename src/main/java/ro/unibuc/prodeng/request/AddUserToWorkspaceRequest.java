package ro.unibuc.prodeng.request;

import jakarta.validation.constraints.NotBlank;

public record AddUserToWorkspaceRequest(
    @NotBlank
    String userId,
    @NotBlank
    String workspaceId
) {}