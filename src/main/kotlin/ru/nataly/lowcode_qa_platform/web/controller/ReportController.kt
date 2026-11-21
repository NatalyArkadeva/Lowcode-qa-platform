package ru.nataly.lowcode_qa_platform.web.controller

import org.springframework.web.bind.annotation.*
import ru.nataly.lowcode_qa_platform.domain.model.ExecutionSummary
import ru.nataly.lowcode_qa_platform.domain.model.TransactionLog
import ru.nataly.lowcode_qa_platform.repository.TransactionLogRepository
import ru.nataly.lowcode_qa_platform.service.TestExecutionService
import java.util.*

@RestController
@RequestMapping("/api/reports/executions")
class ReportController(
    private val service: TestExecutionService,
    private val logs: TransactionLogRepository
) {
    @GetMapping("/{id}/logs")
    fun logs(
        @PathVariable id: UUID,
        @RequestParam(defaultValue = "false") slaBreachOnly: Boolean
    ): List<TransactionLog> {
        service.get(id)
        return logs.getLogsByExecutionId(id, slaBreachOnly)
    }

    @GetMapping("/{id}/summary")
    fun summary(@PathVariable id: UUID): ExecutionSummary {
        service.get(id)
        return logs.summary(id)
    }
}
