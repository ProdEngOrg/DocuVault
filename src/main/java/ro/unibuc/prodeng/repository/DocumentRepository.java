package ro.unibuc.prodeng.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import ro.unibuc.prodeng.model.DocumentEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends MongoRepository<DocumentEntity, String> {

    List<DocumentEntity> findByDocumentGroupIdOrderByVersionDesc(String documentGroupId);

    /** Returns the single latest version of a logical document. */
    Optional<DocumentEntity> findTopByDocumentGroupIdOrderByVersionDesc(String documentGroupId);

    /** All documents owned by a user (latest and historical versions). */
    List<DocumentEntity> findByOwnerId(String ownerId);

    /** All documents in a workspace. */
    List<DocumentEntity> findByWorkspaceId(String workspaceId);

}
