package com.bonitasoft.rest.api.document

import org.bonitasoft.engine.bpm.document.ArchivedDocumentsSearchDescriptor
import org.bonitasoft.engine.search.Order
import org.bonitasoft.engine.search.SearchOptionsBuilder

import com.bonitasoft.engine.api.ProcessAPI
import com.bonitasoft.web.extension.rest.RestAPIContext

class ArchivedCaseDocument extends CaseDocument {

    @Override
    def retrieveDocuments(RestAPIContext context, ProcessAPI processAPI, long caseId) {
        def archivedInstance = retrieveArchivedInstance(context, processAPI, caseId as long)
        return processAPI.searchArchivedDocuments(new SearchOptionsBuilder(0, Integer.MAX_VALUE).with {
            filter(ArchivedDocumentsSearchDescriptor.PROCESSINSTANCE_ID, archivedInstance.sourceObjectId)
            sort(ArchivedDocumentsSearchDescriptor.DOCUMENT_CREATIONDATE, Order.DESC)
            done()
        }).getResult()
    }
}
