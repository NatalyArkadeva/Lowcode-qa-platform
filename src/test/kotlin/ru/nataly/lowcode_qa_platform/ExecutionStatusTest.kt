package ru.nataly.lowcode_qa_platform

import org.testng.Assert.assertFalse
import org.testng.Assert.assertTrue
import org.testng.annotations.Test
import ru.nataly.lowcode_qa_platform.domain.model.ExecutionStatus

class ExecutionStatusTest {
    @Test
    fun `допустимые переходы разрешены`() {
        assertTrue(ExecutionStatus.CREATED.canMoveTo(ExecutionStatus.RUNNING))
        assertTrue(ExecutionStatus.RUNNING.canMoveTo(ExecutionStatus.WAITING_SLA))
        assertTrue(ExecutionStatus.WAITING_SLA.canMoveTo(ExecutionStatus.RUNNING))
        assertTrue(ExecutionStatus.RUNNING.canMoveTo(ExecutionStatus.SUCCESS))
        assertTrue(ExecutionStatus.RUNNING.canMoveTo(ExecutionStatus.FAILED))
    }

    @Test
    fun `недопустимые переходы запрещены`() {
        assertFalse(ExecutionStatus.CREATED.canMoveTo(ExecutionStatus.SUCCESS))
        assertFalse(ExecutionStatus.CREATED.canMoveTo(ExecutionStatus.FAILED))
        assertFalse(ExecutionStatus.WAITING_SLA.canMoveTo(ExecutionStatus.SUCCESS))
        assertFalse(ExecutionStatus.SUCCESS.canMoveTo(ExecutionStatus.RUNNING))
    }

    @Test
    fun `из конечных статусов переходов нет`() {
        ExecutionStatus.values().filter { it.terminal }.forEach { from ->
            ExecutionStatus.values().forEach { to -> assertFalse(from.canMoveTo(to)) }
        }
    }
}
