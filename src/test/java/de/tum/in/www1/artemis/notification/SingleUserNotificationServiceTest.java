package de.tum.in.www1.artemis.notification;

import static de.tum.in.www1.artemis.domain.enumeration.NotificationType.CONVERSATION_ADD_USER_CHANNEL;
import static de.tum.in.www1.artemis.domain.enumeration.NotificationType.CONVERSATION_ADD_USER_GROUP_CHAT;
import static de.tum.in.www1.artemis.domain.enumeration.NotificationType.CONVERSATION_CREATE_GROUP_CHAT;
import static de.tum.in.www1.artemis.domain.enumeration.NotificationType.CONVERSATION_CREATE_ONE_TO_ONE_CHAT;
import static de.tum.in.www1.artemis.domain.enumeration.NotificationType.CONVERSATION_DELETE_CHANNEL;
import static de.tum.in.www1.artemis.domain.enumeration.NotificationType.CONVERSATION_REMOVE_USER_CHANNEL;
import static de.tum.in.www1.artemis.domain.enumeration.NotificationType.CONVERSATION_REMOVE_USER_GROUP_CHAT;
import static de.tum.in.www1.artemis.domain.notification.NotificationConstants.*;
import static de.tum.in.www1.artemis.service.notifications.NotificationSettingsService.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import de.tum.in.www1.artemis.AbstractSpringIntegrationBambooBitbucketJiraTest;
import de.tum.in.www1.artemis.domain.*;
import de.tum.in.www1.artemis.domain.enumeration.AssessmentType;
import de.tum.in.www1.artemis.domain.enumeration.NotificationType;
import de.tum.in.www1.artemis.domain.metis.AnswerPost;
import de.tum.in.www1.artemis.domain.metis.ConversationParticipant;
import de.tum.in.www1.artemis.domain.metis.Post;
import de.tum.in.www1.artemis.domain.metis.conversation.Channel;
import de.tum.in.www1.artemis.domain.metis.conversation.GroupChat;
import de.tum.in.www1.artemis.domain.metis.conversation.OneToOneChat;
import de.tum.in.www1.artemis.domain.notification.Notification;
import de.tum.in.www1.artemis.domain.notification.SingleUserNotification;
import de.tum.in.www1.artemis.domain.plagiarism.PlagiarismCase;
import de.tum.in.www1.artemis.domain.plagiarism.PlagiarismComparison;
import de.tum.in.www1.artemis.domain.plagiarism.PlagiarismSubmission;
import de.tum.in.www1.artemis.domain.plagiarism.PlagiarismVerdict;
import de.tum.in.www1.artemis.domain.plagiarism.text.TextPlagiarismResult;
import de.tum.in.www1.artemis.domain.plagiarism.text.TextSubmissionElement;
import de.tum.in.www1.artemis.domain.tutorialgroups.TutorialGroup;
import de.tum.in.www1.artemis.repository.ExerciseRepository;
import de.tum.in.www1.artemis.repository.NotificationRepository;
import de.tum.in.www1.artemis.repository.NotificationSettingRepository;
import de.tum.in.www1.artemis.repository.ResultRepository;
import de.tum.in.www1.artemis.security.SecurityUtils;
import de.tum.in.www1.artemis.service.notifications.SingleUserNotificationService;
import de.tum.in.www1.artemis.util.ModelFactory;

class SingleUserNotificationServiceTest extends AbstractSpringIntegrationBambooBitbucketJiraTest {

    private static final String TEST_PREFIX = "singleusernotification";

    @Autowired
    private SingleUserNotificationService singleUserNotificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationSettingRepository notificationSettingRepository;

    @Autowired
    private ExerciseRepository exerciseRepository;

    @Autowired
    private ResultRepository resultRepository;

    private User user;

    private User userTwo;

    private User userThree;

    private FileUploadExercise fileUploadExercise;

    private Post post;

    private static final String POST_TITLE = "post title";

    private static final String POST_CONTENT = "post content";

    private AnswerPost answerPost;

    private static final String ANSWER_POST_CONTENT = "answer post content";

    private Course course;

    private static final String COURSE_TITLE = "course title";

    private static final String LECTURE_TITLE = "lecture title";

    private Exercise exercise;

    private PlagiarismCase plagiarismCase;

    private Result result;

    private TutorialGroup tutorialGroup;

    private OneToOneChat oneToOneChat;

    private GroupChat groupChat;

    private Channel channel;

