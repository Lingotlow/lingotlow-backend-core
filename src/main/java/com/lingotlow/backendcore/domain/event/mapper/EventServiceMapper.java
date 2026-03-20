package com.lingotlow.backendcore.domain.event.mapper;

import com.lingotlow.backendcore.domain.event.model.EventRequestDTO;
import com.lingotlow.backendcore.domain.event.model.EventResponseDTO;
import com.lingotlow.backendcore.infrastructure.repository.entity.EventEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

import java.time.OffsetDateTime;
import java.util.UUID;

@Mapper(componentModel = "spring")
public abstract class EventServiceMapper {

    public static final EventServiceMapper INSTANCE = Mappers.getMapper(EventServiceMapper.class);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", source = "tenantId")
    @Mapping(target = "requestId", source = "requestId")
    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "retryCount", constant = "0")
    @Mapping(target = "failureReason", ignore = true)
    @Mapping(target = "metadata", expression = "java(convertMetadataToJson(request))")
    @Mapping(target = "payload", expression = "java(convertToJson(request))")
    @Mapping(target = "headers", expression = "java(convertHeadersToJson(request))")
    @Mapping(target = "sourceIp", ignore = true)
    @Mapping(target = "deliveredAt", ignore = true)
    @Mapping(target = "lastRetryAt", ignore = true)
    public abstract EventEntity mapToCreateEntity(EventRequestDTO request, UUID tenantId, UUID requestId);

    @Mapping(target = "requestId", source = "entity.requestId")
    @Mapping(target = "status", source = "entity.status")
    @Mapping(target = "timestamp", source = "entity.createdAt")
    @Mapping(target = "message", ignore = true)
    public abstract EventResponseDTO mapToResponseDTO(EventEntity entity);

    protected String convertToJson(EventRequestDTO request) {
        if (request.getPayload() == null) return null;
        return "{\"payload\":\"" + request.getPayload() + "\"}";
    }

    protected String convertMetadataToJson(EventRequestDTO request) {
        if (request.getMetadata() == null || request.getMetadata().isEmpty()) return null;
        try {
            StringBuilder json = new StringBuilder("{");
            request.getMetadata().forEach((key, value) -> 
                json.append("\"").append(key).append("\":\"").append(value).append("\","));
            if (json.length() > 1) {
                json.setLength(json.length() - 1); // Remove last comma
            }
            json.append("}");
            return json.toString();
        } catch (Exception e) {
            return null;
        }
    }

    protected String convertHeadersToJson(EventRequestDTO request) {
        if (request.getHeaders() == null || request.getHeaders().isEmpty()) return null;
        try {
            StringBuilder json = new StringBuilder("{");
            request.getHeaders().forEach((key, value) -> 
                json.append("\"").append(key).append("\":\"").append(value).append("\","));
            if (json.length() > 1) {
                json.setLength(json.length() - 1); // Remove last comma
            }
            json.append("}");
            return json.toString();
        } catch (Exception e) {
            return null;
        }
    }
}
