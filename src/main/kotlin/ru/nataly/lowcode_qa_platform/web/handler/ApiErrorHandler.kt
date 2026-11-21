package ru.nataly.lowcode_qa_platform.web.handler

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import ru.nataly.lowcode_qa_platform.domain.exception.ExecutionNotFoundException
import ru.nataly.lowcode_qa_platform.domain.exception.InvalidStatusTransitionException
import ru.nataly.lowcode_qa_platform.domain.exception.ProcessNotDeployedException

@RestControllerAdvice
class ApiErrorHandler {
    @ExceptionHandler(ExecutionNotFoundException::class, NoSuchElementException::class)
    fun notFound(ex: RuntimeException) = error(HttpStatus.NOT_FOUND, ex.message)

    @ExceptionHandler(
        InvalidStatusTransitionException::class, ProcessNotDeployedException::class
    )
    fun conflict(ex: RuntimeException) = error(HttpStatus.CONFLICT, ex.message)

    @ExceptionHandler(IllegalArgumentException::class)
    fun badRequest(ex: RuntimeException) = error(HttpStatus.BAD_REQUEST, ex.message)

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun unreadable(ex: HttpMessageNotReadableException) =
        error(HttpStatus.BAD_REQUEST, "Некорректное тело запроса или значение поля")

    private fun error(status: HttpStatus, message: String?) =
        ResponseEntity.status(status).body(mapOf("error" to message))
}
