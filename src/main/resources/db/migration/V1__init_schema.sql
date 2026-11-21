-- 1. Пользователи и роли (ФТ-1)
CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,           -- BCrypt
    first_name    VARCHAR(100),
    last_name     VARCHAR(100),
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_roles (
    id        BIGSERIAL PRIMARY KEY,
    user_id   UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role_name VARCHAR(50) NOT NULL
        CHECK (role_name IN ('ROLE_ANALYST', 'ROLE_QA', 'ROLE_DEVOPS')),
    UNIQUE (user_id, role_name)
);

-- 2. Шаблоны тестовых сценариев (ссылка на BPMN-процесс в движке Camunda)
CREATE TABLE test_suites (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                   VARCHAR(255) NOT NULL,
    target_api_version     VARCHAR(50),
    process_definition_key VARCHAR(100) NOT NULL UNIQUE,
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 3. Сессии запуска (ФТ-3, ФТ-6); статусы соответствуют диаграмме состояний
CREATE TABLE test_executions (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    suite_id       UUID        NOT NULL REFERENCES test_suites (id) ON DELETE CASCADE,
    user_id        UUID        REFERENCES users (id) ON DELETE SET NULL,
    trigger_source VARCHAR(50) NOT NULL
        CHECK (trigger_source IN ('Manual', 'GitLab_CI', 'Scheduler')),
    global_status  VARCHAR(20) NOT NULL DEFAULT 'CREATED'
        CHECK (global_status IN ('CREATED', 'RUNNING', 'WAITING_SLA', 'SUCCESS', 'FAILED')),
    started_at     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at    TIMESTAMPTZ
);
CREATE INDEX idx_test_executions_status ON test_executions (global_status);
CREATE INDEX idx_test_executions_suite  ON test_executions (suite_id, started_at DESC);

-- 4. HTTP-трассировка шагов (ФТ-7); тела запросов и ответов хранятся в JSONB
CREATE TABLE http_transaction_logs (
    id                    BIGSERIAL PRIMARY KEY,
    execution_id          UUID         NOT NULL
        REFERENCES test_executions (id) ON DELETE CASCADE,
    activity_execution_id VARCHAR(100) NOT NULL,   -- id экземпляра шага в Camunda
    activity_name         VARCHAR(255) NOT NULL,
    request_url           TEXT         NOT NULL,
    http_method           VARCHAR(10)  NOT NULL,   -- GET / POST / PUT / DELETE / SQL
    request_headers       JSONB        NOT NULL DEFAULT '{}',
    request_body          JSONB,
    response_status       INTEGER      NOT NULL,   -- 0 = сеть недоступна / таймаут
    response_headers      JSONB        NOT NULL DEFAULT '{}',
    response_body         JSONB,
    duration_ms           INTEGER      NOT NULL CHECK (duration_ms >= 0),
    is_sla_breach         BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_logs_execution ON http_transaction_logs (execution_id);
-- частичный индекс ускоряет отчёты о нарушениях SLA
CREATE INDEX idx_logs_sla_breach ON http_transaction_logs (execution_id)
    WHERE is_sla_breach;

-- 5. Автоматическая фиксация нарушений SLA (ФТ-8, НФТ-1: порог 500 мс)
CREATE OR REPLACE FUNCTION check_sla_limit() RETURNS TRIGGER AS $$
BEGIN
    NEW.is_sla_breach := NEW.duration_ms > 500;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_before_insert_log
    BEFORE INSERT ON http_transaction_logs
    FOR EACH ROW EXECUTE FUNCTION check_sla_limit();
