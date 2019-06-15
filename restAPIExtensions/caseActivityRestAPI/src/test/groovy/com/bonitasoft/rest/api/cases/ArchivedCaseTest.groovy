package com.bonitasoft.rest.api.cases

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import org.bonitasoft.engine.bpm.process.ArchivedProcessInstance
import org.bonitasoft.engine.identity.User

import com.bonita.lr.model.ExpenseHeader
import com.bonita.lr.model.ExpenseReport
import com.bonitasoft.engine.api.APIClient
import com.bonitasoft.engine.api.IdentityAPI
import com.bonitasoft.rest.api.archive.UserArchivedReport
import com.bonitasoft.web.extension.rest.RestAPIContext

import spock.lang.Specification

class ArchivedCaseTest extends Specification {

    static final String CASE_DESCRIPTION = "Case description"
    static final String CONTEXT_PATH = "localhost:8080/bonita/apps"
    static final String APP_TOKEN = "expenseReportEmployee"
    static final long ID = 100

    def "should format archived instance"() {
        given:

        def RestAPIContext context = Mock()
        def APIClient apiCLient = Mock()
        def IdentityAPI identityAPI = Mock()
        def User user = Mock()
        context.apiClient >> apiCLient
        apiCLient.getIdentityAPI() >> identityAPI
        identityAPI.getUser(_) >> user
        user.firstName >> "Toto"
        user.lastName >> "L'asticot"
        def archivedCase = new UserArchivedReport()
        def reportAcceptedWithDescription = createExpenseReport(CASE_DESCRIPTION, true)
        def reportRefusedWithoutDescription = createExpenseReport(null, false)
        ArchivedProcessInstance archivedInstance = Mock()
        archivedInstance.id >> ID

        when:

        def acceptedOutput = archivedCase.format(context, CONTEXT_PATH, APP_TOKEN, archivedInstance, reportAcceptedWithDescription)
        def refusedOutput = archivedCase.format(context, CONTEXT_PATH, APP_TOKEN, archivedInstance, reportRefusedWithoutDescription)

        then:

        assert acceptedOutput["id"] == ID
        assert acceptedOutput["user"] == "Toto L'asticot"
        assert acceptedOutput["name"] ==  CASE_DESCRIPTION
        assert acceptedOutput["date"] ==  LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        assert acceptedOutput["state"] == """<span class="label label-success">Accepted</span>"""
        assert acceptedOutput["viewAction"] == """<a class="btn btn-primary btn-sm" href="$CONTEXT_PATH/apps/$APP_TOKEN/archivedCase?id=$ID" target="_top">Overview</a>"""

        assert refusedOutput["id"] == ID
        assert refusedOutput["name"] ==  "New expense report"
        assert refusedOutput["date"] ==  LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        assert refusedOutput["state"] == """<span class="label label-danger">Refused</span>"""
        assert refusedOutput["viewAction"] == """<a class="btn btn-primary btn-sm" href="$CONTEXT_PATH/apps/$APP_TOKEN/archivedCase?id=$ID" target="_top">Overview</a>"""
    }

    def ExpenseReport createExpenseReport(String description, boolean accepted) {
        def header = new ExpenseHeader()
        header.description = description

        def expenseReport = new ExpenseReport()
        expenseReport.expenseHeader = header
        expenseReport.reportAccepted = accepted
        expenseReport.closeDate = LocalDate.now()

        expenseReport
    }
}
