package ru.nataly.lowcode_qa_platform.domain.model

import java.util.*

data class TransactionLog(val id: Long? = null,
                          val executionId: UUID,
                          val activityInstanceId: String = "",
                          val activityName: String,
                          val requestUrl: String,
                          val httpMethod: String,
                          val requestHeaders: String = "{}",
                          val requestBody: String = "{}",
                          val responseStatus: Int,
                          val responseHeaders: String = "{}",
                          val responseBody: String = "{}",
                          val durationMs: Int,
                          val isSlaBreach: Boolean = false

) {
    fun isErrorStatus(): Boolean = responseStatus >= 400 || responseStatus == 0
}

