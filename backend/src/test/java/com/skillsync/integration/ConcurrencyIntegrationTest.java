package com.skillsync.integration;

import com.skillsync.dto.application.ApplyProjectRequest;
import com.skillsync.dto.application.ReviewApplicationRequest;
import com.skillsync.entity.Event;
import com.skillsync.entity.Project;
import com.skillsync.entity.ProjectApplication;
import com.skillsync.entity.Team;
import com.skillsync.entity.TeamMember;
import com.skillsync.entity.User;
import com.skillsync.enums.ApplicationStatus;
import com.skillsync.enums.EventRegistrationStatus;
import com.skillsync.enums.EventType;
import com.skillsync.enums.TeamRole;
import com.skillsync.repository.*;
import com.skillsync.service.ConversationService;
import com.skillsync.service.EventService;
import com.skillsync.service.ProjectApplicationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ConcurrencyIntegrationTest {

    @Autowired
    private ProjectApplicationService applicationService;

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private EventService eventService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectApplicationRepository applicationRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TeamMemberRepository teamMemberRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventRegistrationRepository registrationRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private MessageRepository messageRepository;

    @BeforeEach
    void cleanUp() {
        notificationRepository.deleteAll();
        messageRepository.deleteAll();
        conversationRepository.deleteAll();
        registrationRepository.deleteAll();
        eventRepository.deleteAll();
        applicationRepository.deleteAll();
        teamMemberRepository.deleteAll();
        teamRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Race Condition A1: Concurrent duplicate applications for same user must result in exactly 1 saved application")
    void testConcurrentDuplicateApplications() throws InterruptedException {
        User owner = userRepository.save(new User("owner@test.edu", "hash", "Owner", "CS", 4));
        User applicant = userRepository.save(new User("applicant@test.edu", "hash", "Applicant", "CS", 2));

        Project project = new Project(owner, "Concurrent Drone", "Summary", "Description", 4);
        Project savedProject = projectRepository.save(project);

        Team team = teamRepository.save(new Team(savedProject, "Team Drone"));
        savedProject.setTeam(team);
        teamMemberRepository.save(new TeamMember(team, owner, TeamRole.LEADER));

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startGate.await(); // Synchronize all 10 threads to release at the exact same instant
                    applicationService.applyToProject(savedProject.getId(), applicant.getId(), new ApplyProjectRequest("Pitch"));
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    conflictCount.incrementAndGet();
                } finally {
                    endGate.countDown();
                }
            });
        }

        startGate.countDown(); // Fire simultaneous requests
        assertThat(endGate.await(10, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();

        // Database-level assertions
        long dbApplicationCount = applicationRepository.count();
        assertThat(dbApplicationCount).isEqualTo(1L);
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(conflictCount.get()).isEqualTo(threadCount - 1);
    }

    @Test
    @DisplayName("Race Condition A2: Concurrent application acceptance must never exceed project maxTeamSize")
    void testConcurrentApplicationAcceptanceEnforcesMaxTeamSize() throws InterruptedException {
        // Project with maxTeamSize = 2. Owner is 1 member. Exactly 1 slot remains.
        User owner = userRepository.save(new User("leader@test.edu", "hash", "Leader", "CS", 4));
        Project project = new Project(owner, "Limited Project", "Summary", "Description", 2);
        Project savedProject = projectRepository.save(project);

        Team team = teamRepository.save(new Team(savedProject, "Limited Team"));
        savedProject.setTeam(team);
        teamMemberRepository.save(new TeamMember(team, owner, TeamRole.LEADER));

        // Create 3 distinct applicants with submitted pending applications
        List<ProjectApplication> applications = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            User applicant = userRepository.save(new User("cand" + i + "@test.edu", "hash", "Cand " + i, "IT", 2));
            ProjectApplication app = applicationRepository.save(new ProjectApplication(savedProject, applicant, "Pitch " + i));
            applications.add(app);
        }

        int threadCount = 3;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(threadCount);

        AtomicInteger acceptedCount = new AtomicInteger(0);
        AtomicInteger rejectedCapacityCount = new AtomicInteger(0);

        for (ProjectApplication app : applications) {
            executor.submit(() -> {
                try {
                    startGate.await();
                    applicationService.reviewApplication(
                            savedProject.getId(),
                            app.getId(),
                            owner.getId(),
                            new ReviewApplicationRequest(ApplicationStatus.ACCEPTED, null)
                    );
                    acceptedCount.incrementAndGet();
                } catch (Exception e) {
                    rejectedCapacityCount.incrementAndGet();
                } finally {
                    endGate.countDown();
                }
            });
        }

        startGate.countDown();
        assertThat(endGate.await(10, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();

        // Database invariant assertions
        long finalTeamMemberCount = teamMemberRepository.countByIdTeamId(team.getId());
        assertThat(finalTeamMemberCount).isEqualTo(2L); // Max team size is strictly preserved
        assertThat(acceptedCount.get()).isEqualTo(1);   // Only 1 of the 3 applicants could be accepted
        assertThat(rejectedCapacityCount.get()).isEqualTo(2); // The remaining 2 were rejected by capacity check
    }

    @Test
    @DisplayName("Race Condition B: Concurrent event registrations at capacity limit must never oversell")
    void testConcurrentEventRegistrationAtCapacity() throws InterruptedException {
        User organizer = userRepository.save(new User("org@test.edu", "hash", "Organizer", "CS", 4));
        Event event = new Event(
                organizer,
                "Exclusive Workshop",
                "Description",
                "Lab 101",
                EventType.WORKSHOP,
                Instant.now().plus(3, ChronoUnit.DAYS),
                Instant.now().plus(4, ChronoUnit.DAYS),
                2 // Capacity strictly 2 attendees
        );
        Event savedEvent = eventRepository.save(event);

        // 6 distinct attendees try to register concurrently
        int candidateCount = 6;
        List<User> attendees = new ArrayList<>();
        for (int i = 1; i <= candidateCount; i++) {
            attendees.add(userRepository.save(new User("attendee" + i + "@test.edu", "hash", "Att " + i, "CS", 1)));
        }

        ExecutorService executor = Executors.newFixedThreadPool(candidateCount);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(candidateCount);

        AtomicInteger successRegistrations = new AtomicInteger(0);
        AtomicInteger capacityExceededCount = new AtomicInteger(0);

        for (User attendee : attendees) {
            executor.submit(() -> {
                try {
                    startGate.await();
                    eventService.registerForEvent(savedEvent.getId(), attendee.getId());
                    successRegistrations.incrementAndGet();
                } catch (Exception e) {
                    capacityExceededCount.incrementAndGet();
                } finally {
                    endGate.countDown();
                }
            });
        }

        startGate.countDown();
        assertThat(endGate.await(10, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();

        // Database invariant assertions
        long registeredCount = registrationRepository.countByIdEventIdAndStatus(savedEvent.getId(), EventRegistrationStatus.REGISTERED);
        assertThat(registeredCount).isEqualTo(2L); // Never exceeds capacity of 2
        assertThat(successRegistrations.get()).isEqualTo(2);
        assertThat(capacityExceededCount.get()).isEqualTo(candidateCount - 2);
    }

    @Test
    @DisplayName("Race Condition C: Simultaneous direct conversation creation between 2 users yields exactly 1 conversation")
    void testConcurrentDirectConversationCreation() throws InterruptedException {
        User u1 = userRepository.save(new User("alice.conc@test.edu", "hash", "Alice", "CS", 2));
        User u2 = userRepository.save(new User("bob.conc@test.edu", "hash", "Bob", "IT", 3));

        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(threadCount);

        // Thread 1: u1 initiates conversation with u2
        executor.submit(() -> {
            try {
                startGate.await();
                conversationService.getOrCreateDirectConversation(u1.getId(), u2.getId());
            } catch (Exception ignored) {
            } finally {
                endGate.countDown();
            }
        });

        // Thread 2: u2 initiates conversation with u1 simultaneously
        executor.submit(() -> {
            try {
                startGate.await();
                conversationService.getOrCreateDirectConversation(u2.getId(), u1.getId());
            } catch (Exception ignored) {
            } finally {
                endGate.countDown();
            }
        });

        startGate.countDown();
        assertThat(endGate.await(10, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();

        // Database invariant: exactly one conversation row in database
        assertThat(conversationRepository.count()).isEqualTo(1L);
    }
}
