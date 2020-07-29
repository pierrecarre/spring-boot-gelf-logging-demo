package org.springframework.boot.logging.logback;

import biz.paluch.logging.gelf.logback.GelfLogbackAppender;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy;
import ch.qos.logback.core.util.FileSize;
import ch.qos.logback.core.util.OptionHelper;
import org.springframework.boot.logging.LogFile;
import org.springframework.boot.logging.LoggingInitializationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.unit.DataSize;

import java.lang.reflect.Method;
import java.util.LinkedList;

class GelfLogbackConfiguration {

    private static final int GELF_LOGGER_PORT = 12201;

    private static final String GELF_LOGGER_ADDITIONAL_FIELDS = "ApplicationName=${SPRING_APPLICATION_NAME}";

    private static final String GELF_LOGGER_ADDITIONAL_FIELD_TYPES = "ApplicationName=String";

    private static final String CONSOLE_LOG_PATTERN = "%clr(%d{${LOG_DATEFORMAT_PATTERN:-yyyy-MM-dd HH:mm:ss.SSS}}){faint} "
            + "%clr(${LOG_LEVEL_PATTERN:-%5p}) %clr(${PID:- }){magenta} %clr(---){faint} "
            + "%clr([%15.15t]){faint} %clr(%-40.40logger{39}){cyan} "
            + "%clr(:){faint} %m%n${LOG_EXCEPTION_CONVERSION_WORD:-%wEx}";

    private static final String FILE_LOG_PATTERN = "%d{${LOG_DATEFORMAT_PATTERN:-yyyy-MM-dd HH:mm:ss.SSS}} "
            + "${LOG_LEVEL_PATTERN:-%5p} ${PID:- } --- [%t] %-40.40logger{39} : %m%n${LOG_EXCEPTION_CONVERSION_WORD:-%wEx}";

    private static final DataSize MAX_FILE_SIZE = DataSize.ofMegabytes(10);

    private static final Integer MAX_FILE_HISTORY = 7;


    private final PropertyResolver patterns;

    private final LogFile logFile;

    GelfLogbackConfiguration(LoggingInitializationContext initializationContext, LogFile logFile) {
        this.patterns = getPatternsResolver(initializationContext.getEnvironment());
        this.logFile = logFile;
    }

    private PropertyResolver getPatternsResolver(Environment environment) {
        if (environment == null) {
            return new PropertySourcesPropertyResolver(null);
        }
        if (environment instanceof ConfigurableEnvironment) {
            PropertySourcesPropertyResolver resolver = new PropertySourcesPropertyResolver(
                    ((ConfigurableEnvironment) environment).getPropertySources());
            resolver.setIgnoreUnresolvableNestedPlaceholders(true);
            return resolver;
        }
        return environment;
    }

    void apply(LogbackConfigurator config) {
        synchronized (config.getConfigurationLock()) {
            base(config);
            LinkedList<Appender<ILoggingEvent>> appenders = new LinkedList<>();
            appenders.add(consoleAppender(config));
            if (this.logFile != null) {
                appenders.add(fileAppender(config, this.logFile.toString()));
            }
            if (isGelfLoggingEnabled()) {
                appenders.add(gelfAppender(config));
            }
            config.root(Level.INFO, appenders.toArray(new Appender[]{}));
        }
    }

    private boolean isGelfLoggingEnabled() {
        return this.patterns.getProperty("logging.gelf.enabled", Boolean.class, Boolean.FALSE);
    }

    private void base(LogbackConfigurator config) {
        config.conversionRule("clr", ColorConverter.class);
        config.conversionRule("wex", WhitespaceThrowableProxyConverter.class);
        config.conversionRule("wEx", ExtendedWhitespaceThrowableProxyConverter.class);
        config.logger("org.apache.catalina.startup.DigesterFactory", Level.ERROR);
        config.logger("org.apache.catalina.util.LifecycleBase", Level.ERROR);
        config.logger("org.apache.coyote.http11.Http11NioProtocol", Level.WARN);
        config.logger("org.apache.sshd.common.util.SecurityUtils", Level.WARN);
        config.logger("org.apache.tomcat.util.net.NioSelectorPool", Level.WARN);
        config.logger("org.eclipse.jetty.util.component.AbstractLifeCycle", Level.ERROR);
        config.logger("org.hibernate.validator.internal.util.Version", Level.WARN);
        config.logger("org.springframework.boot.actuate.endpoint.jmx", Level.WARN);
    }

