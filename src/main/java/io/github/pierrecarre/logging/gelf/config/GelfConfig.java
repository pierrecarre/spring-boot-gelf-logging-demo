package io.github.pierrecarre.logging.gelf.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "logging.gelf")
public class GelfConfig {

    public boolean enabled = false;

    public String host;

    public int port = 12201;

    public String version;

    public String additionalFields;

    public String additionalFieldTypes;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getAdditionalFields() {
        return additionalFields;
    }

    public void setAdditionalFields(String additionalFields) {
        this.additionalFields = additionalFields;
    }

    public String getAdditionalFieldTypes() {
        return additionalFieldTypes;
    }

    public void setAdditionalFieldTypes(String additionalFieldTypes) {
        this.additionalFieldTypes = additionalFieldTypes;
    }
}
