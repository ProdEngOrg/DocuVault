package ro.unibuc.prodeng.request;

public record AddUserToWorkspaceRequest(
    String userId,
    String workspaceId
) {}