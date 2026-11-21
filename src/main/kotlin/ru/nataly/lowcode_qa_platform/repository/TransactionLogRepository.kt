package ru.nataly.lowcode_qa_platform.repository

import ru.nataly.lowcode_qa_platform.domain.model.ExecutionSummary
import ru.nataly.lowcode_qa_platform.domain.model.TransactionLog
import java.util.*

interface TransactionLogRepository {
    fun saveTransactionLog(log: TransactionLog)
    fun getLogsByExecutionId(executionId: UUID, slaBreachOnly: Boolean): List<TransactionLog>
    fun summary(executionId: UUID): ExecutionSummary
}