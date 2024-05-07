package com.example.clubservice.dynamo.migration;

import org.springframework.stereotype.Component;
/*
the job of the operation mode manager is to manage the operation mode of the service.
 */
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