    /**
     * Sets up all needed mocks and their wanted behavior
     */
    @BeforeEach
    void setUp() {
        SecurityUtils.setAuthorizationObject();

        course = database.createCourse();
        course.setTitle(COURSE_TITLE);

        database.addUsers(TEST_PREFIX, 3, 0, 0, 0);
        user = database.getUserByLogin(TEST_PREFIX + "student1");
        userTwo = database.getUserByLogin(TEST_PREFIX + "student2");
        userThree = database.getUserByLogin(TEST_PREFIX + "student3");

        notificationRepository.deleteAllInBatch();

        exercise = new TextExercise();
        exercise.setCourse(course);
        exercise.setMaxPoints(10D);

        fileUploadExercise = new FileUploadExercise();
        fileUploadExercise.setCourse(course);

        Lecture lecture = new Lecture();
        lecture.setCourse(course);
        lecture.setTitle(LECTURE_TITLE);

        post = new Post();
        post.setExercise(exercise);
        post.setLecture(lecture);
        post.setAuthor(user);
        post.setCourse(course);
        post.setTitle(POST_TITLE);
        post.setContent(POST_CONTENT);

        Post answerPostPost = new Post();
        answerPostPost.setExercise(exercise);
        answerPostPost.setLecture(lecture);
        answerPostPost.setAuthor(user);
        answerPostPost.setCourse(course);
        answerPost = new AnswerPost();
        answerPost.setPost(new Post());
        answerPost.setAuthor(user);
        answerPost.setContent(ANSWER_POST_CONTENT);

        PlagiarismSubmission<TextSubmissionElement> plagiarismSubmission = new PlagiarismSubmission<>();
        plagiarismSubmission.setStudentLogin(user.getLogin());

        TextPlagiarismResult plagiarismResult = new TextPlagiarismResult();
        plagiarismResult.setExercise(exercise);

        PlagiarismComparison<TextSubmissionElement> plagiarismComparison = new PlagiarismComparison<>();
        plagiarismComparison.setSubmissionA(plagiarismSubmission);
        plagiarismComparison.setPlagiarismResult(plagiarismResult);

        plagiarismCase = new PlagiarismCase();
        plagiarismCase.setExercise(exercise);

        result = new Result();
        result.setScore(1D);
        result.setCompletionDate(ZonedDateTime.now().minusMinutes(1));

        tutorialGroup = new TutorialGroup();
        tutorialGroup.setCourse(course);
        tutorialGroup.setTeachingAssistant(userTwo);

        oneToOneChat = new OneToOneChat();
        oneToOneChat.setCourse(course);
        oneToOneChat.setCreator(userTwo);
        oneToOneChat.setCreationDate(ZonedDateTime.now());
        ConversationParticipant conversationParticipant1 = new ConversationParticipant();
        conversationParticipant1.setUser(user);
        ConversationParticipant conversationParticipant2 = new ConversationParticipant();
        conversationParticipant2.setUser(userTwo);
        oneToOneChat.setConversationParticipants(Set.of(conversationParticipant1, conversationParticipant2));

        groupChat = new GroupChat();
        groupChat.setCourse(course);
        groupChat.setCreator(userTwo);
        groupChat.setCreationDate(ZonedDateTime.now());
        ConversationParticipant conversationParticipant3 = new ConversationParticipant();
        conversationParticipant1.setUser(userThree);
        groupChat.setConversationParticipants(Set.of(conversationParticipant1, conversationParticipant2, conversationParticipant3));

        channel = new Channel();
        channel.setCourse(course);
        channel.setName("test");
        channel.setCreator(userTwo);
        channel.setCreationDate(ZonedDateTime.now());
        channel.setConversationParticipants(Set.of(conversationParticipant1, conversationParticipant2, conversationParticipant3));

        doNothing().when(javaMailSender).send(any(MimeMessage.class));
    }

    /**
     * Auxiliary method that checks if the groupNotificationRepository was called once successfully with the correct notification (type)
     *
     * @param expectedNotificationTitle is the title (NotificationTitleTypeConstants) of the expected notification
     */
    private void verifyRepositoryCallWithCorrectNotification(String expectedNotificationTitle) {
        Notification capturedNotification = notificationRepository.findAll().get(0);
        assertThat(capturedNotification.getTitle()).as("Title of the captured notification should be equal to the expected one").isEqualTo(expectedNotificationTitle);
    }

    /// General notify Tests

