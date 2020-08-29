package org.jasig.ssp.service.impl;

import org.jasig.ssp.dao.EarlyAlertDao;
import org.jasig.ssp.model.*;
import org.jasig.ssp.model.reference.ProgramStatus;
import org.jasig.ssp.model.reference.StudentType;
import org.jasig.ssp.service.MessageService;
import org.jasig.ssp.service.ObjectNotFoundException;
import org.jasig.ssp.service.PersonProgramStatusService;
import org.jasig.ssp.service.PersonService;
import org.jasig.ssp.service.reference.*;
import org.jasig.ssp.web.api.validation.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import javax.mail.SendFailedException;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

// total: 28 + 8 = 36

public class CreateEarlyAlert {

    // 1
    @Autowired
    private transient PersonService personService;

    // 1
    @Autowired
    private transient ConfigService configService;

    // 1
    @Autowired
    private transient EarlyAlertDao dao;

    // 1
    @Autowired
    private transient ProgramStatusService programStatusService;

    // 1
    @Autowired
    private transient PersonProgramStatusService personProgramStatusService;

    // 1
    @Autowired
    private transient StudentTypeService studentTypeService;

    // 1
    @Autowired
    private transient MessageTemplateService messageTemplateService;

    // 1
    @Autowired
    private transient MessageService messageService;

    @Autowired
    private transient FillTemplateParameters fillTemplate;

    // 2
    private static final Logger LOGGER = LoggerFactory
            .getLogger(EarlyAlertServiceImpl.class);

    @Transactional(rollbackFor = { ObjectNotFoundException.class, ValidationException.class })
    public EarlyAlert create(@NotNull final EarlyAlert earlyAlert)
            throws ObjectNotFoundException, ValidationException {
        // Validate objects
        // 1
        if (earlyAlert == null) {
            throw new IllegalArgumentException("EarlyAlert must be provided.");
        }

        // 1
        if (earlyAlert.getPerson() == null) {
            throw new ValidationException(
                    "EarlyAlert Student data must be provided.");
        }

        // 1
        final Person student = earlyAlert.getPerson();

        // Figure student advisor or early alert coordinator
        final UUID assignedAdvisor = getEarlyAlertAdvisor(earlyAlert);
        // 1
        if (assignedAdvisor == null) {
            throw new ValidationException(
                    "Could not determine the Early Alert Advisor for student ID "
                            + student.getId());
        }

        // 1
        if (student.getCoach() == null
                || assignedAdvisor.equals(student.getCoach().getId())) {
            student.setCoach(personService.get(assignedAdvisor));
        }

        ensureValidAlertedOnPersonStateNoFail(student);

        // Create alert
        final EarlyAlert saved = dao.save(earlyAlert);

        // Send e-mail to assigned advisor (coach)
        // 2
        try {
            SendMessageToAdvisor sender = new SendMessageToAdvisor();
            sender.send(earlyAlert, earlyAlert.getEmailCC());
        } catch (final SendFailedException e) {
            LOGGER.warn(
                    "Could not send Early Alert message to advisor.",
                    e);
            // 1
            throw new ValidationException(
                    "Early Alert notification e-mail could not be sent to advisor. Early Alert was NOT created.",
                    e);
        }

        // Send e-mail CONFIRMATION to faculty
        // 2
        try {
            sendConfirmationMessageToFaculty(saved);
        } catch (final SendFailedException e) {
            LOGGER.warn(
                    "Could not send Early Alert confirmation to faculty.",
                    e);
            throw new ValidationException(
                    "Early Alert confirmation e-mail could not be sent. Early Alert was NOT created.",
                    e);
        }

        return saved;
    }

