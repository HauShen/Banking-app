package com.Banking_app.config.kafka;

/**
 * Names of the Kafka topics. The ".v1" is the version of the message format: a change that old consumers could not
 * read goes to a new topic (".v2") instead of breaking them.
 */
public final class KafkaTopics {

    /** Events of completed money movements: transfers, cash deposits, foreign remittances. Key = account number. */
    public static final String TRANSACTIONS = "banking.transactions.v1";

    /**
     * Dead-letter topic: messages a consumer still could not process after its retries. ".DLT" is the suffix Spring
     * Kafka's DeadLetterPublishingRecoverer uses by default.
     */
    public static final String TRANSACTIONS_DLT = TRANSACTIONS + ".DLT";

    private KafkaTopics() {
    }
}
