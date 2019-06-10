package com.bonitasoft.rest.api.serializer

import java.time.format.DateTimeFormatter

import com.bonita.lr.model.ExpenseHeader
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider

class ExpenseHeaderSerializer extends JsonSerializer<ExpenseHeader> {

    @Override
    public void serialize(ExpenseHeader header, JsonGenerator jgen, SerializerProvider serializers) throws IOException {
        jgen.writeStartObject()

        jgen.writeStringField("departement", header.departement)
        jgen.writeStringField("budget", header.budget)
        jgen.writeStringField("description", header.description)
        jgen.writeStringField("submissionDate", header.submissionDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
        jgen.writeStringField("accountingDate", header.accountingDate.format(DateTimeFormatter.ISO_LOCAL_DATE))

        jgen.writeEndObject()
    }
}
