package ro.unibuc.prodeng.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import ro.unibuc.prodeng.exception.EntityNotFoundException;
import ro.unibuc.prodeng.request.AddUserToWorkspaceRequest;
import ro.unibuc.prodeng.request.CreateWorkspaceRequest;
import ro.unibuc.prodeng.response.UserResponse;
import ro.unibuc.prodeng.response.WorkspaceResponse;
import ro.unibuc.prodeng.response.WorkspaceStatisticsResponse;
import ro.unibuc.prodeng.service.WorkspaceService;

@RestController
@RequestMapping("/api/workspaces")
public class WorkspaceController {
    @Autowired
    private WorkspaceService workspaceService;

    @GetMapping
    public ResponseEntity<List<WorkspaceResponse>> getAllWorkspaces() {
        List<WorkspaceResponse> workspaces = workspaceService.getAllWorkspaces();
        return ResponseEntity.ok(workspaces);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkspaceResponse> getWorkspaceById (@PathVariable String id) {
        WorkspaceResponse workspace = workspaceService.getWorkspaceById(id);
        return ResponseEntity.ok(workspace);
    }

    @GetMapping("/statistics/{id}")
    public ResponseEntity<WorkspaceStatisticsResponse> getWorkspaceStatistics(@PathVariable String id) {
        WorkspaceStatisticsResponse statistics = workspaceService.getWorkspaceStatistics(id);
        return ResponseEntity.ok(statistics);
    }

    @PostMapping
    public ResponseEntity<WorkspaceResponse> createWorkspace(
        @Valid @RequestBody CreateWorkspaceRequest request) {
        WorkspaceResponse workspace = workspaceService.createWorkspace(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(workspace);
    }

    @PostMapping("/add-user")
    public ResponseEntity<WorkspaceResponse> addUserToWorkspace(
        @Valid @RequestBody AddUserToWorkspaceRequest request) throws EntityNotFoundException {
        WorkspaceResponse workspace = workspaceService.addUserToWorkspace(request);
        return ResponseEntity.ok(workspace);
    }
}