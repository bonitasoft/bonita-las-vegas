package com.bonitasoft.rest.api.serializer

import java.time.format.DateTimeFormatter

import com.bonita.lr.model.ExpenseLine
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider

class ExpenseLineSerializer extends JsonSerializer<ExpenseLine> {

    public static final String KM_TYPE = "KM"

    @Override
    public void serialize(ExpenseLine expenseLine, JsonGenerator jgen, SerializerProvider serializers) throws IOException {
        jgen.writeStartObject()

        jgen.writeStringField("expenseDate", expenseLine.expenseDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
        jgen.writeStringField("description", expenseLine.description)
        jgen.writeStringField("expenseType", expenseLine.expenseType)

        if (Objects.equals(expenseLine.expenseType, KM_TYPE)) {
            jgen.writeStringField("vehicleType", expenseLine.vehicleType)
            jgen.writeNumberField("vehicleRate", expenseLine.vehicleRate)
            jgen.writeNumberField("km", expenseLine.km)
        } else {
            jgen.writeStringField("currency", expenseLine.currency)
            jgen.writeNumberField("currencyRatio", expenseLine.currencyRatio)
        }

        jgen.writeNumberField("amount", expenseLine.amount)

        jgen.writeEndObject()
    }
}
