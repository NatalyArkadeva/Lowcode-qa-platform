package ru.nataly.lowcode_qa_platform

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import ru.nataly.lowcode_qa_platform.domain.model.ExecutionStatus
import ru.nataly.lowcode_qa_platform.domain.exception.ExecutionNotFoundException
import ru.nataly.lowcode_qa_platform.domain.exception.InvalidStatusTransitionException
import ru.nataly.lowcode_qa_platform.repository.impl.TestExecutionRepositoryImpl
import ru.nataly.lowcode_qa_platform.service.TestExecutionService
import java.util.UUID

class TestExecutionServiceTest {
    private val repo = mock(TestExecutionRepositoryImpl::class.java)
    private val service = TestExecutionService(repo)
    private val id = UUID.fromString("22222222-2222-2222-2222-222222222222")

    @Test
    fun `допустимая смена статуса сохраняется`() {
        given(repo.findStatusForUpdate(id)).willReturn(ExecutionStatus.RUNNING)
        service.changeStatus(id, ExecutionStatus.WAITING_SLA)
        verify(repo).updateStatus(id, ExecutionStatus.WAITING_SLA)
    }

    @Test
    fun `недопустимая смена статуса отклоняется без записи`() {
        given(repo.findStatusForUpdate(id)).willReturn(ExecutionStatus.SUCCESS)
        assertThrows(InvalidStatusTransitionException::class.java) {
            service.changeStatus(id, ExecutionStatus.RUNNING)
        }
        verify(repo, never()).updateStatus(id, ExecutionStatus.RUNNING)
    }

    @Test
    fun `смена статуса несуществующей сессии даёт ошибку не найдено`() {
        given(repo.findStatusForUpdate(id)).willReturn(null)
        assertThrows(ExecutionNotFoundException::class.java) {
            service.changeStatus(id, ExecutionStatus.RUNNING)
        }
    }

    @Test
    fun `finish принимает только конечные статусы`() {
        assertThrows(IllegalArgumentException::class.java) {
            service.finish(id, ExecutionStatus.RUNNING)
        }
    }
}
