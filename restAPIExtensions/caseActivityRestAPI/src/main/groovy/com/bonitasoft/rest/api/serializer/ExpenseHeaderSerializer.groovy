package com.bonitasoft.rest.api.serializer

import com.bonita.lr.model.ExpenseHeader
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider

class ExpenseHeaderSerializer extends JsonSerializer<ExpenseHeader> {

    @Override
    public void serialize(ExpenseHeader header, JsonGenerator jgen, SerializerProvider serializers) throws IOException {
        jgen.writeStartObject()

        writeStringField(jgen, "departement", header.departement)
        writeStringField(jgen, "budget", header.budget)
        writeStringField(jgen, "description", header.description)
        writeStringField(jgen, "region", header.region)

        jgen.writeEndObject()
    }

    def writeStringField(JsonGenerator jgen, String fieldName, String fieldValue) {
        if (fieldValue) {
            jgen.writeStringField(fieldName, fieldValue)
        }
    }
}
