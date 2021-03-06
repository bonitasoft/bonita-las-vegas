package com.bonitasoft.rest.api.helper;

import org.bonitasoft.web.extension.rest.RestApiResponse
import org.bonitasoft.web.extension.rest.RestApiResponseBuilder

/**
 * Agglomerate several dedicated traits with common methods
 */
trait Helper implements StateHelper, ProcessHelper, SecurityHelper {

    def RestApiResponse buildResponse(RestApiResponseBuilder responseBuilder, int httpStatus, Serializable body) {
        return responseBuilder
                .withResponseStatus(httpStatus)
                .withResponse(body)
                .build()
    }

    def RestApiResponse buildResponse(RestApiResponseBuilder responseBuilder, int httpStatus, Serializable body, int p, int c, long totalSize) {
        return responseBuilder
                .withContentRange(p, c, totalSize)
                .withResponseStatus(httpStatus)
                .withResponse(body)
                .build()
    }
}
