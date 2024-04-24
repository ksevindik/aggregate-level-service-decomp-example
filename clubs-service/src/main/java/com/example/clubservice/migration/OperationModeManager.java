package com.example.clubservice.migration;

import org.springframework.stereotype.Component;

@Component
public class OperationModeManager {
    private OperationMode operationMode = OperationMode.READ_ONLY;

    public OperationMode getOperationMode() {
        return operationMode;
    }

    public void setOperationMode(OperationMode operationMode) {
        this.operationMode = operationMode;
    }

    public boolean isReadOnly() {
        return operationMode == OperationMode.READ_ONLY;
    }

    public boolean isReadWrite() {
        return operationMode == OperationMode.READ_WRITE;
    }

    public boolean isDryRun() {
        return operationMode == OperationMode.DRY_RUN;
    }
}
