package ru.nataly.lowcode_qa_platform.delegate

import org.camunda.bpm.engine.delegate.DelegateExecution
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestTemplate
import ru.nataly.lowcode_qa_platform.repository.impl.TransactionLogRepositoryImpl
import ru.nataly.lowcode_qa_platform.service.JsonSupport
import ru.nataly.lowcode_qa_platform.service.TestExecutionService
import java.net.URI

/**
 * Универсальный коннектор: выполняет HTTP-запрос, описанный переменными
 * шага (requestUrl, httpMethod, requestHeaders, requestBody, expectedStatus,
 * extractRules), и извлекает переменные для следующих шагов сценария.
 */
@Component("UniversalRestConnectorDelegate")
class UniversalRestConnectorDelegate(
    private val restTemplate: RestTemplate,
    logs: TransactionLogRepositoryImpl,
    executions: TestExecutionService,
    json: JsonSupport
) : AbstractStepDelegate(logs, executions, json) {

    override fun executeStep(execution: DelegateExecution): StepOutcome {
        val url = json.resolve(requiredParam(execution, "requestUrl"), execution)
        val method = HttpMethod.valueOf(requiredParam(execution, "httpMethod").uppercase())
        val body = (execution.getVariable("requestBody") as? String)
            ?.let { json.resolve(it, execution) }
            ?.takeIf { it.isNotBlank() }

        val headers = HttpHeaders()
        (execution.getVariable("requestHeaders") as? Map<*, *>)?.forEach { (k, v) ->
            headers.add(k.toString(), json.resolve(v.toString(), execution))
        }
        if (body != null && headers.contentType == null) {
            headers.contentType = MediaType.APPLICATION_JSON
        }

        var status: Int
        var responseHeaders = "{}"
        var responseBody: String
        try {
            val response = restTemplate.exchange(
                URI.create(url), method, HttpEntity(body, headers), String::class.java
            )
            status = response.statusCode.value()
            responseHeaders = json.toJson(response.headers.toSingleValueMap())
            responseBody = response.body.orEmpty()
        } catch (ex: HttpStatusCodeException) {          // ответ 4xx / 5xx получен
            status = ex.statusCode.value()
            responseHeaders = ex.responseHeaders?.toSingleValueMap()?.let { json.toJson(it) } ?: "{}"
            responseBody = ex.responseBodyAsString
        } catch (ex: ResourceAccessException) {          // сеть недоступна / таймаут
            status = 0
            responseBody = json.toJson(mapOf("error" to ex.message))
        }

        val expected = execution.getVariable("expectedStatus")?.toString()?.toIntOrNull()
        val passed = if (expected != null) status == expected else status in 200..299

        return StepOutcome(
            requestUrl = url,
            httpMethod = method.name(),
            requestHeaders = json.toJson(headers.toSingleValueMap().minus("Authorization")),
            requestBody = json.toJsonText(body),
            responseStatus = status,
            responseHeaders = responseHeaders,
            responseBody = json.toJsonText(responseBody),
            passed = passed,
            failureMessage = "Шаг '${execution.currentActivityName}': получен HTTP $status, " +
                    "ожидался ${expected ?: "2xx"}"
        )
    }

    override fun onSuccess(execution: DelegateExecution, outcome: StepOutcome, durationMs: Int) {
        execution.setVariable("lastResponseStatus", outcome.responseStatus)
        execution.setVariable("lastDurationMs", durationMs)

        (execution.getVariable("extractRules") as? String)
            ?.takeIf { it.isNotBlank() }
            ?.let { rules ->
                json.extract(outcome.responseBody, rules).forEach { (name, value) ->
                    execution.setVariable(name, value)   // доступно следующим шагам как {{name}}
                }
            }
    }
}
