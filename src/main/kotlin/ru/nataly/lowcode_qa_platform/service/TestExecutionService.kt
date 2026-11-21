package ru.nataly.lowcode_qa_platform.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import ru.nataly.lowcode_qa_platform.domain.exception.ExecutionNotFoundException
import ru.nataly.lowcode_qa_platform.domain.exception.InvalidStatusTransitionException
import ru.nataly.lowcode_qa_platform.domain.model.ExecutionStatus
import ru.nataly.lowcode_qa_platform.domain.model.TestExecution
import ru.nataly.lowcode_qa_platform.repository.TestExecutionRepository
import java.util.*

@Service
class TestExecutionService(private val repo: TestExecutionRepository) {

    fun findProcessKey(suiteId: UUID): String? = repo.findProcessKey(suiteId)

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun createExecution(suiteId: UUID, username: String?, source: String): UUID {
        val id = UUID.randomUUID()
        repo.insert(id, suiteId, username, source)
        return id
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun changeStatus(id: UUID, next: ExecutionStatus) = transition(id, next)

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun finish(id: UUID, finalStatus: ExecutionStatus) {
        require(finalStatus.terminal) { "$finalStatus не является конечным статусом" }
        transition(id, finalStatus)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun failIfRunning(id: UUID) {
        if (repo.findStatusForUpdate(id) == ExecutionStatus.RUNNING) {
            repo.updateStatus(id, ExecutionStatus.FAILED)
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun succeedIfRunning(id: UUID) {
        if (repo.findStatusForUpdate(id) == ExecutionStatus.RUNNING) {
            repo.updateStatus(id, ExecutionStatus.SUCCESS)
        }
    }

    fun get(id: UUID): TestExecution = repo.find(id) ?: throw ExecutionNotFoundException(id)

    fun list(
        status: ExecutionStatus?, suiteId: UUID?, limit: Int = 100
    ): List<TestExecution> = repo.findAll(status, suiteId, limit.coerceIn(1, 500))

    private fun transition(id: UUID, next: ExecutionStatus) {
        val current = repo.findStatusForUpdate(id) ?: throw ExecutionNotFoundException(id)
        if (!current.canMoveTo(next)) throw InvalidStatusTransitionException(current, next)
        repo.updateStatus(id, next)
    }
}
