package com.bonitasoft.rest.api.serializer

import com.bonita.lr.model.ExpenseHeader
import com.bonita.lr.model.ExpenseLine
import com.bonita.lr.model.ExpenseReport
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule

class ExpenseMapper extends ObjectMapper {

    public ExpenseMapper() {
        SimpleModule module = new SimpleModule()

        module.addSerializer(ExpenseReport.class, new ExpenseReportSerializer())
        module.addSerializer(ExpenseHeader.class, new ExpenseHeaderSerializer())
        module.addSerializer(ExpenseLine.class, new ExpenseLineSerializer())

        registerModule(module)
    }
}
