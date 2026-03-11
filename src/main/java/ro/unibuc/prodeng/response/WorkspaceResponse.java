package ro.unibuc.prodeng.response;

import java.util.List;

public record WorkspaceResponse(
    String id,
    String name,
    List<String> users
) {}
