package com.example.clubservice.migration;

/*
- read-only: only the reads are served from the service side, any writes are forwarded to the monolith side.
- read-write: both reads and writes are served from the service side, changes are reflected to the monolith DB so that
    data level dependencies are maintained on the monolith side.
- dry-run: both reads and writes are forwarded to the monolith side, but writes are also handled on the service side
    so that a separate data verification could run to compare data between monolith and service DB in order to make
    sure service side business processing works as expected.
 */
public enum OperationMode {
    READ_ONLY,
    READ_WRITE,
    DRY_RUN
}
