package org.jasig.ssp.service.impl;

import org.apache.commons.lang.StringUtils;
import org.jasig.ssp.dao.JournalEntryDao;
import org.jasig.ssp.dao.PersonDao;
import org.jasig.ssp.model.ObjectStatus;
import org.jasig.ssp.service.ObjectNotFoundException;
import org.jasig.ssp.transferobject.reports.BaseStudentReportTO;
import org.jasig.ssp.transferobject.reports.JournalCaseNotesStudentReportTO;
import org.jasig.ssp.transferobject.reports.JournalStepSearchFormTO;
import org.jasig.ssp.util.sort.PagingWrapper;
import org.jasig.ssp.util.sort.SortingAndPaging;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

public class FindPeopleWithJournalEntries {

    // 1
    @Autowired
    private transient JournalEntryDao dao;

    @Autowired
    // 1
    private transient PersonDao personDao;

    // 3
    public List<JournalCaseNotesStudentReportTO>
    find(
            JournalStepSearchFormTO personSearchForm,
            SortingAndPaging sAndP
    ) throws ObjectNotFoundException {
        final List<JournalCaseNotesStudentReportTO> peopleWithJournalEntries =
                dao.getJournalCaseNoteStudentReportTOsFromCriteria(personSearchForm, sAndP);

        final Map<String, JournalCaseNotesStudentReportTO> map = new HashMap<String, JournalCaseNotesStudentReportTO>();

        // 1
        for(JournalCaseNotesStudentReportTO entry:peopleWithJournalEntries){
            map.put(entry.getSchoolId(), entry);
        }

        final SortingAndPaging personSAndP = SortingAndPaging.createForSingleSortAll(ObjectStatus.ACTIVE, "lastName", "DESC") ;
        // 2
        final PagingWrapper<BaseStudentReportTO> persons = personDao.getStudentReportTOs(personSearchForm, personSAndP);

        // 1
        if (persons == null) {
            return peopleWithJournalEntries;
        }

        // 1
        for (BaseStudentReportTO person:persons) {
            addPersonIfNewSchool(personSearchForm, peopleWithJournalEntries, map, person);
        }

        sortByStudentName(peopleWithJournalEntries);

        return peopleWithJournalEntries;
    }

    private void addPersonIfNewSchool(
            JournalStepSearchFormTO personSearchForm,
            List<JournalCaseNotesStudentReportTO> personsWithJournalEntries,
            Map<String, JournalCaseNotesStudentReportTO> map, BaseStudentReportTO person
    ) {
        // 1
        if (map.containsKey(person.getSchoolId()) || StringUtils.isBlank(person.getCoachSchoolId())) {
            return;
        }

        // 1
        if (personSearchForm.getJournalSourceIds() != null
                && (dao.getJournalCountForPersonForJournalSourceIds(person.getId(), personSearchForm.getJournalSourceIds()) == 0)
        ) {
            return;
        }

        final JournalCaseNotesStudentReportTO entry = new JournalCaseNotesStudentReportTO(person);
        personsWithJournalEntries.add(entry);
        map.put(entry.getSchoolId(), entry);
    }

    // total: 6
    private static void sortByStudentName(List<JournalCaseNotesStudentReportTO> toSort) {
        // 1
        Collections.sort(toSort, new Comparator<JournalCaseNotesStudentReportTO>() {
            public int compare(JournalCaseNotesStudentReportTO p1, JournalCaseNotesStudentReportTO p2) {

                int value = p1.getLastName().compareToIgnoreCase(
                        p2.getLastName());
                // 1
                if (value != 0)
                    return value;

                value = p1.getFirstName().compareToIgnoreCase(
                        p2.getFirstName());
                // 1
                if (value != 0)
                    return value;
                // 1
                if (p1.getMiddleName() == null && p2.getMiddleName() == null)
                    return 0;
                // 1
                if (p1.getMiddleName() == null)
                    return -1;
                // 1
                if (p2.getMiddleName() == null)
                    return 1;
                return p1.getMiddleName().compareToIgnoreCase(
                        p2.getMiddleName());
            }
        });
    }
}
