package com.Banking_app.config.kafka;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;

/** The retry logic, with a stand-in for KafkaAdmin.initialize(): no Spring, no broker. */
class KafkaTopicsInitializerTest {

    /** Answers in turn: "unreachable" (throws), false (couldn't configure), true (done). */
    private static BooleanSupplier broker(Deque<Object> answers, int[] calls) {
        return () -> {
            calls[0]++;
            Object next = answers.pop();
            if (next instanceof RuntimeException e) {
                throw e;
            }
            return (Boolean) next;
        };
    }

    @Test
    @DisplayName("broker down at first: keeps trying (errors and false answers alike), stops after the first success")
    void retriesUntilDone() {
        Deque<Object> answers = new ArrayDeque<>();
        answers.add(new IllegalStateException("Could not create admin", new RuntimeException("broker unreachable")));
        answers.add(false);
        answers.add(true);
        int[] calls = {0};
        KafkaTopicsInitializer initializer = new KafkaTopicsInitializer(broker(answers, calls));

        initializer.ensureTopics();
        assertThat(initializer.isDone()).isFalse();
        initializer.ensureTopics();
        assertThat(initializer.isDone()).isFalse();
        initializer.ensureTopics();
        assertThat(initializer.isDone()).isTrue();

        initializer.ensureTopics();
        initializer.ensureTopics();
        assertThat(calls[0]).isEqualTo(3);   // no more calls once the topics are in place
    }

    @Test
    @DisplayName("broker up on a normal start: one call, done")
    void normalStart() {
        int[] calls = {0};
        Deque<Object> answers = new ArrayDeque<>();
        answers.add(true);
        KafkaTopicsInitializer initializer = new KafkaTopicsInitializer(broker(answers, calls));

        initializer.ensureTopics();
        initializer.ensureTopics();

        assertThat(initializer.isDone()).isTrue();
        assertThat(calls[0]).isEqualTo(1);
    }
}
