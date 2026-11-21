package ru.nataly.lowcode_qa_platform.domain.model

data class ExecutionSummary(val steps: Int,
                            val slaBreaches: Int,
                            val errors: Int,
                            val avgDurationMs: Int
)