    /**
     * Tests if no notification (or email) is sent if the settings are deactivated
     * However, the notification has to be saved to the DB
     */
    @Test
    void testSendNoNotificationOrEmailWhenSettingsAreDeactivated() {
        notificationSettingRepository.save(new NotificationSetting(user, false, true, NOTIFICATION__EXERCISE_NOTIFICATION__NEW_REPLY_FOR_EXERCISE_POST));
        assertThat(notificationRepository.findAll()).as("No notifications should be present prior to the method call").isEmpty();

        singleUserNotificationService.notifyUserAboutNewReplyForExercise(post, answerPost, course);

        assertThat(notificationRepository.findAll()).as("The notification should have been saved to the DB").hasSize(1);
        // no web app notification or email should be sent
        verify(messagingTemplate, times(0)).convertAndSend(any());
    }

    /**
     * Test for notifyStudentGroupAboutAttachmentChange method
     */
    @Test
    void testNotifyUserAboutNewAnswerForExercise() {
        singleUserNotificationService.notifyUserAboutNewReplyForExercise(post, answerPost, course);
        verifyRepositoryCallWithCorrectNotification(NEW_REPLY_FOR_EXERCISE_POST_TITLE);
    }

    /**
     * Test for notifyUserAboutNewAnswerForLecture method
     */
    @Test
    void testNotifyUserAboutNewAnswerForLecture() {
        singleUserNotificationService.notifyUserAboutNewReplyForLecture(post, answerPost, course);
        verifyRepositoryCallWithCorrectNotification(NEW_REPLY_FOR_LECTURE_POST_TITLE);
    }

    /**
     * Test for notifyUserAboutNewAnswerForCoursePost method
     */
    @Test
    void testNotifyUserAboutNewAnswerForCoursePost() {
        singleUserNotificationService.notifyUserAboutNewReplyForCoursePost(post, answerPost, course);
        verifyRepositoryCallWithCorrectNotification(NEW_REPLY_FOR_COURSE_POST_TITLE);
    }

    /**
     * Test for notifyUserAboutSuccessfulFileUploadSubmission method
     */
    @Test
    void testNotifyUserAboutSuccessfulFileUploadSubmission() {
        notificationSettingRepository.save(new NotificationSetting(user, true, true, NOTIFICATION__EXERCISE_NOTIFICATION__FILE_SUBMISSION_SUCCESSFUL));
        singleUserNotificationService.notifyUserAboutSuccessfulFileUploadSubmission(fileUploadExercise, user);
        verifyRepositoryCallWithCorrectNotification(FILE_SUBMISSION_SUCCESSFUL_TITLE);
        verifyEmail();
    }

    // AssessedExerciseSubmission related

    /**
     * Test for notifyUserAboutAssessedExerciseSubmission method
     */
    @Test
    void testNotifyUserAboutAssessedExerciseSubmission() {
        NotificationSetting notificationSetting = new NotificationSetting(user, true, true, NOTIFICATION__EXERCISE_NOTIFICATION__EXERCISE_SUBMISSION_ASSESSED);
        notificationSettingRepository.save(notificationSetting);

        singleUserNotificationService.checkNotificationForAssessmentExerciseSubmission(exercise, user, result);

        verifyRepositoryCallWithCorrectNotification(EXERCISE_SUBMISSION_ASSESSED_TITLE);
        verifyEmail();
    }

    /**
     * Test for checkNotificationForAssessmentExerciseSubmission method with an undefined release date
     */
    @Test
    void testCheckNotificationForAssessmentExerciseSubmission_undefinedAssessmentDueDate() {
        exercise = ModelFactory.generateTextExercise(null, null, null, course);
        singleUserNotificationService.checkNotificationForAssessmentExerciseSubmission(exercise, user, result);
        verify(singleUserNotificationService, times(1)).checkNotificationForAssessmentExerciseSubmission(exercise, user, result);
    }

    /**
     * Test for checkNotificationForExerciseRelease method with a current or past release date
     */
    @Test
    void testCheckNotificationForAssessmentExerciseSubmission_currentOrPastAssessmentDueDate() {
        exercise = ModelFactory.generateTextExercise(null, null, ZonedDateTime.now(), course);
        singleUserNotificationService.checkNotificationForAssessmentExerciseSubmission(exercise, user, result);
        assertThat(notificationRepository.findAll()).as("One new notification should have been created").hasSize(1);
    }

    /**
     * Test for checkNotificationForExerciseRelease method with a future release date
     */
    @Test
    void testCheckNotificationForAssessmentExerciseSubmission_futureAssessmentDueDate() {
        exercise = ModelFactory.generateTextExercise(null, null, ZonedDateTime.now().plusHours(1), course);
        singleUserNotificationService.checkNotificationForAssessmentExerciseSubmission(exercise, user, result);
        assertThat(notificationRepository.findAll()).as("No new notification should have been created").isEmpty();
    }

