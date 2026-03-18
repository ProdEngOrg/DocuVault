package ro.unibuc.prodeng.request;

import jakarta.validation.constraints.NotBlank;

public record DocumentAddViewerRequest(
    @NotBlank
    String userId,
    @NotBlank
    String documentGroupId
) {}
