package com.aavita.config.alexa;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "alexa")
@Getter
@Setter
public class AlexaProperties {

    /**
     * Shared secret sent by Alexa Developer Console in X-Alexa-Secret header.
     * Must match the value configured in the Alexa Smart Home Skill endpoint settings.
     *
     * Set in application.yml:
     *   alexa:
     *     shared-secret: Aavita@Alexa#2026
     */
    private String sharedSecret;
}