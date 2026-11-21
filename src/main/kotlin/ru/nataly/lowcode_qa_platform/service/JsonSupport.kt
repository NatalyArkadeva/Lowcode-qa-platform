package ru.nataly.lowcode_qa_platform.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.camunda.bpm.engine.delegate.DelegateExecution
import org.springframework.stereotype.Component

@Component
class JsonSupport(private val mapper: ObjectMapper) {

    /** Заменяет {{name}} значением переменной процесса (например, {{token}}). */
    fun resolve(template: String, execution: DelegateExecution): String =
        PLACEHOLDER.replace(template) { m ->
            execution.getVariable(m.groupValues[1])?.toString() ?: m.value
        }

    /** Значение для столбца JSONB: невалидный JSON (HTML, текст) оборачивается в объект. */
    fun toJsonText(raw: String?): String {
        if (raw.isNullOrBlank()) return "{}"
        return try {
            mapper.readTree(raw)
            raw
        } catch (ex: Exception) {
            mapper.writeValueAsString(mapOf("raw" to raw))
        }
    }

    fun toJson(value: Any?): String = mapper.writeValueAsString(value)

    /**
     * Правила извлечения: {"token": "/access_token", "orderId": "/data/id"}.
     * Значения — JSON Pointer; результат передаётся следующим шагам сценария.
     */
    fun extract(responseBody: String, rulesJson: String): Map<String, String> {
        val body = mapper.readTree(toJsonText(responseBody))
        val rules = mapper.readTree(rulesJson)
        val result = mutableMapOf<String, String>()
        rules.fields().forEach { (name, pointer) ->
            val node = body.at(pointer.asText())
            if (!node.isMissingNode && !node.isNull) result[name] = node.asText()
        }
        return result
    }

    private companion object {
        val PLACEHOLDER = Regex("\\{\\{(\\w+)}}")
    }
}
