package ru.nataly.lowcode_qa_platform.web.dto

import ru.nataly.lowcode_qa_platform.domain.model.ExecutionStatus
import ru.nataly.lowcode_qa_platform.domain.model.TestExecution
import java.time.OffsetDateTime
import java.util.*

data class ExecutionView(
    val id: UUID,
    val suiteId: UUID,
    val triggerSource: String,
    val status: ExecutionStatus,
    val startedAt: OffsetDateTime,
    val finishedAt: OffsetDateTime?,
)

fun TestExecution.toView() =
    ExecutionView(id, suiteId, triggerSource, status, startedAt, finishedAt)
