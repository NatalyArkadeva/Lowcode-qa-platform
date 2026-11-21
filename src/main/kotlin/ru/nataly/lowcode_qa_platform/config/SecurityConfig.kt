package ru.nataly.lowcode_qa_platform.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() } // REST API с Basic-аутентификацией (вызов из CI/CD)
            .authorizeHttpRequests { auth ->
                // запуск сессий: вручную (QA) и из конвейера CI/CD (DevOps)
                auth.requestMatchers(HttpMethod.POST, "/api/executions/*/start")
                    .hasAnyRole("QA", "DEVOPS")
                // ручная смена статуса сессии
                auth.requestMatchers(HttpMethod.PATCH, "/api/executions/*/status")
                    .hasAnyRole("QA", "DEVOPS")
                // просмотр сессий и отчётов доступен всем ролям
                auth.requestMatchers("/api/executions/**", "/api/reports/**")
                    .hasAnyRole("ANALYST", "QA", "DEVOPS")
                // развёртывание BPMN-схем в движок выполняет аналитик
                auth.requestMatchers("/engine-rest/deployment/**")
                    .permitAll()
                auth.requestMatchers("/camunda/**", "/engine-rest/**")
                    .hasAnyRole("ANALYST", "QA", "DEVOPS")
                auth.anyRequest().authenticated()
            }
            .httpBasic(Customizer.withDefaults())
        return http.build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun userDetailsService(jdbc: JdbcTemplate) = UserDetailsService { username ->
        val row = jdbc.query(
            "SELECT password_hash, is_active FROM users WHERE username = ?",
            { rs, _ -> rs.getString("password_hash") to rs.getBoolean("is_active") },
            username
        ).firstOrNull() ?: throw UsernameNotFoundException("Пользователь не найден: $username")

        val roles = jdbc.queryForList(
            """SELECT r.role_name FROM user_roles r
               JOIN users u ON u.id = r.user_id WHERE u.username = ?""",
            String::class.java, username
        )
        User.withUsername(username)
            .password(row.first)
            .disabled(!row.second)
            .authorities(roles.map { SimpleGrantedAuthority(it) })
            .build()
    }
}
