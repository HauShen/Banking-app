package com.Banking_app.config.kafka;

/**
 * A message that can never be processed, however often it is retried: not JSON, a missing field, an unknown version.
 * The error handler sends it to the dead-letter topic right away instead of retrying (KafkaConsumerConfig).
 */
public class UnprocessableMessageException extends RuntimeException {

    public UnprocessableMessageException(String message) {
        super(message);
    }

    public UnprocessableMessageException(String message, Throwable cause) {
        super(message, cause);
    }
}
