package de.tum.in.www1.artemis.service.notifications;

import static de.tum.in.www1.artemis.domain.notification.NotificationConstants.*;
import static de.tum.in.www1.artemis.service.notifications.NotificationSettingsService.NOTIFICATION__TUTORIAL_GROUP_NOTIFICATION__TUTORIAL_GROUP_DELETE_UPDATE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.mail.internet.MimeMessage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

import de.tum.in.www1.artemis.AbstractSpringIntegrationBambooBitbucketJiraTest;
import de.tum.in.www1.artemis.domain.*;
import de.tum.in.www1.artemis.domain.enumeration.Language;
import de.tum.in.www1.artemis.domain.enumeration.tutorialgroups.TutorialGroupRegistrationType;
import de.tum.in.www1.artemis.domain.notification.Notification;
import de.tum.in.www1.artemis.domain.notification.TutorialGroupNotification;
import de.tum.in.www1.artemis.domain.tutorialgroups.TutorialGroup;
import de.tum.in.www1.artemis.domain.tutorialgroups.TutorialGroupRegistration;
import de.tum.in.www1.artemis.repository.CourseRepository;
import de.tum.in.www1.artemis.repository.NotificationSettingRepository;
import de.tum.in.www1.artemis.repository.UserRepository;
import de.tum.in.www1.artemis.repository.tutorialgroups.TutorialGroupNotificationRepository;
import de.tum.in.www1.artemis.repository.tutorialgroups.TutorialGroupRegistrationRepository;
import de.tum.in.www1.artemis.repository.tutorialgroups.TutorialGroupRepository;

class TutorialGroupNotificationServiceTest extends AbstractSpringIntegrationBambooBitbucketJiraTest {

    private static final String TEST_PREFIX = "tutorialgroupnotifservice";

    @Autowired
    private TutorialGroupNotificationRepository tutorialGroupNotificationRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private TutorialGroupRepository tutorialGroupRepository;

    @Autowired
    private TutorialGroupRegistrationRepository tutorialGroupRegistrationRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    private NotificationSettingRepository notificationSettingRepository;

    private TutorialGroup tutorialGroup;

    private User student1;

    private User tutor1;

    @BeforeEach
    void setUp() {
        this.database.addUsers(TEST_PREFIX, 5, 1, 0, 1);
        Course course = this.database.createCourse();
        student1 = userRepository.findOneByLogin(TEST_PREFIX + "student1").get();
        tutor1 = userRepository.findOneByLogin(TEST_PREFIX + "tutor1").get();
        tutorialGroup = createAndSaveTutorialGroup(course.getId(), new SecureRandom().nextInt(1000000000) + "", "LoremIpsum1", 10, false, "LoremIpsum1", Language.ENGLISH,
                userRepository.findOneByLogin(TEST_PREFIX + "tutor1").get(),
                Set.of(userRepository.findOneByLogin(TEST_PREFIX + "student1").get(), userRepository.findOneByLogin(TEST_PREFIX + "student2").get(),
                        userRepository.findOneByLogin(TEST_PREFIX + "student3").get(), userRepository.findOneByLogin(TEST_PREFIX + "student4").get(),
                        userRepository.findOneByLogin(TEST_PREFIX + "student5").get()));

        doNothing().when(javaMailSender).send(any(MimeMessage.class));
        tutorialGroupNotificationRepository.deleteAll();
    }

    private void verifyRepositoryCallWithCorrectNotification(int numberOfGroupsAndCalls, String expectedNotificationTitle) {
        List<TutorialGroupNotification> capturedNotifications = tutorialGroupNotificationRepository.findAll();
        Notification capturedNotification = capturedNotifications.get(0);
        assertThat(capturedNotification.getTitle()).isEqualTo(expectedNotificationTitle);
        assertThat(capturedNotifications).hasSize(numberOfGroupsAndCalls);
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    @WithMockUser(username = TEST_PREFIX + "instructor1", roles = "INSTRUCTOR")
    void notifyAboutTutorialGroupUpdate_shouldSaveAndSend(boolean contactTutor) {
        var setting1 = prepareNotificationSettingForTest(student1, NOTIFICATION__TUTORIAL_GROUP_NOTIFICATION__TUTORIAL_GROUP_DELETE_UPDATE);
        var setting2 = prepareNotificationSettingForTest(tutor1, NOTIFICATION__TUTORIAL_GROUP_NOTIFICATION__TUTORIAL_GROUP_DELETE_UPDATE);

        tutorialGroupNotificationService.notifyAboutTutorialGroupUpdate(tutorialGroup, contactTutor, "LoremIpsum");
        verifyRepositoryCallWithCorrectNotification(1, TUTORIAL_GROUP_UPDATED_TITLE);
        if (contactTutor) {
            verifyEmail(2);
        }
        else {
            verifyEmail(1);
        }

        notificationSettingRepository.deleteById(setting1.getId());
        notificationSettingRepository.deleteById(setting2.getId());
    }

    @Test
    @WithMockUser(username = TEST_PREFIX + "instructor1", roles = "INSTRUCTOR")
    void notifyAboutTutorialGroupDeletion_shouldSaveAndSend() {
        var setting = prepareNotificationSettingForTest(student1, NOTIFICATION__TUTORIAL_GROUP_NOTIFICATION__TUTORIAL_GROUP_DELETE_UPDATE);
        tutorialGroupNotificationService.notifyAboutTutorialGroupDeletion(tutorialGroup);
        verifyRepositoryCallWithCorrectNotification(1, TUTORIAL_GROUP_DELETED_TITLE);
        verifyEmail(1);

        notificationSettingRepository.deleteById(setting.getId());
    }

    private NotificationSetting prepareNotificationSettingForTest(User user, String notificationSettingIdentifier) {
        NotificationSetting notificationSetting = new NotificationSetting(user, true, true, notificationSettingIdentifier);
        notificationSettingRepository.save(notificationSetting);
        return notificationSetting;
    }

    private void verifyEmail(int times) {
        verify(javaMailSender, timeout(1500).times(times)).createMimeMessage();
    }

    private TutorialGroup createAndSaveTutorialGroup(Long courseId, String title, String additionalInformation, Integer capacity, Boolean isOnline, String campus,
            Language language, User teachingAssistant, Set<User> registeredStudents) {
        var course = courseRepository.findByIdElseThrow(courseId);

        var tutorialGroup = tutorialGroupRepository
                .saveAndFlush(new TutorialGroup(course, title, additionalInformation, capacity, isOnline, campus, language.name(), teachingAssistant, new HashSet<>()));

        var registrations = new HashSet<TutorialGroupRegistration>();
        for (var student : registeredStudents) {
            registrations.add(new TutorialGroupRegistration(student, tutorialGroup, TutorialGroupRegistrationType.INSTRUCTOR_REGISTRATION));
        }
        tutorialGroupRegistrationRepository.saveAllAndFlush(registrations);

        return tutorialGroupRepository.findByIdWithTeachingAssistantAndRegistrationsElseThrow(tutorialGroup.getId());
    }

}
