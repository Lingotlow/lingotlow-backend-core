package com.lingotlow.backendcore.interfaces.api.event.mapper;

import com.lingotlow.backendcore.domain.event.model.EventRequestDTO;
import com.lingotlow.backendcore.domain.event.model.EventResponseDTO;
import com.lingotlow.backendcore.interfaces.api.event.model.EventIngestRequest;
import com.lingotlow.backendcore.interfaces.api.event.model.EventIngestResponse;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public abstract class EventControllerMapper {

    public static final EventControllerMapper INSTANCE = Mappers.getMapper(EventControllerMapper.class);

    public abstract EventRequestDTO mapToDomain(EventIngestRequest request);

    public abstract EventIngestResponse mapToResponse(EventResponseDTO response);
}