    /**
     * Business logic to determine the advisor that is assigned to the student
     * for this Early Alert.
     *
     * @param earlyAlert
     *            EarlyAlert instance
     * @throws ValidationException
     *             If Early Alert, Student, and/or system information could not
     *             determine the advisor for this student.
     * @return The assigned advisor
     */
    private UUID getEarlyAlertAdvisor(final EarlyAlert earlyAlert)
            throws ValidationException {
        // Check for student already assigned to an advisor (a.k.a. coach)
        // 1
        if ((earlyAlert.getPerson().getCoach() != null) &&
                (earlyAlert.getPerson().getCoach().getId() != null)) {
            return earlyAlert.getPerson().getCoach().getId();
        }

        // Get campus Early Alert coordinator
        // 1
        if (earlyAlert.getCampus() == null) {
            throw new IllegalArgumentException("Campus ID can not be null.");
        }

        // 1
        if (earlyAlert.getCampus().getEarlyAlertCoordinatorId() != null) {
            // Return Early Alert coordinator UUID
            return earlyAlert.getCampus().getEarlyAlertCoordinatorId();
        }

        // TODO If no campus EA Coordinator, assign to default EA Coordinator
        // (which is not yet implemented)

        // getEarlyAlertAdvisor should never return null
        throw new ValidationException(
                "Could not determined the Early Alert Coordinator for this student. Ensure that a default coordinator is set globally and for all campuses.");
    }

    private void ensureValidAlertedOnPersonStateNoFail(Person person) {
        try {
            ensureValidAlertedOnPersonStateOrFail(person);
        } catch ( Exception e ) {
            LOGGER.error("Unable to set a program status or student type on "
                    + "person '{}'. This is likely to prevent that person "
                    + "record from appearing in caseloads, student searches, "
                    + "and some reports.", person.getId(), e);
        }
    }

    private void ensureValidAlertedOnPersonStateOrFail(Person person)
            throws ObjectNotFoundException, ValidationException {

        // 1
        if ( person.getObjectStatus() != ObjectStatus.ACTIVE ) {
            person.setObjectStatus(ObjectStatus.ACTIVE);
        }

        // 1
        final ProgramStatus programStatus =  programStatusService.getActiveStatus();
        // 1
        if ( programStatus == null ) {
            throw new ObjectNotFoundException(
                    "Unable to find a ProgramStatus representing \"activeness\".",
                    "ProgramStatus");
        }

        // 1
        Set<PersonProgramStatus> programStatuses =
                person.getProgramStatuses();
        // 1
        if ( programStatuses == null || programStatuses.isEmpty() ) {
            PersonProgramStatus personProgramStatus = new PersonProgramStatus();
            personProgramStatus.setEffectiveDate(new Date());
            personProgramStatus.setProgramStatus(programStatus);
            personProgramStatus.setPerson(person);
            programStatuses.add(personProgramStatus);
            person.setProgramStatuses(programStatuses);
            // save should cascade, but make sure custom create logic fires
            personProgramStatusService.create(personProgramStatus);
        }

        // 1
        if ( person.getStudentType() == null ) {
            // 1
            StudentType studentType = studentTypeService.get(StudentType.EAL_ID);
            // 1
            if ( studentType == null ) {
                throw new ObjectNotFoundException(
                        "Unable to find a StudentType representing an early "
                                + "alert-assigned type.", "StudentType");
            }
            person.setStudentType(studentType);
        }
    }

    /**
     * Send confirmation e-mail ({@link Message}) to the faculty who created
     * this alert.
     *
     * @param earlyAlert
     *            Early Alert
     * @throws ObjectNotFoundException
     * @throws SendFailedException
     * @throws ValidationException
     */
    private void sendConfirmationMessageToFaculty(final EarlyAlert earlyAlert)
            throws ObjectNotFoundException, SendFailedException,
            ValidationException {
        // 1
        if (earlyAlert == null) {
            throw new IllegalArgumentException("EarlyAlert was missing.");
        }

        // 1
        if (earlyAlert.getPerson() == null) {
            throw new IllegalArgumentException("EarlyAlert.Person is missing.");
        }

        // 1
        if (!configService.getByNameOrDefaultValue("send_faculty_mail")) {
            LOGGER.debug("Skipping Faculty Early Alert Confirmation Email: Config Turned Off");
            return; //skip if faculty early alert email turned off
        }

        final UUID personId = earlyAlert.getCreatedBy().getId();
        Person person = personService.get(personId);
        // 2
        if ( person == null ) {
            LOGGER.warn("EarlyAlert {} has no creator. Unable to send"
                    + " confirmation message to faculty.", earlyAlert);
        } else {
            final SubjectAndBody subjAndBody = messageTemplateService
                    .createEarlyAlertFacultyConfirmationMessage(fillTemplate.fill(earlyAlert));

            // Create and queue the message
            final Message message = messageService.createMessage(person, null,
                    subjAndBody);

            LOGGER.info("Message {} created for EarlyAlert {}", message, earlyAlert);
        }
    }
}
