package io.github.pierrecarre.gelfloggingdemo.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GelfLoggingResource {
    private static final Logger logger = LoggerFactory.getLogger(GelfLoggingResource.class);

    @GetMapping("/gelf-logging")
    public void log() {
        logger.info("Some useful log message");
    }
}
