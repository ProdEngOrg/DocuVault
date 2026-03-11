package ro.unibuc.prodeng.model;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "workspaces")
public record WorkspaceEntity(
    @Id
    String id,
    String name,
    List<String> users
) {}
