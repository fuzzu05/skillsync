package com.skillsync.controller;

import com.skillsync.dto.event.CreateEventRequest;
import com.skillsync.dto.event.EventRegistrationResponse;
import com.skillsync.dto.event.EventResponse;
import com.skillsync.dto.event.UpdateEventRequest;
import com.skillsync.enums.EventType;
import com.skillsync.security.UserPrincipal;
import com.skillsync.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/events")
@Tag(name = "Events", description = "Campus hackathons, workshops, tech talks, and attendee registrations")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    @Operation(summary = "Create an event", description = "Publish a new campus hackathon, workshop, or tech talk.")
    public ResponseEntity<EventResponse> createEvent(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateEventRequest request
    ) {
        EventResponse created = eventService.createEvent(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    @Operation(summary = "List upcoming events", description = "Discover future campus events with optional event type or search keyword filters.")
    public ResponseEntity<Page<EventResponse>> listEvents(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) EventType eventType,
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Long currentUserId = principal != null ? principal.getId() : null;
        Page<EventResponse> events = eventService.listUpcomingEvents(query, eventType, currentUserId, pageable);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get event details", description = "Retrieve event venue, schedule, description, and registration status.")
    public ResponseEntity<EventResponse> getEvent(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long currentUserId = principal != null ? principal.getId() : null;
        EventResponse event = eventService.getEvent(id, currentUserId);
        return ResponseEntity.ok(event);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update event", description = "Edit event schedule, location, or capacity (Organizer only).")
    public ResponseEntity<EventResponse> updateEvent(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateEventRequest request
    ) {
        EventResponse updated = eventService.updateEvent(id, principal.getId(), request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete event", description = "Cancel and remove an event (Organizer only).")
    public ResponseEntity<Void> deleteEvent(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        eventService.deleteEvent(id, principal.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/register")
    @Operation(summary = "Register for event", description = "Enroll current student for an event. Enforces seat capacity check safely.")
    public ResponseEntity<EventRegistrationResponse> registerForEvent(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        EventRegistrationResponse registration = eventService.registerForEvent(id, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(registration);
    }

    @DeleteMapping("/{id}/register")
    @Operation(summary = "Cancel event registration", description = "Cancel attendance for an event.")
    public ResponseEntity<Void> cancelRegistration(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        eventService.cancelRegistration(id, principal.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/my-registrations")
    @Operation(summary = "Get my event registrations", description = "List all campus events the current student is registered for.")
    public ResponseEntity<List<EventRegistrationResponse>> getMyRegistrations(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<EventRegistrationResponse> registrations = eventService.getMyRegistrations(principal.getId());
        return ResponseEntity.ok(registrations);
    }
}
