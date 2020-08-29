package org.jasig.ssp.service.impl;

import org.jasig.ssp.dao.EarlyAlertDao;
import org.jasig.ssp.model.EarlyAlert;
import org.jasig.ssp.model.reference.EarlyAlertReason;
import org.jasig.ssp.model.reference.EarlyAlertSuggestion;
import org.jasig.ssp.service.ObjectNotFoundException;
import org.jasig.ssp.service.PersonService;
import org.jasig.ssp.service.reference.EarlyAlertReasonService;
import org.jasig.ssp.service.reference.EarlyAlertSuggestionService;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.Set;

// total: 10 + 4 = 14

public class SaveEarlyAlert {

    // 1
    @Autowired
    private transient EarlyAlertDao dao;

    // 1
    @Autowired
    private transient PersonService personService;

    // 1
    @Autowired
    private transient EarlyAlertReasonService earlyAlertReasonService;

    // 1
    @Autowired
    private transient EarlyAlertSuggestionService earlyAlertSuggestionService;

    public EarlyAlert save(@NotNull final EarlyAlert obj)
            throws ObjectNotFoundException {
        final EarlyAlert current = dao.get(obj.getId());

        current.setCourseName(obj.getCourseName());
        current.setCourseTitle(obj.getCourseTitle());
        current.setEmailCC(obj.getEmailCC());
        current.setCampus(obj.getCampus());
        current.setEarlyAlertReasonOtherDescription(obj
                .getEarlyAlertReasonOtherDescription());
        current.setComment(obj.getComment());
        current.setClosedDate(obj.getClosedDate());

        // 2
        if ( obj.getClosedById() == null ) {
            current.setClosedBy(null);
        } else {
            current.setClosedBy(personService.get(obj.getClosedById()));
        }

        // 2
        if (obj.getPerson() == null) {
            current.setPerson(null);
        } else {
            current.setPerson(personService.get(obj.getPerson().getId()));
        }

        // 1
        final Set<EarlyAlertReason> earlyAlertReasons = new HashSet<EarlyAlertReason>();
        // 1
        if (obj.getEarlyAlertReasons() != null) {
            // 1
            for (final EarlyAlertReason reason : obj.getEarlyAlertReasons()) {
                earlyAlertReasons.add(earlyAlertReasonService.load(reason
                        .getId()));
            }
        }

        current.setEarlyAlertReasons(earlyAlertReasons);

        // 1
        final Set<EarlyAlertSuggestion> earlyAlertSuggestions = new HashSet<EarlyAlertSuggestion>();
        // 1
        if (obj.getEarlyAlertSuggestions() != null) {
            // 1
            for (final EarlyAlertSuggestion reason : obj
                    .getEarlyAlertSuggestions()) {
                earlyAlertSuggestions.add(earlyAlertSuggestionService
                        .load(reason
                                .getId()));
            }
        }

        current.setEarlyAlertSuggestions(earlyAlertSuggestions);

        return dao.save(current);
    }
}
