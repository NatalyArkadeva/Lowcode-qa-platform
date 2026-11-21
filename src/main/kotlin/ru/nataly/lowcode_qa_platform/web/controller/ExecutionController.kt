package ru.nataly.lowcode_qa_platform.web.controller

import org.camunda.bpm.engine.RepositoryService
import org.camunda.bpm.engine.RuntimeService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import ru.nataly.lowcode_qa_platform.domain.exception.ProcessNotDeployedException
import ru.nataly.lowcode_qa_platform.domain.model.ExecutionStatus
import ru.nataly.lowcode_qa_platform.service.TestExecutionService
import ru.nataly.lowcode_qa_platform.web.dto.ExecutionView
import ru.nataly.lowcode_qa_platform.web.dto.StartRequest
import ru.nataly.lowcode_qa_platform.web.dto.StatusRequest
import ru.nataly.lowcode_qa_platform.web.dto.toView
import java.util.*

@RestController
@RequestMapping("/api/executions")
class ExecutionController(
    private val runtimeService: RuntimeService,
    private val repositoryService: RepositoryService,
    private val service: TestExecutionService
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val allowedSources = setOf("Manual", "GitLab_CI", "Scheduler")

    @PostMapping("/{suiteId}/start")
    fun start(
        @PathVariable suiteId: UUID,
        @RequestBody(required = false) body: StartRequest?,
        auth: Authentication
    ): ResponseEntity<ExecutionView> {
        val request = body ?: StartRequest()
        if (request.triggerSource !in allowedSources) {
            throw IllegalArgumentException("Неизвестный источник: ${request.triggerSource}")
        }
        if (request.variables.keys.any { it.isBlank() }) {
            throw IllegalArgumentException("Имена переменных не должны быть пустыми")
        }
        val processKey = service.findProcessKey(suiteId)
            ?: throw NoSuchElementException("Сценарий $suiteId не найден")
        val deployed = repositoryService.createProcessDefinitionQuery()
            .processDefinitionKey(processKey).count()
        if (deployed == 0L) throw ProcessNotDeployedException(processKey)

        val id = service.createExecution(suiteId, auth.name, request.triggerSource)
        service.changeStatus(id, ExecutionStatus.RUNNING)
        try {
            runtimeService.startProcessInstanceByKey(
                processKey, id.toString(), request.variables
            )
        } catch (ex: Exception) {
            log.warn("Сессия {} завершилась ошибкой: {}", id, ex.message)
            service.failIfRunning(id)
        }
        val view = service.get(id).toView()
        val code = when (view.status) {
            ExecutionStatus.SUCCESS -> HttpStatus.OK
            ExecutionStatus.FAILED -> HttpStatus.UNPROCESSABLE_ENTITY
            else -> HttpStatus.ACCEPTED
        }
        return ResponseEntity.status(code).body(view)
    }

    @GetMapping
    fun list(
        @RequestParam(required = false) status: ExecutionStatus?,
        @RequestParam(required = false) suiteId: UUID?,
        @RequestParam(defaultValue = "100") limit: Int
    ): List<ExecutionView> = service.list(status, suiteId, limit).map { it.toView() }

    @GetMapping("/{id}")
    fun get(@PathVariable id: UUID): ExecutionView = service.get(id).toView()

    @PatchMapping("/{id}/status")
    fun changeStatus(@PathVariable id: UUID, @RequestBody body: StatusRequest): ExecutionView {
        val next = body.status ?: throw IllegalArgumentException("Не указан новый статус")
        service.changeStatus(id, next)
        return service.get(id).toView()
    }
}

