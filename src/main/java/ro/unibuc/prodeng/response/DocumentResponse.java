package ro.unibuc.prodeng.response;

import java.time.Instant;
import java.util.List;

public record DocumentResponse(

        /** Unique ID of this specific version's record in MongoDB. */
        String id,

        /** Shared identifier grouping all versions of the same logical document. */
        String documentGroupId,

        /** Version number of this record (starts at 1). */
        int version,

        String ownerId,

        String title,

        String content,

        String workspaceId,

        List<String> viewers,

        List<String> editors,

        Instant createdAt
) {}
