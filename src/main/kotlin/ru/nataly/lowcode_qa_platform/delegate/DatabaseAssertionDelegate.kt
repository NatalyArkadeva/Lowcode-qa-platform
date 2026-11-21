package ru.nataly.lowcode_qa_platform.delegate

import org.camunda.bpm.engine.delegate.DelegateExecution
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.stereotype.Component
import ru.nataly.lowcode_qa_platform.repository.TransactionLogRepository
import ru.nataly.lowcode_qa_platform.service.JsonSupport
import ru.nataly.lowcode_qa_platform.service.TestExecutionService

@Component("DatabaseAssertionDelegate")
class DatabaseAssertionDelegate(
    logs: TransactionLogRepository,
    executions: TestExecutionService,
    json: JsonSupport
) : AbstractStepDelegate(logs, executions, json) {

    override fun executeStep(execution: DelegateExecution): StepOutcome {
        val dbUrl = requiredParam(execution, "targetDbUrl")
        val query = json.resolve(requiredParam(execution, "assertionQuery"), execution)
        val expected = json.resolve(requiredParam(execution, "expectedValue"), execution)

        require(query.trimStart().startsWith("select", ignoreCase = true)) {
            "SQL-ассерт допускает только SELECT"
        }

        val target = JdbcTemplate(
            DriverManagerDataSource(
                dbUrl,
                requiredParam(execution, "targetDbUser"),
                requiredParam(execution, "targetDbPassword")
            )
        ).apply { queryTimeout = 10 }

        var actual: String? = null
        var error: String? = null
        try {
            actual = target.queryForList(query).firstOrNull()?.values?.firstOrNull()?.toString()
        } catch (ex: Exception) {
            error = ex.message
        }
        val passed = error == null && actual == expected

        return StepOutcome(
            requestUrl = dbUrl,
            httpMethod = "SQL",
            requestBody = json.toJson(mapOf("query" to query, "expected" to expected)),
            responseStatus = if (passed) 200 else 500,
            responseBody = json.toJson(mapOf("actual" to actual, "error" to error, "passed" to passed)),
            passed = passed,
            failureMessage = "SQL-ассерт '${execution.currentActivityName}' не пройден: " +
                    (error ?: "ожидалось '$expected', получено '$actual'")
        )
    }
}

