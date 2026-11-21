package ru.nataly.lowcode_qa_platform

import org.camunda.bpm.engine.RepositoryService
import org.camunda.bpm.engine.RuntimeService
import org.camunda.bpm.engine.repository.ProcessDefinitionQuery
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.willThrow
import org.mockito.Mockito.*
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import ru.nataly.lowcode_qa_platform.domain.exception.InvalidStatusTransitionException
import ru.nataly.lowcode_qa_platform.domain.model.ExecutionStatus
import ru.nataly.lowcode_qa_platform.domain.model.TestExecution
import ru.nataly.lowcode_qa_platform.service.TestExecutionService
import ru.nataly.lowcode_qa_platform.web.controller.ExecutionController
import ru.nataly.lowcode_qa_platform.web.handler.ApiErrorHandler
import java.time.LocalDateTime
import java.util.*

class ExecutionControllerTest {
    private val service = mock(TestExecutionService::class.java)
    private val runtime = mock(RuntimeService::class.java)
    private val repository = mock(RepositoryService::class.java)
    private val query = mock(ProcessDefinitionQuery::class.java)
    private lateinit var mvc: MockMvc

    private val suite = UUID.fromString("11111111-1111-1111-1111-111111111111")
    private val id = UUID.fromString("22222222-2222-2222-2222-222222222222")
    private val qa = UsernamePasswordAuthenticationToken("qa", "n/a")
    private val json = MediaType.APPLICATION_JSON

    @BeforeEach
    fun setUp() {
        mvc = MockMvcBuilders
            .standaloneSetup(ExecutionController(runtime, repository, service))
            .setControllerAdvice(ApiErrorHandler())
            .build()
        given(repository.createProcessDefinitionQuery()).willReturn(query)
        given(query.processDefinitionKey("order-chain-e2e-test")).willReturn(query)
        given(query.count()).willReturn(1L)
        given(service.findProcessKey(suite)).willReturn("order-chain-e2e-test")
    }

    private fun execution(s: ExecutionStatus) =
        TestExecution(id, suite, "Manual", s, LocalDateTime.now(), null)

    @Test
    fun `запуск с корректными данными возвращает 200 и статус SUCCESS`() {
        given(service.createExecution(suite, "qa", "Manual")).willReturn(id)
        given(service.get(id)).willReturn(execution(ExecutionStatus.SUCCESS))
        mvc.perform(post("/api/executions/$suite/start").principal(qa)
            .contentType(json).content("""{"triggerSource":"Manual"}"""))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("SUCCESS"))
        verify(runtime)
            .startProcessInstanceByKey("order-chain-e2e-test", id.toString(), emptyMap())
    }

    @Test
    fun `запуск с неизвестным источником даёт 400 и не создаёт сессию`() {
        mvc.perform(post("/api/executions/$suite/start").principal(qa)
            .contentType(json).content("""{"triggerSource":"Telegram"}"""))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").exists())
        verifyNoInteractions(service)
    }

    @Test
    fun `запуск несуществующего сценария даёт 404`() {
        val unknown = UUID.randomUUID()
        given(service.findProcessKey(unknown)).willReturn(null)
        mvc.perform(post("/api/executions/$unknown/start").principal(qa))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `запуск сценария с неразвёрнутой схемой даёт 409`() {
        given(query.count()).willReturn(0L)
        mvc.perform(post("/api/executions/$suite/start").principal(qa))
            .andExpect(status().isConflict)
    }

    @Test
    fun `провал шага возвращает 422 и статус FAILED`() {
        given(service.createExecution(suite, "qa", "Manual")).willReturn(id)
        given(service.get(id)).willReturn(execution(ExecutionStatus.FAILED))
        mvc.perform(post("/api/executions/$suite/start").principal(qa))
            .andExpect(status().isUnprocessableEntity)
            .andExpect(jsonPath("$.status").value("FAILED"))
    }

    @Test
    fun `список сессий фильтруется по статусу`() {
        given(service.list(ExecutionStatus.FAILED, null, 100))
            .willReturn(listOf(execution(ExecutionStatus.FAILED)))
        mvc.perform(get("/api/executions?status=FAILED"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].status").value("FAILED"))
    }

    @Test
    fun `допустимая смена статуса возвращает обновлённую сессию`() {
        given(service.get(id)).willReturn(execution(ExecutionStatus.WAITING_SLA))
        mvc.perform(patch("/api/executions/$id/status")
            .contentType(json).content("""{"status":"WAITING_SLA"}"""))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("WAITING_SLA"))
    }

    @Test
    fun `недопустимый переход статуса даёт 409`() {
        val ex = InvalidStatusTransitionException(
            ExecutionStatus.SUCCESS, ExecutionStatus.RUNNING
        )
        willThrow(ex)
            .given(service).changeStatus(id, ExecutionStatus.RUNNING)
        mvc.perform(patch("/api/executions/$id/status")
            .contentType(json).content("""{"status":"RUNNING"}"""))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.error").exists())
    }

    @Test
    fun `неизвестное значение статуса даёт 400`() {
        mvc.perform(patch("/api/executions/$id/status")
            .contentType(json).content("""{"status":"DONE"}"""))
            .andExpect(status().isBadRequest)
    }
}
