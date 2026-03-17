package ro.unibuc.prodeng.request;

import java.util.List;

public record DocumentUpdateRequest(

                /** New title — null means keep the existing title. */
                String title,

                /** New content — null means keep the existing content. */
                String content,

                /** Replaces the workspace list entirely when provided. */
                String workspaceId,

                /** Replaces the viewers list entirely when provided. */
                List<String> viewers,

                /** Replaces the editors list entirely when provided. */
                List<String> editors) {
}
