package com.skillsync.service;

import com.skillsync.dto.event.CreateEventRequest;
import com.skillsync.dto.event.EventRegistrationResponse;
import com.skillsync.dto.event.EventResponse;
import com.skillsync.dto.event.UpdateEventRequest;
import com.skillsync.entity.Event;
import com.skillsync.entity.EventRegistration;
import com.skillsync.entity.User;
import com.skillsync.enums.EventRegistrationStatus;
import com.skillsync.enums.EventType;
import com.skillsync.exception.AccessDeniedException;
import com.skillsync.exception.BadRequestException;
import com.skillsync.exception.DuplicateResourceException;
import com.skillsync.repository.EventRegistrationRepository;
import com.skillsync.repository.EventRepository;
import com.skillsync.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;
    @Mock
    private EventRegistrationRepository registrationRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private EventService eventService;

    private User organizer;
    private User attendee;
    private Event futureEvent;

    @BeforeEach
    void setUp() {
        organizer = new User("organizer@university.edu", "hash", "Organizer User", "CS", 4);
        organizer.setId(10L);

        attendee = new User("attendee@university.edu", "hash", "Attendee User", "IT", 2);
        attendee.setId(20L);

        futureEvent = new Event(
                organizer,
                "Campus Hackathon",
                "48-hour build contest",
                "Student Union Hall",
                EventType.HACKATHON,
                Instant.now().plus(2, ChronoUnit.DAYS),
                Instant.now().plus(4, ChronoUnit.DAYS),
                50
        );
        futureEvent.setId(100L);
    }

    @Test
    @DisplayName("Should reject event creation if start time is not before end time")
    void testCreateEventRejectsInvalidTimeRange() {
        CreateEventRequest request = new CreateEventRequest(
                "Invalid Event",
                "Description",
                "Auditorium",
                EventType.TECH_TALK,
                Instant.now().plus(5, ChronoUnit.DAYS), // start
                Instant.now().plus(2, ChronoUnit.DAYS), // end (before start)
                100
        );

        assertThatThrownBy(() -> eventService.createEvent(10L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Event start time must precede end time");

        verify(eventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject registration for concluded event")
    void testRegisterForEventRejectsConcludedEvent() {
        Event concludedEvent = new Event(
                organizer,
                "Past Workshop",
                "Past description",
                "Room 101",
                EventType.WORKSHOP,
                Instant.now().minus(5, ChronoUnit.DAYS),
                Instant.now().minus(2, ChronoUnit.DAYS),
                30
        );
        concludedEvent.setId(200L);

        when(eventRepository.findByIdWithPessimisticLock(200L)).thenReturn(Optional.of(concludedEvent));

        assertThatThrownBy(() -> eventService.registerForEvent(200L, 20L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot register for an event that has already concluded");

        verify(registrationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject registration when event capacity is reached")
    void testRegisterForEventRejectsWhenCapacityExceeded() {
        futureEvent.setCapacity(2);
        when(eventRepository.findByIdWithPessimisticLock(100L)).thenReturn(Optional.of(futureEvent));
        when(userRepository.findById(20L)).thenReturn(Optional.of(attendee));
        when(registrationRepository.findByIdEventIdAndIdUserId(100L, 20L)).thenReturn(Optional.empty());
        when(registrationRepository.countByIdEventIdAndStatus(100L, EventRegistrationStatus.REGISTERED)).thenReturn(2L); // At capacity

        assertThatThrownBy(() -> eventService.registerForEvent(100L, 20L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Event is at full capacity (2 attendees)");

        verify(registrationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject duplicate active registration")
    void testRegisterForEventRejectsDuplicateActiveRegistration() {
        EventRegistration activeReg = new EventRegistration(futureEvent, attendee);
        activeReg.setStatus(EventRegistrationStatus.REGISTERED);

        when(eventRepository.findByIdWithPessimisticLock(100L)).thenReturn(Optional.of(futureEvent));
        when(userRepository.findById(20L)).thenReturn(Optional.of(attendee));
        when(registrationRepository.findByIdEventIdAndIdUserId(100L, 20L)).thenReturn(Optional.of(activeReg));

        assertThatThrownBy(() -> eventService.registerForEvent(100L, 20L))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already registered for this event");
    }

    @Test
    @DisplayName("Should reactivate registration if user was previously cancelled")
    void testRegisterForEventReactivatesCancelledRegistration() {
        EventRegistration cancelledReg = new EventRegistration(futureEvent, attendee);
        cancelledReg.setStatus(EventRegistrationStatus.CANCELLED);

        when(eventRepository.findByIdWithPessimisticLock(100L)).thenReturn(Optional.of(futureEvent));
        when(userRepository.findById(20L)).thenReturn(Optional.of(attendee));
        when(registrationRepository.findByIdEventIdAndIdUserId(100L, 20L)).thenReturn(Optional.of(cancelledReg));
        when(registrationRepository.save(any(EventRegistration.class))).thenAnswer(inv -> inv.getArgument(0));

        EventRegistrationResponse response = eventService.registerForEvent(100L, 20L);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(EventRegistrationStatus.REGISTERED);
        verify(registrationRepository).save(cancelledReg);
    }

    @Test
    @DisplayName("Should reject event update from non-organizer")
    void testUpdateEventRejectsNonOrganizer() {
        when(eventRepository.findById(100L)).thenReturn(Optional.of(futureEvent));

        UpdateEventRequest request = new UpdateEventRequest(
                "Hacked Title", "Desc", "Auditorium", EventType.HACKATHON,
                futureEvent.getStartTime(), futureEvent.getEndTime(), 100
        );

        assertThatThrownBy(() -> eventService.updateEvent(100L, 999L, request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only the event organizer can update event details");

        verify(eventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject event deletion from non-organizer")
    void testDeleteEventRejectsNonOrganizer() {
        when(eventRepository.findById(100L)).thenReturn(Optional.of(futureEvent));

        assertThatThrownBy(() -> eventService.deleteEvent(100L, 999L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only the event organizer can delete this event");

        verify(eventRepository, never()).delete(any());
    }
}
