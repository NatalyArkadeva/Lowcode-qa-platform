package ru.nataly.lowcode_qa_platform.delegate

import org.camunda.bpm.engine.delegate.DelegateExecution
import org.camunda.bpm.engine.delegate.JavaDelegate
import ru.nataly.lowcode_qa_platform.domain.exception.TestStepFailedException
import ru.nataly.lowcode_qa_platform.domain.model.TransactionLog
import ru.nataly.lowcode_qa_platform.repository.TransactionLogRepository
import ru.nataly.lowcode_qa_platform.service.JsonSupport
import ru.nataly.lowcode_qa_platform.service.TestExecutionService
import java.util.*

abstract class AbstractStepDelegate(
    protected val logs: TransactionLogRepository,
    protected val executions: TestExecutionService,
    protected val json: JsonSupport
) : JavaDelegate {

    protected data class StepOutcome(
        val requestUrl: String,
        val httpMethod: String,
        val requestHeaders: String = "{}",
        val requestBody: String = "{}",
        val responseStatus: Int,
        val responseHeaders: String = "{}",
        val responseBody: String = "{}",
        val passed: Boolean,
        val failureMessage: String? = null
    )

    override fun execute(execution: DelegateExecution) {
        val executionId = UUID.fromString(
            execution.processBusinessKey ?: error("Не задан businessKey (id сессии)")
        )
        val startedAt = System.nanoTime()
        val outcome = executeStep(execution)
        val durationMs = ((System.nanoTime() - startedAt) / 1_000_000).toInt()

        logs.saveTransactionLog(
            TransactionLog(
                executionId = executionId,
                activityInstanceId = execution.activityInstanceId ?: "n/a",
                activityName = execution.currentActivityName ?: execution.currentActivityId,
                requestUrl = outcome.requestUrl,
                httpMethod = outcome.httpMethod,
                requestHeaders = outcome.requestHeaders,
                requestBody = outcome.requestBody,
                responseStatus = outcome.responseStatus,
                responseHeaders = outcome.responseHeaders,
                responseBody = outcome.responseBody,
                durationMs = durationMs
            )
        )

        if (!outcome.passed) {
            executions.failIfRunning(executionId)
            throw TestStepFailedException(
                outcome.failureMessage ?: "Шаг '${execution.currentActivityName}' не пройден"
            )
        }

        onSuccess(execution, outcome, durationMs)
    }

    protected abstract fun executeStep(execution: DelegateExecution): StepOutcome

    protected open fun onSuccess(execution: DelegateExecution, outcome: StepOutcome, durationMs: Int) {}

    protected fun requiredParam(execution: DelegateExecution, name: String): String =
        execution.getVariable(name)?.toString()?.takeIf { it.isNotBlank() }
            ?: throw TestStepFailedException("Не задан обязательный параметр шага: $name")
}