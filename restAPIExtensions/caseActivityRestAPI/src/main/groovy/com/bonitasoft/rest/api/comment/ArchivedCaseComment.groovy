
package com.bonitasoft.rest.api.comment

import org.bonitasoft.engine.bpm.comment.ArchivedCommentsSearchDescriptor
import org.bonitasoft.engine.search.Order
import org.bonitasoft.engine.search.SearchOptionsBuilder

import com.bonitasoft.engine.api.ProcessAPI
import com.bonitasoft.web.extension.rest.RestAPIContext

class ArchivedCaseComment extends CaseComment {

    @Override
    def retrieveComments(RestAPIContext context, ProcessAPI processAPI, long caseId) {
        def archivedInstance = retrieveArchivedInstance(context, processAPI, caseId as long)
        return processAPI.searchArchivedComments(new SearchOptionsBuilder(0, Integer.MAX_VALUE).with {
            filter(ArchivedCommentsSearchDescriptor.PROCESS_INSTANCE_ID, archivedInstance.sourceObjectId)
            sort(ArchivedCommentsSearchDescriptor.POSTDATE, Order.DESC)
            done()
        }).getResult().findAll {
            it.userId != null && it.userId != -1
        }
    }
}
