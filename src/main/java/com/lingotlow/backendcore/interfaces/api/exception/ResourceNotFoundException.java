package com.lingotlow.backendcore.interfaces.api.exception;

public class ResourceNotFoundException extends RuntimeException {
    
    public ResourceNotFoundException(String message) {
        super(message);
    }
    
    public ResourceNotFoundException(String resource, String identifier) {
        super(String.format("%s with identifier '%s' not found", resource, identifier));
    }
}
