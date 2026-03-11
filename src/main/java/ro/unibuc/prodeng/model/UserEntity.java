package ro.unibuc.prodeng.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document(collection = "users")
public record UserEntity(
    @Id
    String id,
    String name,
    String email,
    List<String> workspaces
) {
    public UserEntity {
        if (workspaces == null) {
            workspaces = new ArrayList<>();
        }
    }
}
