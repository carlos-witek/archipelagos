package org.carlos.archipelagos.engine;

public enum RunStatus {
    QUEUED, RUNNING, FINISHED, STOPPED, FAILED;

    public boolean isTerminal() {
        return this == FINISHED || this == STOPPED || this == FAILED;
    }
}