    @Test
    void testNotifyUsersAboutAssessedExerciseSubmission() {
        Course testCourse = database.addCourseWithFileUploadExercise();
        Exercise testExercise = testCourse.getExercises().iterator().next();

        User studentWithParticipationAndSubmissionAndAutomaticResult = database.getUserByLogin(TEST_PREFIX + "student1");
        User studentWithParticipationAndSubmissionAndManualResult = database.getUserByLogin(TEST_PREFIX + "student2");
        User studentWithParticipationButWithoutSubmission = database.getUserByLogin(TEST_PREFIX + "student3");

        database.createParticipationSubmissionAndResult(testExercise.getId(), studentWithParticipationAndSubmissionAndAutomaticResult, 10.0, 10.0, 50, true);
        Result manualResult = database.createParticipationSubmissionAndResult(testExercise.getId(), studentWithParticipationAndSubmissionAndManualResult, 10.0, 10.0, 50, true);
        manualResult.setAssessmentType(AssessmentType.MANUAL);
        resultRepository.save(manualResult);
        database.createAndSaveParticipationForExercise(testExercise, studentWithParticipationButWithoutSubmission.getLogin());

        testExercise = exerciseRepository.findAllExercisesByCourseId(testCourse.getId()).iterator().next();

        singleUserNotificationService.notifyUsersAboutAssessedExerciseSubmission(testExercise);

        List<Notification> sentNotifications = notificationRepository.findAll();

        assertThat(sentNotifications).as("Only one notification should have been created (for the user with a valid participation, submission, and manual result)").hasSize(1);
        assertThat(sentNotifications.get(0)).isInstanceOf(SingleUserNotification.class);
        assertThat(((SingleUserNotification) sentNotifications.get(0)).getRecipient()).isEqualTo(studentWithParticipationAndSubmissionAndManualResult);
    }

    // Plagiarism related

    /**
     * Test for notifyUserAboutNewPossiblePlagiarismCase method
     */
    @Test
    void testNotifyUserAboutNewPossiblePlagiarismCase() throws MessagingException {
        // explicitly change the user to prevent issues in the following server call due to userRepository.getUser() (@WithMockUser is not working here)
        database.changeUser(TEST_PREFIX + "student1");
        String exerciseTitle = "Test New Plagiarism";
        exercise.setTitle(exerciseTitle);
        post.setPlagiarismCase(plagiarismCase);
        plagiarismCase.setPost(post);
        singleUserNotificationService.notifyUserAboutNewPlagiarismCase(plagiarismCase, user);
        verifyRepositoryCallWithCorrectNotification(NEW_PLAGIARISM_CASE_STUDENT_TITLE);
        ArgumentCaptor<MimeMessage> mimeMessageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(javaMailSender, timeout(1000).times(1)).send(mimeMessageCaptor.capture());
        assertThat(mimeMessageCaptor.getValue().getSubject()).isEqualTo("New Plagiarism Case: Exercise \"" + exerciseTitle + "\" in the course \"" + course.getTitle() + "\"");
    }

    /**
     * Test for notifyUserAboutFinalPlagiarismState method
     */
    @Test
    void testNotifyUserAboutFinalPlagiarismState() throws MessagingException {
        // explicitly change the user to prevent issues in the following server call due to userRepository.getUser() (@WithMockUser is not working here)
        database.changeUser(TEST_PREFIX + "student1");
        plagiarismCase.setVerdict(PlagiarismVerdict.NO_PLAGIARISM);
        singleUserNotificationService.notifyUserAboutPlagiarismCaseVerdict(plagiarismCase, user);
        verifyRepositoryCallWithCorrectNotification(PLAGIARISM_CASE_VERDICT_STUDENT_TITLE);
        ArgumentCaptor<MimeMessage> mimeMessageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(javaMailSender, timeout(1000).times(1)).send(mimeMessageCaptor.capture());
        assertThat(mimeMessageCaptor.getValue().getSubject()).isEqualTo(PLAGIARISM_CASE_VERDICT_STUDENT_TITLE);
    }

