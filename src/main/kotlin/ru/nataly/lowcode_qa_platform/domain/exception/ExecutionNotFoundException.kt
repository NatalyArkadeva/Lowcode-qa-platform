package ru.nataly.lowcode_qa_platform.domain.exception

import java.util.*

class ExecutionNotFoundException(id: UUID) :
    RuntimeException("Сессия запуска $id не найдена")
