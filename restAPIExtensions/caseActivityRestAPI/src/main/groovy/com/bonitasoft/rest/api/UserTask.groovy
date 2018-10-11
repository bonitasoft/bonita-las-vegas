package com.bonitasoft.rest.api;

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import org.bonitasoft.web.extension.rest.RestApiResponse
import org.bonitasoft.web.extension.rest.RestApiResponseBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.bonitasoft.web.extension.rest.RestAPIContext
import com.bonitasoft.web.extension.rest.RestApiController

import groovy.json.JsonSlurper
import groovy.json.internal.LazyMap

class UserTask implements RestApiController {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserTask.class)

    @Override
    RestApiResponse doHandle(HttpServletRequest request, RestApiResponseBuilder responseBuilder, RestAPIContext context) {
        def jsonBody = new JsonSlurper().parse(request.getReader())
        def processAPI = context.apiClient.getProcessAPI()
        if(!jsonBody.taskId) {
            return responseBuilder.with {
                withResponseStatus(HttpServletResponse.SC_BAD_REQUEST)
                withResponse("No taskId in payload")
                build()
            }
        }
        def taskId = jsonBody.taskId.toLong()
        processAPI.assignUserTask(taskId, context.apiSession.userId)
        Map<String,Serializable> contractMap = toHashMap(jsonBody.content)
        LOGGER.info(contractMap.toString())
        processAPI.executeUserTask(taskId, contractMap)
        //processAPI.addProcessComment(jsonBody.processInstanceId.toLong(), jsonBody.content) TODO FIXME
        return responseBuilder.with {
            withResponseStatus(HttpServletResponse.SC_CREATED)
            build()
        }
    }

    Map<String, Serializable> toHashMap(LazyMap map) {
        Map<String,Serializable> newMap = new HashMap<>();
        newMap.putAll(map);
        if (newMap.keySet() != null) {
            newMap.keySet().forEach {
                def value = newMap.get(it)
                if (value instanceof LazyMap) {
                    newMap.put(it, toHashMap(value))
                }
            }
        }

        return newMap;
    }
}