    @Test
    void testConversationNotificationsOneToOneChatCreation() {
        var notificationsBefore = (int) notificationRepository.count();
        singleUserNotificationService.notifyClientAboutConversationCreationOrDeletion(oneToOneChat, user, userTwo, CONVERSATION_CREATE_ONE_TO_ONE_CHAT);
        List<Notification> capturedNotifications = notificationRepository.findAll();
        assertThat(capturedNotifications).as("Notification should not have been saved").hasSize(notificationsBefore);
        // notification should be sent
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/user/" + user.getId() + "/notifications"), (Object) any());
    }

    @Test
    void testConversationNotificationsGroupChatCreation() {
        int notificationsBefore = (int) notificationRepository.count();
        singleUserNotificationService.notifyClientAboutConversationCreationOrDeletion(groupChat, user, userTwo, CONVERSATION_CREATE_GROUP_CHAT);
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/user/" + user.getId() + "/notifications"), (Object) any());

        singleUserNotificationService.notifyClientAboutConversationCreationOrDeletion(groupChat, userThree, userTwo, CONVERSATION_CREATE_GROUP_CHAT);
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/user/" + userThree.getId() + "/notifications"), (Object) any());

        List<Notification> capturedNotifications = notificationRepository.findAll();
        assertThat(capturedNotifications).as("Both notifications should have been saved").hasSize(notificationsBefore + 2);
        capturedNotifications.forEach(capturedNotification -> {
            assertThat(capturedNotification.getTitle()).as("Title of the captured notification should be equal to the expected one")
                    .isEqualTo(CONVERSATION_CREATE_GROUP_CHAT_TITLE);
        });
    }

    @ParameterizedTest
    @MethodSource("getNotificationTypesAndTitlesParametersForGroupChat")
    void testConversationNotificationsGroupChatAddAndRemoveUsers(NotificationType notificationType, String expectedTitle) {
        singleUserNotificationService.notifyClientAboutConversationCreationOrDeletion(groupChat, user, userTwo, notificationType);
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/user/" + user.getId() + "/notifications"), (Object) any());

        verifyRepositoryCallWithCorrectNotification(expectedTitle);
    }

    @ParameterizedTest
    @MethodSource("getNotificationTypesAndTitlesParametersForChannel")
    void testConversationNotificationsChannel(NotificationType notificationType, String expectedTitle) {
        singleUserNotificationService.notifyClientAboutConversationCreationOrDeletion(channel, user, userTwo, notificationType);
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/user/" + user.getId() + "/notifications"), (Object) any());

        verifyRepositoryCallWithCorrectNotification(expectedTitle);
    }

    @Test
    void testConversationNotificationsNewMessageReply() {
        Post post = new Post();
        post.setAuthor(user);
        post.setCreationDate(ZonedDateTime.now());
        post.setConversation(groupChat);
        post.setCourse(course);

        AnswerPost answerPost = new AnswerPost();
        answerPost.setAuthor(userTwo);
        answerPost.setCreationDate(ZonedDateTime.now().plusSeconds(5));
        answerPost.setPost(post);

        singleUserNotificationService.notifyUserAboutNewMessageReply(answerPost, user, userTwo);
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/user/" + user.getId() + "/notifications"), (Object) any());

        verifyRepositoryCallWithCorrectNotification(MESSAGE_REPLY_IN_CONVERSATION_TITLE);
    }

    // Tutorial Group related

    @Test
    void testTutorialGroupNotifications_studentRegistration() {
        notificationSettingRepository.deleteAll();
        notificationSettingRepository.save(new NotificationSetting(user, true, true, NOTIFICATION__TUTORIAL_GROUP_NOTIFICATION__TUTORIAL_GROUP_REGISTRATION));
        singleUserNotificationService.notifyStudentAboutRegistrationToTutorialGroup(tutorialGroup, user, userTwo);
        verifyRepositoryCallWithCorrectNotification(TUTORIAL_GROUP_REGISTRATION_STUDENT_TITLE);
        verifyEmail();
    }

    @Test
    void testTutorialGroupNotifications_studentDeregistration() {
        notificationSettingRepository.deleteAll();
        notificationSettingRepository.save(new NotificationSetting(user, true, true, NOTIFICATION__TUTORIAL_GROUP_NOTIFICATION__TUTORIAL_GROUP_REGISTRATION));
        singleUserNotificationService.notifyStudentAboutDeregistrationFromTutorialGroup(tutorialGroup, user, userTwo);
        verifyRepositoryCallWithCorrectNotification(TUTORIAL_GROUP_DEREGISTRATION_STUDENT_TITLE);
        verifyEmail();
    }

