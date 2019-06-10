package com.bonitasoft.rest.api.serializer

import com.bonita.lr.model.ExpenseReport
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider

class ExpenseReportSerializer extends JsonSerializer<ExpenseReport> {

    @Override
    public void serialize(ExpenseReport report, JsonGenerator jgen, SerializerProvider serializers) throws IOException {
        jgen.writeStartObject()
        jgen.writeObjectField("expenseHeader", report.expenseHeader)

        jgen.writeArrayFieldStart("expenseLine")
        report.expenseLine.each { jgen.writeObject(it) }
        jgen.writeEndArray()

        jgen.writeBooleanField("reportAccepted", report.reportAccepted)
        jgen.writeEndObject()
    }
}
