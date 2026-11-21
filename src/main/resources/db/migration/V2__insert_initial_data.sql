-- Пароли демонстрационные; BCrypt-хэши считает pgcrypto, перед эксплуатацией сменить.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO users (username, password_hash, first_name, last_name) VALUES
    ('analyst', crypt('analyst_pass', gen_salt('bf', 10)), 'Анна',  'Системная'),
    ('qa',      crypt('qa_pass',      gen_salt('bf', 10)), 'Игорь', 'Тестов'),
    ('devops',  crypt('devops_pass',  gen_salt('bf', 10)), 'Олег',  'Конвейеров');

INSERT INTO user_roles (user_id, role_name)
SELECT id, 'ROLE_ANALYST' FROM users WHERE username = 'analyst'
UNION ALL SELECT id, 'ROLE_QA'     FROM users WHERE username = 'qa'
UNION ALL SELECT id, 'ROLE_DEVOPS' FROM users WHERE username = 'devops';

-- Пример сценария: сквозная проверка цепочки «заказ — оплата — склад»
INSERT INTO test_suites (id, name, target_api_version, process_definition_key)
VALUES ('11111111-1111-1111-1111-111111111111',
        'Сквозной тест оформления заказа', 'v2.4.0', 'order-chain-e2e-test');
