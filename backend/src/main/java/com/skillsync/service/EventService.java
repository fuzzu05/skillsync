package com.skillsync.service;

import com.skillsync.dto.event.CreateEventRequest;
import com.skillsync.dto.event.EventRegistrationResponse;
import com.skillsync.dto.event.EventResponse;
import com.skillsync.dto.event.UpdateEventRequest;
import com.skillsync.entity.Event;
import com.skillsync.entity.EventRegistration;
import com.skillsync.entity.EventRegistrationId;
import com.skillsync.entity.User;
import com.skillsync.enums.EventRegistrationStatus;
import com.skillsync.enums.EventType;
import com.skillsync.exception.AccessDeniedException;
import com.skillsync.exception.BadRequestException;
import com.skillsync.exception.DuplicateResourceException;
import com.skillsync.exception.ResourceNotFoundException;
import com.skillsync.repository.EventRegistrationRepository;
import com.skillsync.repository.EventRepository;
import com.skillsync.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final EventRegistrationRepository registrationRepository;
    private final UserRepository userRepository;

    public EventService(EventRepository eventRepository,
                        EventRegistrationRepository registrationRepository,
                        UserRepository userRepository) {
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public EventResponse createEvent(Long organizerId, CreateEventRequest request) {
        if (!request.startTime().isBefore(request.endTime())) {
            throw new BadRequestException("Event start time must precede end time");
        }

        User organizer = userRepository.findById(organizerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", organizerId));

        Event event = new Event(
                organizer,
                request.title().trim(),
                request.description().trim(),
                request.location().trim(),
                request.eventType(),
                request.startTime(),
                request.endTime(),
                request.capacity()
        );

        Event saved = eventRepository.save(event);
        return EventResponse.fromEntity(saved, 0L, false, true);
    }

    @Transactional
    public EventResponse updateEvent(Long eventId, Long organizerId, UpdateEventRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        if (!event.getOrganizer().getId().equals(organizerId)) {
            throw new AccessDeniedException("Only the event organizer can update event details");
        }

        if (!request.startTime().isBefore(request.endTime())) {
            throw new BadRequestException("Event start time must precede end time");
        }

        event.setTitle(request.title().trim());
        event.setDescription(request.description().trim());
        event.setLocation(request.location().trim());
        event.setEventType(request.eventType());
        event.setStartTime(request.startTime());
        event.setEndTime(request.endTime());
        event.setCapacity(request.capacity());

        Event saved = eventRepository.save(event);
        long count = registrationRepository.countByIdEventIdAndStatus(eventId, EventRegistrationStatus.REGISTERED);
        boolean isRegistered = registrationRepository.findByIdEventIdAndIdUserId(eventId, organizerId)
                .map(r -> r.getStatus() == EventRegistrationStatus.REGISTERED)
                .orElse(false);

        return EventResponse.fromEntity(saved, count, isRegistered, true);
    }

    @Transactional
    public void deleteEvent(Long eventId, Long organizerId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        if (!event.getOrganizer().getId().equals(organizerId)) {
            throw new AccessDeniedException("Only the event organizer can delete this event");
        }

        eventRepository.delete(event);
    }

    @Transactional(readOnly = true)
    public EventResponse getEvent(Long eventId, Long currentUserId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        long count = registrationRepository.countByIdEventIdAndStatus(eventId, EventRegistrationStatus.REGISTERED);
        boolean isRegistered = false;
        boolean isOrganizer = false;

        if (currentUserId != null) {
            isOrganizer = event.getOrganizer().getId().equals(currentUserId);
            isRegistered = registrationRepository.findByIdEventIdAndIdUserId(eventId, currentUserId)
                    .map(r -> r.getStatus() == EventRegistrationStatus.REGISTERED)
                    .orElse(false);
        }

        return EventResponse.fromEntity(event, count, isRegistered, isOrganizer);
    }

    @Transactional(readOnly = true)
    public Page<EventResponse> listUpcomingEvents(String query, EventType eventType, Long currentUserId, Pageable pageable) {
        return eventRepository.searchUpcomingEvents(query, eventType, Instant.now(), pageable)
                .map(event -> {
                    long count = registrationRepository.countByIdEventIdAndStatus(event.getId(), EventRegistrationStatus.REGISTERED);
                    boolean isRegistered = currentUserId != null && registrationRepository.findByIdEventIdAndIdUserId(event.getId(), currentUserId)
                            .map(r -> r.getStatus() == EventRegistrationStatus.REGISTERED)
                            .orElse(false);
                    boolean isOrganizer = currentUserId != null && event.getOrganizer().getId().equals(currentUserId);
                    return EventResponse.fromEntity(event, count, isRegistered, isOrganizer);
                });
    }

    @Transactional
    public EventRegistrationResponse registerForEvent(Long eventId, Long userId) {
        // Concurrency-safe lock on the event
        Event event = eventRepository.findByIdWithPessimisticLock(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        if (event.getEndTime().isBefore(Instant.now())) {
            throw new BadRequestException("Cannot register for an event that has already concluded");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Optional<EventRegistration> existingOpt = registrationRepository.findByIdEventIdAndIdUserId(eventId, userId);
        if (existingOpt.isPresent()) {
            EventRegistration existing = existingOpt.get();
            if (existing.getStatus() == EventRegistrationStatus.REGISTERED) {
                throw new DuplicateResourceException("You are already registered for this event");
            }
            // Re-activate if previously cancelled
            existing.setStatus(EventRegistrationStatus.REGISTERED);
            existing.setRegisteredAt(Instant.now());
            EventRegistration updated = registrationRepository.save(existing);
            return EventRegistrationResponse.fromEntity(updated);
        }

        // Capacity check
        if (event.getCapacity() != null) {
            long currentCount = registrationRepository.countByIdEventIdAndStatus(eventId, EventRegistrationStatus.REGISTERED);
            if (currentCount >= event.getCapacity()) {
                throw new BadRequestException("Event is at full capacity (" + event.getCapacity() + " attendees)");
            }
        }

        EventRegistration registration = new EventRegistration(event, user);
        EventRegistration saved = registrationRepository.save(registration);
        return EventRegistrationResponse.fromEntity(saved);
    }

    @Transactional
    public void cancelRegistration(Long eventId, Long userId) {
        EventRegistration registration = registrationRepository.findByIdEventIdAndIdUserId(eventId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("No registration found for this event"));

        if (registration.getStatus() != EventRegistrationStatus.REGISTERED) {
            throw new BadRequestException("Registration is not active");
        }

        registration.setStatus(EventRegistrationStatus.CANCELLED);
        registrationRepository.save(registration);
    }

    @Transactional(readOnly = true)
    public List<EventRegistrationResponse> getMyRegistrations(Long userId) {
        return registrationRepository.findByUserIdWithEvent(userId)
                .stream()
                .map(EventRegistrationResponse::fromEntity)
                .toList();
    }
}