    @Test
    void testTutorialGroupNotifications_tutorRegistration() {
        notificationSettingRepository.deleteAll();
        notificationSettingRepository
                .save(new NotificationSetting(tutorialGroup.getTeachingAssistant(), true, true, NOTIFICATION__TUTOR_NOTIFICATION__TUTORIAL_GROUP_REGISTRATION));
        singleUserNotificationService.notifyTutorAboutRegistrationToTutorialGroup(tutorialGroup, user, userThree);
        verifyRepositoryCallWithCorrectNotification(TUTORIAL_GROUP_REGISTRATION_TUTOR_TITLE);
        verifyEmail();

    }

    @Test
    void testTutorialGroupNotifications_tutorRegistrationMultiple() {
        notificationSettingRepository.deleteAll();
        notificationSettingRepository
                .save(new NotificationSetting(tutorialGroup.getTeachingAssistant(), true, true, NOTIFICATION__TUTOR_NOTIFICATION__TUTORIAL_GROUP_REGISTRATION));
        singleUserNotificationService.notifyTutorAboutMultipleRegistrationsToTutorialGroup(tutorialGroup, Set.of(user), userThree);
        verifyRepositoryCallWithCorrectNotification(TUTORIAL_GROUP_REGISTRATION_MULTIPLE_TUTOR_TITLE);
        verifyEmail();
    }

    @Test
    void testTutorialGroupNotifications_tutorDeregistration() {
        notificationSettingRepository.deleteAll();
        notificationSettingRepository
                .save(new NotificationSetting(tutorialGroup.getTeachingAssistant(), true, true, NOTIFICATION__TUTOR_NOTIFICATION__TUTORIAL_GROUP_REGISTRATION));
        singleUserNotificationService.notifyTutorAboutDeregistrationFromTutorialGroup(tutorialGroup, user, userThree);
        verifyRepositoryCallWithCorrectNotification(TUTORIAL_GROUP_DEREGISTRATION_TUTOR_TITLE);
        verifyEmail();
    }

    @Test
    void testTutorialGroupNotifications_groupAssigned() {
        notificationSettingRepository.deleteAll();
        notificationSettingRepository
                .save(new NotificationSetting(tutorialGroup.getTeachingAssistant(), true, true, NOTIFICATION__TUTOR_NOTIFICATION__TUTORIAL_GROUP_ASSIGN_UNASSIGN));
        singleUserNotificationService.notifyTutorAboutAssignmentToTutorialGroup(tutorialGroup, tutorialGroup.getTeachingAssistant(), userThree);
        verifyRepositoryCallWithCorrectNotification(TUTORIAL_GROUP_ASSIGNED_TITLE);
        verifyEmail();
    }

    @Test
    void testTutorialGroupNotifications_groupUnassigned() {
        notificationSettingRepository.deleteAll();
        notificationSettingRepository
                .save(new NotificationSetting(tutorialGroup.getTeachingAssistant(), true, true, NOTIFICATION__TUTOR_NOTIFICATION__TUTORIAL_GROUP_ASSIGN_UNASSIGN));
        singleUserNotificationService.notifyTutorAboutUnassignmentFromTutorialGroup(tutorialGroup, tutorialGroup.getTeachingAssistant(), userThree);
        verifyRepositoryCallWithCorrectNotification(TUTORIAL_GROUP_UNASSIGNED_TITLE);
        verifyEmail();
    }

    /**
     * Checks if an email was created and send
     */
    private void verifyEmail() {
        verify(javaMailSender, timeout(1000).times(1)).send(any(MimeMessage.class));
    }

    private static Stream<Arguments> getNotificationTypesAndTitlesParametersForGroupChat() {
        return Stream.of(Arguments.of(CONVERSATION_ADD_USER_GROUP_CHAT, CONVERSATION_ADD_USER_GROUP_CHAT_TITLE),
                Arguments.of(CONVERSATION_REMOVE_USER_GROUP_CHAT, CONVERSATION_REMOVE_USER_GROUP_CHAT_TITLE));
    }

    private static Stream<Arguments> getNotificationTypesAndTitlesParametersForChannel() {
        return Stream.of(Arguments.of(CONVERSATION_ADD_USER_CHANNEL, CONVERSATION_ADD_USER_CHANNEL_TITLE),
                Arguments.of(CONVERSATION_REMOVE_USER_CHANNEL, CONVERSATION_REMOVE_USER_CHANNEL_TITLE),
                Arguments.of(CONVERSATION_DELETE_CHANNEL, CONVERSATION_DELETE_CHANNEL_TITLE));
    }
}
