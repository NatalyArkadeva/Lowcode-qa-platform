package ru.nataly.lowcode_qa_platform.domain.exception

import ru.nataly.lowcode_qa_platform.domain.model.ExecutionStatus

class InvalidStatusTransitionException(from: ExecutionStatus, to: ExecutionStatus) :
    RuntimeException("Недопустимый переход статуса: $from -> $to")
