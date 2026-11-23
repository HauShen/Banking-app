package com.Banking_app.support;

import com.github.dockerjava.api.command.InspectContainerResponse;
import org.testcontainers.kafka.KafkaContainer;

import java.io.IOException;

/**
 * Testcontainers' {@link KafkaContainer} with a start-up race fixed (Testcontainers 1.21.4, issue #11682).
 *
 * <p>The stock container starts with a loop that waits until {@code /tmp/testcontainers_start.sh} exists and then
 * runs it. Testcontainers copies that script in while the container is already running, and the file exists a moment
 * before the copy has finished. If the loop sees it in that moment, running it fails with "Text file busy", the
 * container exits with code 126, and the wait for Kafka's "RECOVERY to RUNNING" log line fails. It happens only now
 * and then, mostly on busy CI machines.
 *
 * <p>Here the loop waits for a separate marker file instead, which is created only after the copy is complete.
 */
public class ReliableKafkaContainer extends KafkaContainer {

    private static final String START_SCRIPT = "/tmp/testcontainers_start.sh";
    private static final String SCRIPT_READY = "/tmp/testcontainers_start.ready";

    public ReliableKafkaContainer(String imageName) {
        super(imageName);
        withCommand("sh", "-c", "while [ ! -f " + SCRIPT_READY + " ]; do sleep 0.1; done; " + START_SCRIPT);
    }

    @Override
    protected void containerIsStarting(InspectContainerResponse containerInfo) {
        super.containerIsStarting(containerInfo);   // copies the start script into the container
        try {
            ExecResult result = execInContainer("touch", SCRIPT_READY);
            if (result.getExitCode() != 0) {
                throw new IllegalStateException("Could not create " + SCRIPT_READY + ": " + result.getStderr());
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not create " + SCRIPT_READY, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while creating " + SCRIPT_READY, e);
        }
    }
}
