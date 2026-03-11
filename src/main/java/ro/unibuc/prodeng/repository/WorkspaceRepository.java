package ro.unibuc.prodeng.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import ro.unibuc.prodeng.model.WorkspaceEntity;

@Repository
public interface WorkspaceRepository extends MongoRepository<WorkspaceEntity, String> {}
