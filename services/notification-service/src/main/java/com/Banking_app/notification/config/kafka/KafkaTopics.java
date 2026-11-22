package com.Banking_app.notification.config.kafka;

/**
 * Names of the Kafka topics (the same names as in the main app, which creates the topics; this service only reads
 * them). The ".v1" is the version of the message format: a change that old consumers could not
 * read goes to a new topic (".v2") instead of breaking them.
 */
public final class KafkaTopics {

    /** Events of completed money movements: transfers, cash deposits, foreign remittances. Key = account number. */
    public static final String TRANSACTIONS = "banking.transactions.v1";

    /**
     * Suffix of a dead-letter topic. Set explicitly in KafkaConsumerConfig: Spring Kafka's own default is "-dlt", and
     * the broker's auto-create is off, so a name that doesn't match an existing topic would make dead-lettering fail.
     */
    public static final String DLT_SUFFIX = ".DLT";

    /** Dead-letter topic: messages a consumer still could not process after its retries. */
    public static final String TRANSACTIONS_DLT = TRANSACTIONS + DLT_SUFFIX;

    private KafkaTopics() {
    }
}
