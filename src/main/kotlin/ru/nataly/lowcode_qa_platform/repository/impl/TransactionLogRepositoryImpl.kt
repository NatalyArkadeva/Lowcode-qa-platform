package ru.nataly.lowcode_qa_platform.repository.impl

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import ru.nataly.lowcode_qa_platform.domain.model.ExecutionSummary
import ru.nataly.lowcode_qa_platform.domain.model.TransactionLog
import ru.nataly.lowcode_qa_platform.repository.TransactionLogRepository
import java.util.*

@Repository
class TransactionLogRepositoryImpl(private val jdbc: JdbcTemplate) : TransactionLogRepository {

    private val mapper = RowMapper { rs, _ ->
        TransactionLog(
            id = rs.getLong("id"),
            executionId = rs.getObject("execution_id", UUID::class.java),
            activityInstanceId = rs.getString("activity_execution_id"),
            activityName = rs.getString("activity_name"),
            requestUrl = rs.getString("request_url"),
            httpMethod = rs.getString("http_method"),
            requestHeaders = rs.getString("request_headers") ?: "{}",
            requestBody = rs.getString("request_body") ?: "{}",
            responseStatus = rs.getInt("response_status"),
            responseHeaders = rs.getString("response_headers") ?: "{}",
            responseBody = rs.getString("response_body") ?: "{}",
            durationMs = rs.getInt("duration_ms"),
            isSlaBreach = rs.getBoolean("is_sla_breach")
        )
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    override fun saveTransactionLog(log: TransactionLog) {
        jdbc.update(
            """INSERT INTO http_transaction_logs (
                   execution_id, activity_execution_id, activity_name, request_url,
                   http_method, request_headers, request_body, response_status,
                   response_headers, response_body, duration_ms
               ) VALUES (?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, ?, ?::jsonb, ?::jsonb, ?)""",
            log.executionId, log.activityInstanceId, log.activityName, log.requestUrl,
            log.httpMethod, log.requestHeaders, log.requestBody, log.responseStatus,
            log.responseHeaders, log.responseBody, log.durationMs
        )
    }

    override fun getLogsByExecutionId(executionId: UUID, slaBreachOnly: Boolean): List<TransactionLog> =
        jdbc.query(
            """SELECT * FROM http_transaction_logs
               WHERE execution_id = ? AND (? = FALSE OR is_sla_breach)
               ORDER BY id""",
            mapper, executionId, slaBreachOnly
        )

    override fun summary(executionId: UUID): ExecutionSummary = jdbc.queryForObject(
        """SELECT COUNT(*) AS steps,
                  COUNT(*) FILTER (WHERE is_sla_breach) AS breaches,
                  COUNT(*) FILTER (WHERE response_status >= 400
                                      OR response_status = 0) AS errors,
                  COALESCE(AVG(duration_ms), 0)::int AS avg_ms
           FROM http_transaction_logs WHERE execution_id = ?""",
        { rs, _ -> ExecutionSummary(rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getInt(4)) },
        executionId
    )!!
}
