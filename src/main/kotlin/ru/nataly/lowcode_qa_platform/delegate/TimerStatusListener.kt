package ru.nataly.lowcode_qa_platform.delegate

import org.camunda.bpm.engine.delegate.DelegateExecution
import org.camunda.bpm.engine.delegate.ExecutionListener
import org.springframework.stereotype.Component
import ru.nataly.lowcode_qa_platform.domain.model.ExecutionStatus
import ru.nataly.lowcode_qa_platform.service.TestExecutionService
import java.util.*

private fun DelegateExecution.executionId(): UUID =
    UUID.fromString(processBusinessKey ?: error("Не задан businessKey (id сессии)"))

@Component("TimerStatusListener")
class TimerStatusListener(private val service: TestExecutionService) : ExecutionListener {
    override fun notify(execution: DelegateExecution) {
        val next = when (execution.eventName) {
            ExecutionListener.EVENTNAME_START -> ExecutionStatus.WAITING_SLA
            ExecutionListener.EVENTNAME_END -> ExecutionStatus.RUNNING
            else -> return
        }
        service.changeStatus(execution.executionId(), next)
    }
}
