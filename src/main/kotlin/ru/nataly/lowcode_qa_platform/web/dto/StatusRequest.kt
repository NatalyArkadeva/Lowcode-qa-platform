package ru.nataly.lowcode_qa_platform.web.dto

import ru.nataly.lowcode_qa_platform.domain.model.ExecutionStatus

data class StatusRequest(val status: ExecutionStatus? = null)
