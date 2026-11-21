package ru.nataly.lowcode_qa_platform.repository

import ru.nataly.lowcode_qa_platform.domain.model.ExecutionStatus
import ru.nataly.lowcode_qa_platform.domain.model.TestExecution
import java.util.*

interface TestExecutionRepository {
    fun findProcessKey(suiteId: UUID): String?
    fun insert(id: UUID, suiteId: UUID, username: String?, source: String)
    fun find(id: UUID): TestExecution?
    fun findStatusForUpdate(id: UUID): ExecutionStatus?
    fun updateStatus(id: UUID, status: ExecutionStatus)
    fun findAll(status: ExecutionStatus?, suiteId: UUID?, limit: Int): List<TestExecution>
}