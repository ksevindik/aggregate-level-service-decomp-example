package com.example.clubservice.migration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/migrations")
public class MigrationController {

    private final BulkSyncTool bulkSyncTool;

    private OperationModeManager operationModeManager;

    @Autowired
    public MigrationController(BulkSyncTool bulkSyncTool,
                               OperationModeManager operationModeManager) {
        this.bulkSyncTool = bulkSyncTool;
        this.operationModeManager = operationModeManager;
    }

    @PostMapping("/bulkSync")
    public ResponseEntity<String> startSync() {
        try {
            bulkSyncTool.bulkSync();
            return ResponseEntity.ok("Bulk sync completed successfully.");
        } catch (Exception e) {
            // Basic error handling for simplicity
            return ResponseEntity.internalServerError().body("Bulk sync failed: " + e.getMessage());
        }
    }

    @GetMapping("/operationMode")
    public OperationMode getOperationMode() {
        return operationModeManager.getOperationMode();
    }

    @PutMapping("/operationMode")
    public void setOperationMode(@RequestBody  OperationMode operationMode) {
        operationModeManager.setOperationMode(operationMode);
    }
}