package ru.nataly.lowcode_qa_platform.web.dto

data class StartRequest(
    val variables: Map<String, Any?> = emptyMap(),
    val triggerSource: String = "Manual"
)
