package ru.nataly.lowcode_qa_platform.domain.model

enum class ExecutionStatus(val terminal: Boolean = false) {
    CREATED,
    RUNNING,
    WAITING_SLA,
    SUCCESS(terminal = true),
    FAILED(terminal = true);

    fun canMoveTo(next: ExecutionStatus): Boolean =
        next in TRANSITIONS[this].orEmpty()

    companion object {
        private val TRANSITIONS: Map<ExecutionStatus, Set<ExecutionStatus>> = mapOf(
            CREATED to setOf(RUNNING),
            RUNNING to setOf(WAITING_SLA, SUCCESS, FAILED),
            WAITING_SLA to setOf(RUNNING)
        )
    }
}
