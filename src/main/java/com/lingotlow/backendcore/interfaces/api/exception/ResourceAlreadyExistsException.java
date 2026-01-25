package com.lingotlow.backendcore.interfaces.api.exception;

public class ResourceAlreadyExistsException extends RuntimeException {
    
    public ResourceAlreadyExistsException(String message) {
        super(message);
    }
    
    public ResourceAlreadyExistsException(String resource, String identifier) {
        super(String.format("%s with identifier '%s' already exists", resource, identifier));
    }
}
