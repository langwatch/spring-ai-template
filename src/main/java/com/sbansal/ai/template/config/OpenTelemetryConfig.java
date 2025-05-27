package com.sbansal.ai.template.config;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
                        return new GenAiSpanFilteringProcessor(processor);
                    }
                    return processor;
                });
            }
        };
    }

    public static class GenAiSpanFilteringProcessor implements SpanProcessor {

        private final SpanProcessor delegate;
        private final Set<String> exportableTraceIds = ConcurrentHashMap.newKeySet();

        public GenAiSpanFilteringProcessor(SpanProcessor delegate) {
            this.delegate = delegate;
        }

        private boolean isGenAiSpan(ReadableSpan span) {
            Attributes attributes = span.toSpanData().getAttributes();
            return attributes.asMap().keySet().stream()
                    .anyMatch(attributeKey -> attributeKey.getKey().startsWith("gen_ai."));
        }

        @Override
        public void onStart(Context parentContext, ReadWriteSpan span) {
            if (isGenAiSpan(span)) {
                exportableTraceIds.add(span.getSpanContext().getTraceId());
            }
            delegate.onStart(parentContext, span);
        }

        @Override
        public boolean isStartRequired() {
            return delegate.isStartRequired();
        }

        @Override
        public void onEnd(ReadableSpan span) {
            if (isGenAiSpan(span)) {
                exportableTraceIds.add(span.getSpanContext().getTraceId());
            }

            if (exportableTraceIds.contains(span.getSpanContext().getTraceId())) {
                delegate.onEnd(span);
            }
        }

        @Override
        public boolean isEndRequired() {
            return true;
        }

        @Override
        public CompletableResultCode shutdown() {
            return delegate.shutdown();
        }

        @Override
        public CompletableResultCode forceFlush() {
            return delegate.forceFlush();
        }
    }
}