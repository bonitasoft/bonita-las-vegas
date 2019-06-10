package com.bonitasoft.rest.api.exception

class ProcessInstanceNotFoundException extends Exception {

    ProcessInstanceNotFoundException(GString message) {
        super(message.toString())
    }

    ProcessInstanceNotFoundException(String message) {
        super(message)
    }
}
