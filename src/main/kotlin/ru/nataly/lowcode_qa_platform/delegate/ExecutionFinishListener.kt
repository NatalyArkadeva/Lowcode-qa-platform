package ru.nataly.lowcode_qa_platform.delegate

import org.camunda.bpm.engine.delegate.DelegateExecution
import org.camunda.bpm.engine.delegate.ExecutionListener
import org.springframework.stereotype.Component
import ru.nataly.lowcode_qa_platform.service.TestExecutionService
import java.util.*

private fun DelegateExecution.executionId(): UUID =
    UUID.fromString(processBusinessKey ?: error("Не задан businessKey (id сессии)"))

@Component("ExecutionFinishListener")
class ExecutionFinishListener(private val service: TestExecutionService) : ExecutionListener {
    override fun notify(execution: DelegateExecution) =
        service.succeedIfRunning(execution.executionId())
}
