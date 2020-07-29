package io.github.pierrecarre.logging.gelf.autoconfigure;

import io.github.pierrecarre.logging.gelf.config.GelfConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(biz.paluch.logging.gelf.logback.GelfLogbackAppender.class)
@ConditionalOnProperty(prefix = "logging.gelf", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(GelfConfig.class)
public class GelfLoggingAutoConfiguration {
}