    private GelfLogbackAppender gelfAppender(LogbackConfigurator config) {
        GelfLogbackAppender appender = new GelfLogbackAppender();
        appender.setHost(this.patterns.getProperty("logging.gelf.host"));
        appender.setPort(this.patterns.getProperty("logging.gelf.port", Integer.class, GELF_LOGGER_PORT));
        appender.setVersion(this.patterns.getProperty("logging.gelf.version"));
        appender.setAdditionalFields(this.patterns.getProperty("logging.gelf.additional-fields", GELF_LOGGER_ADDITIONAL_FIELDS));
        appender.setAdditionalFieldTypes(this.patterns.getProperty("logging.gelf.additional-field-types", GELF_LOGGER_ADDITIONAL_FIELD_TYPES));
        config.appender("GELF", appender);
        return appender;
    }

    private Appender<ILoggingEvent> consoleAppender(LogbackConfigurator config) {
        ConsoleAppender<ILoggingEvent> appender = new ConsoleAppender<>();
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        String logPattern = this.patterns.getProperty("logging.pattern.console", CONSOLE_LOG_PATTERN);
        encoder.setPattern(OptionHelper.substVars(logPattern, config.getContext()));
        config.start(encoder);
        appender.setEncoder(encoder);
        config.appender("CONSOLE", appender);
        return appender;
    }

    private Appender<ILoggingEvent> fileAppender(LogbackConfigurator config, String logFile) {
        RollingFileAppender<ILoggingEvent> appender = new RollingFileAppender<>();
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        String logPattern = this.patterns.getProperty("logging.pattern.file", FILE_LOG_PATTERN);
        encoder.setPattern(OptionHelper.substVars(logPattern, config.getContext()));
        appender.setEncoder(encoder);
        config.start(encoder);
        appender.setFile(logFile);
        setRollingPolicy(appender, config, logFile);
        config.appender("FILE", appender);
        return appender;
    }

    private void setRollingPolicy(RollingFileAppender<ILoggingEvent> appender, LogbackConfigurator config,
                                  String logFile) {
        SizeAndTimeBasedRollingPolicy<ILoggingEvent> rollingPolicy = new SizeAndTimeBasedRollingPolicy<>();
        rollingPolicy.setCleanHistoryOnStart(
                this.patterns.getProperty("logging.file.clean-history-on-start", Boolean.class, false));
        rollingPolicy.setFileNamePattern(
                this.patterns.getProperty("logging.pattern.rolling-file-name", logFile + ".%d{yyyy-MM-dd}.%i.gz"));
        setMaxFileSize(rollingPolicy, getDataSize("logging.file.max-size", MAX_FILE_SIZE));
        rollingPolicy
                .setMaxHistory(this.patterns.getProperty("logging.file.max-history", Integer.class, MAX_FILE_HISTORY));
        DataSize totalSizeCap = getDataSize("logging.file.total-size-cap",
                DataSize.ofBytes(CoreConstants.UNBOUNDED_TOTAL_SIZE_CAP));
        rollingPolicy.setTotalSizeCap(new FileSize(totalSizeCap.toBytes()));
        appender.setRollingPolicy(rollingPolicy);
        rollingPolicy.setParent(appender);
        config.start(rollingPolicy);
    }

    private void setMaxFileSize(SizeAndTimeBasedRollingPolicy<ILoggingEvent> rollingPolicy, DataSize maxFileSize) {
        try {
            rollingPolicy.setMaxFileSize(new FileSize(maxFileSize.toBytes()));
        }
        catch (NoSuchMethodError ex) {
            // Logback < 1.1.8 used String configuration
            Method method = ReflectionUtils.findMethod(SizeAndTimeBasedRollingPolicy.class, "setMaxFileSize",
                    String.class);
            ReflectionUtils.invokeMethod(method, rollingPolicy, String.valueOf(maxFileSize.toBytes()));
        }
    }

    private DataSize getDataSize(String property, DataSize defaultSize) {
        String value = this.patterns.getProperty(property);
        if (value == null) {
            return defaultSize;
        }
        try {
            return DataSize.parse(value);
        }
        catch (IllegalArgumentException ex) {
            FileSize fileSize = FileSize.valueOf(value);
            return DataSize.ofBytes(fileSize.getSize());
        }
    }
}
