package com.sbansal.ai.template.config.otel;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;

public class GenAiSpanFilteringProcessor implements SpanProcessor {

    private final SpanProcessor delegate;

    public GenAiSpanFilteringProcessor(SpanProcessor delegate) {
        this.delegate = delegate;
    }

    @Override
    public void onStart(Context parentContext, ReadWriteSpan span) {
        // No filtering on start, pass to delegate
        delegate.onStart(parentContext, span);
    }

    @Override
    public boolean isStartRequired() {
        return delegate.isStartRequired();
    }

    @Override
    public void onEnd(ReadableSpan span) {
        if (shouldExport(span)) {
            delegate.onEnd(span);
        }
    }

    @Override
    public boolean isEndRequired() {
        return delegate.isEndRequired();
    }

    @Override
    public CompletableResultCode shutdown() {
        return delegate.shutdown();
    }

    @Override
    public CompletableResultCode forceFlush() {
        return delegate.forceFlush();
    }

    private boolean shouldExport(ReadableSpan span) {
        Attributes attributes = span.toSpanData().getAttributes();
        return attributes.asMap().keySet().stream()
                .anyMatch(attributeKey -> attributeKey.getKey().startsWith("gen_ai."));
    }
}