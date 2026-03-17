package ro.unibuc.prodeng.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import ro.unibuc.prodeng.request.CreateWorkspaceRequest;
import ro.unibuc.prodeng.response.WorkspaceResponse;
import ro.unibuc.prodeng.service.WorkspaceService;

@RestController
@RequestMapping("/api/workspaces")
public class WorkspaceController {
    @Autowired
    private WorkspaceService workspaceService;

    @PostMapping
    public ResponseEntity<WorkspaceResponse> createWorkspace(@Valid @RequestBody CreateWorkspaceRequest request) {
        WorkspaceResponse workspace = workspaceService.createWorkspace(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(workspace);
    }
}