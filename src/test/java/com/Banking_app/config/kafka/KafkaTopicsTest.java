package com.Banking_app.config.kafka;

import com.Banking_app.support.AbstractKafkaIntegrationTest;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/** With Kafka on, the app creates its topics on the broker and can send to it. */
class KafkaTopicsTest extends AbstractKafkaIntegrationTest {

    @Autowired
    private KafkaAdmin kafkaAdmin;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Test
    void createsTheTransactionTopicAndItsDeadLetterTopicWithThreePartitions() {
        Map<String, TopicDescription> topics =
                kafkaAdmin.describeTopics(KafkaTopics.TRANSACTIONS, KafkaTopics.TRANSACTIONS_DLT);

        assertThat(topics.get(KafkaTopics.TRANSACTIONS).partitions()).hasSize(3);
        assertThat(topics.get(KafkaTopics.TRANSACTIONS_DLT).partitions()).hasSize(3);
    }

    /**
     * The producer settings (acks=all, idempotence) work against this broker version. A topic of its own, so no
     * test that reads the transaction topic later ever sees this message.
     */
    @Test
    void producerCanSendToTheBroker() throws Exception {
        String topic = "test.producer-smoke";
        kafkaAdmin.createOrModifyTopics(TopicBuilder.name(topic).partitions(1).replicas(1).build());

        RecordMetadata sent = kafkaTemplate.send(topic, "MY0000000001", "{\"hello\":\"kafka\"}")
                .get(10, TimeUnit.SECONDS)
                .getRecordMetadata();

        assertThat(sent.topic()).isEqualTo(topic);
        assertThat(sent.hasOffset()).isTrue();
    }
}
