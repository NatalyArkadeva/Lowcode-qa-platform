package ru.nataly.lowcode_qa_platform.domain.model

import java.time.OffsetDateTime
import java.util.*

data class TestExecution(
    val id: UUID,
    val suiteId: UUID,
    val triggerSource: String,
    val status: ExecutionStatus,
    val startedAt: OffsetDateTime,
    val finishedAt: OffsetDateTime?,
)

