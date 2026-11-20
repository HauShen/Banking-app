package com.Banking_app.config.kafka;

import com.Banking_app.support.AbstractIntegrationTest;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * With {@code app.kafka.enabled=false} (the normal test suite) the app declares no topics, so KafkaAdmin never
 * tries to reach a broker, and the app still starts and serves requests without one.
 */
class KafkaDisabledTest extends AbstractIntegrationTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void noTopicsAreDeclaredAndTheAppRunsWithoutABroker() {
        assertThat(context.getBeansOfType(NewTopic.class)).isEmpty();
        assertStatus(get("/actuator/health", null), 200);
    }
}
