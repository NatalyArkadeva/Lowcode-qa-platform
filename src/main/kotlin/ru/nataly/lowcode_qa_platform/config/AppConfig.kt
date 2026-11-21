package ru.nataly.lowcode_qa_platform.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate
import java.time.Duration

@Configuration
class AppConfig {

    @Bean
    fun restTemplate(
        builder: RestTemplateBuilder,
        @Value("\${qa.http.timeout-ms}") timeoutMs: Long
    ): RestTemplate = builder
        .setConnectTimeout(Duration.ofMillis(timeoutMs))
        .setReadTimeout(Duration.ofMillis(timeoutMs))
        .build()
}
