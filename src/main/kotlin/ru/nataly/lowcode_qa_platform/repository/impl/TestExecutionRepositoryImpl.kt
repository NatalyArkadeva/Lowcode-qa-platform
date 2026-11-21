package ru.nataly.lowcode_qa_platform.repository.impl

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import ru.nataly.lowcode_qa_platform.domain.model.ExecutionStatus
import ru.nataly.lowcode_qa_platform.domain.model.TestExecution
import ru.nataly.lowcode_qa_platform.repository.TestExecutionRepository
import java.time.OffsetDateTime
import java.util.*

@Repository
class TestExecutionRepositoryImpl(private val jdbc: JdbcTemplate) : TestExecutionRepository {

    private val mapper = RowMapper { rs, _ ->
        TestExecution(
            id = rs.getObject("id", UUID::class.java),
            suiteId = rs.getObject("suite_id", UUID::class.java),
            triggerSource = rs.getString("trigger_source"),
            status = ExecutionStatus.valueOf(rs.getString("global_status")),
            startedAt = rs.getObject("started_at", OffsetDateTime::class.java),
            finishedAt = rs.getObject("finished_at", OffsetDateTime::class.java)
        )
    }

    override fun findProcessKey(suiteId: UUID): String? = jdbc.queryForList(
        "SELECT process_definition_key FROM test_suites WHERE id = ?",
        String::class.java, suiteId
    ).firstOrNull()

    override fun insert(id: UUID, suiteId: UUID, username: String?, source: String) {
        jdbc.update(
            """INSERT INTO test_executions
                   (id, suite_id, user_id, trigger_source, global_status)
               VALUES (?, ?, (SELECT id FROM users WHERE username = ?), ?, ?)""",
            id, suiteId, username, source, ExecutionStatus.CREATED.name
        )
    }

    override fun find(id: UUID): TestExecution? =
        jdbc.query("SELECT * FROM test_executions WHERE id = ?", mapper, id).firstOrNull()

    override fun findStatusForUpdate(id: UUID): ExecutionStatus? = jdbc.queryForList(
        "SELECT global_status FROM test_executions WHERE id = ? FOR UPDATE",
        String::class.java, id
    ).firstOrNull()?.let { ExecutionStatus.valueOf(it) }

    override fun updateStatus(id: UUID, status: ExecutionStatus) {
        jdbc.update(
            """UPDATE test_executions SET global_status = ?,
               finished_at = CASE WHEN ? THEN CURRENT_TIMESTAMP ELSE finished_at END
               WHERE id = ?""",
            status.name, status.terminal, id
        )
    }

    override fun findAll(
        status: ExecutionStatus?, suiteId: UUID?, limit: Int
    ): List<TestExecution> {
        val sql = StringBuilder("SELECT * FROM test_executions WHERE 1 = 1")
        val args = mutableListOf<Any>()
        if (status != null) { sql.append(" AND global_status = ?"); args += status.name }
        if (suiteId != null) { sql.append(" AND suite_id = ?"); args += suiteId }
        sql.append(" ORDER BY started_at DESC LIMIT ?"); args += limit
        return jdbc.query(sql.toString(), mapper, *args.toTypedArray())
    }
}
