package com.sbansal.ai.template.config;

import com.sbansal.ai.template.config.otel.GenAiSpanFilteringProcessor;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "otel.exporter.otlp.traces.endpoint")
public class OpenTelemetryConfig {

    @Bean
    public AutoConfigurationCustomizerProvider otelCustomizerProvider() {
        return new AutoConfigurationCustomizerProvider() {
            @Override
            public void customize(AutoConfigurationCustomizer autoConfigurationCustomizer) {
                autoConfigurationCustomizer.addSpanProcessorCustomizer((processor, config) -> {
                    if (processor instanceof BatchSpanProcessor) {
                        // Wrap the existing BatchSpanProcessor with our filtering logic
                        return new GenAiSpanFilteringProcessor(processor);
                    }
                    // Return other processors as-is
                    return processor;
                });
            }
        };
    }
}