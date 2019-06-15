package com.bonitasoft.rest.api.serializer

import java.time.format.DateTimeFormatter

import com.bonita.lr.model.ExpenseReport
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider

class ExpenseReportSerializer extends JsonSerializer<ExpenseReport> {

    @Override
    public void serialize(ExpenseReport report, JsonGenerator jgen, SerializerProvider serializers) throws IOException {
        jgen.writeStartObject()

        jgen.writeNumberField("employeeId", report.employeeId)
        jgen.writeObjectField("expenseHeader", report.expenseHeader)

        jgen.writeArrayFieldStart("expenseLine")
        report.expenseLine.each { jgen.writeObject(it) }
        jgen.writeEndArray()

        writeStringField(jgen, "closeDate", report.closeDate?.format(DateTimeFormatter.ISO_LOCAL_DATE))
        jgen.writeBooleanField("reportAccepted", report.reportAccepted)
        jgen.writeEndObject()
    }

    def writeStringField(JsonGenerator jgen, String fieldName, String fieldValue) {
        if (fieldValue) {
            jgen.writeStringField(fieldName, fieldValue)
        }
    }
}
