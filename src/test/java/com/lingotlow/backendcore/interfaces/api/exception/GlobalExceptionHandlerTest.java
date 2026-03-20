package com.lingotlow.backendcore.interfaces.api.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("GlobalExceptionHandler Integration Tests")
class GlobalExceptionHandlerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GlobalExceptionHandler globalExceptionHandler;

    private WebRequest webRequest;

    @BeforeEach
    void setUp() {
        webRequest = mock(WebRequest.class);
        when(webRequest.getDescription(false)).thenReturn("uri=/api/test");
    }

    @Test
    @DisplayName("Should handle ResourceNotFoundException correctly")
    void handleResourceNotFound_Success() throws Exception {
        // Given
        ResourceNotFoundException exception = new ResourceNotFoundException("Tenant", "test-tenant");

        // When
        var response = globalExceptionHandler.handleResourceNotFound(exception, webRequest);

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getError()).isEqualTo("Resource Not Found");
        assertThat(response.getBody().getMessage()).isEqualTo("Tenant with identifier 'test-tenant' not found");
        assertThat(response.getBody().getPath()).isEqualTo("/api/test");
    }

    @Test
    @DisplayName("Should handle ResourceAlreadyExistsException correctly")
    void handleResourceAlreadyExists_Success() throws Exception {
        // Given
        ResourceAlreadyExistsException exception = new ResourceAlreadyExistsException("Tenant", "test-tenant");

        // When
        var response = globalExceptionHandler.handleResourceAlreadyExists(exception, webRequest);

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(409);
        assertThat(response.getBody().getError()).isEqualTo("Conflict");
        assertThat(response.getBody().getMessage()).isEqualTo("Tenant with identifier 'test-tenant' already exists");
        assertThat(response.getBody().getPath()).isEqualTo("/api/test");
    }

    @Test
    @DisplayName("Should handle MethodArgumentNotValidException correctly")
    void handleValidationExceptions_Success() throws Exception {
        // Given
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        org.springframework.validation.BindingResult bindingResult = mock(org.springframework.validation.BindingResult.class);
        
        List<org.springframework.validation.ObjectError> objectErrors = List.of(
                new FieldError("tenantCreateRequest", "tenantKey", "must not be blank"),
                new FieldError("tenantCreateRequest", "name", "must not be empty")
        );
        
        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getAllErrors()).thenReturn(objectErrors);

        // When
        var response = globalExceptionHandler.handleValidationExceptions(exception, webRequest);

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getError()).isEqualTo("Validation Failed");
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid input data");
        assertThat(response.getBody().getValidationErrors()).containsKeys("tenantKey", "name");
        assertThat(response.getBody().getValidationErrors().get("tenantKey")).isEqualTo("must not be blank");
        assertThat(response.getBody().getValidationErrors().get("name")).isEqualTo("must not be empty");
    }

    @Test
    @DisplayName("Should handle IllegalArgumentException correctly")
    void handleIllegalArgument_Success() throws Exception {
        // Given
        IllegalArgumentException exception = new IllegalArgumentException("Invalid parameter value");

        // When
        var response = globalExceptionHandler.handleIllegalArgument(exception, webRequest);

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getError()).isEqualTo("Bad Request");
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid parameter value");
        assertThat(response.getBody().getPath()).isEqualTo("/api/test");
    }

    @Test
    @DisplayName("Should handle AuthenticationException correctly")
    void handleAuthentication_Success() throws Exception {
        // Given
        BadCredentialsException exception = new BadCredentialsException("Invalid credentials");

        // When
        var response = globalExceptionHandler.handleAuthentication(exception, webRequest);

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(401);
        assertThat(response.getBody().getError()).isEqualTo("Unauthorized");
        assertThat(response.getBody().getMessage()).isEqualTo("Authentication failed");
        assertThat(response.getBody().getPath()).isEqualTo("/api/test");
    }

    @Test
    @DisplayName("Should handle AccessDeniedException correctly")
    void handleAccessDenied_Success() throws Exception {
        // Given
        AccessDeniedException exception = new AccessDeniedException("Access denied");

        // When
        var response = globalExceptionHandler.handleAccessDenied(exception, webRequest);

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(403);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(403);
        assertThat(response.getBody().getError()).isEqualTo("Forbidden");
        assertThat(response.getBody().getMessage()).isEqualTo("Access denied");
        assertThat(response.getBody().getPath()).isEqualTo("/api/test");
    }

    @Test
    @DisplayName("Should handle generic Exception correctly")
    void handleGlobalException_Success() throws Exception {
        // Given
        RuntimeException exception = new RuntimeException("Unexpected database error");

        // When
        var response = globalExceptionHandler.handleGlobalException(exception, webRequest);

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(500);
        assertThat(response.getBody().getError()).isEqualTo("Internal Server Error");
        assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred");
        assertThat(response.getBody().getPath()).isEqualTo("/api/test");
    }

    @Test
    @DisplayName("Should handle null message gracefully")
    void handleResourceNotFound_HandlesNullMessage() throws Exception {
        // Given
        ResourceNotFoundException exception = new ResourceNotFoundException("Tenant", null);

        // When
        var response = globalExceptionHandler.handleResourceNotFound(exception, webRequest);

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Tenant with identifier 'null' not found");
    }

    @Test
    @DisplayName("Should handle empty validation errors gracefully")
    void handleValidationExceptions_HandlesEmptyErrors() throws Exception {
        // Given
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        org.springframework.validation.BindingResult bindingResult = mock(org.springframework.validation.BindingResult.class);
        
        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getAllErrors()).thenReturn(List.of());

        // When
        var response = globalExceptionHandler.handleValidationExceptions(exception, webRequest);

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getValidationErrors()).isEmpty();
    }

    @Test
    @DisplayName("Should include timestamp in all error responses")
    void allErrorResponses_IncludeTimestamp() throws Exception {
        // Given
        ResourceNotFoundException exception = new ResourceNotFoundException("Tenant", "test-tenant");

        // When
        var response = globalExceptionHandler.handleResourceNotFound(exception, webRequest);

        // Then
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTimestamp()).isNotNull();
        assertThat(response.getBody().getTimestamp()).isBeforeOrEqualTo(java.time.OffsetDateTime.now().plusSeconds(1));
    }

    @Test
    @DisplayName("Should handle complex URI path correctly")
    void handleResourceNotFound_HandlesComplexPath() throws Exception {
        // Given
        when(webRequest.getDescription(false)).thenReturn("uri=/api/tenants/test-tenant/events");
        ResourceNotFoundException exception = new ResourceNotFoundException("Event", "event-123");

        // When
        var response = globalExceptionHandler.handleResourceNotFound(exception, webRequest);

        // Then
        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody().getPath()).isEqualTo("/api/tenants/test-tenant/events");
    }
}
