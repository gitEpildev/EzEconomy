package com.gitepildev.giteconomy.api.storage.exceptions;

public class StorageInitException extends StorageException {
    public StorageInitException(String message) {
        super(message);
    }
    public StorageInitException(String message, Throwable cause) {
        super(message, cause);
    }
}
