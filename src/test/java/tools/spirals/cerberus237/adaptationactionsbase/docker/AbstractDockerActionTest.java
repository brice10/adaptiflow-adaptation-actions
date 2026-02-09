package tools.spirals.cerberus237.adaptationactionsbase.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import org.junit.Before;
import org.junit.Test;

import tools.spirals.cerberus237.adaptationactionsbase.enums.AdaptationActionResult;
import tools.spirals.cerberus237.adaptationactionsbase.enums.DockerActionType;
import tools.spirals.cerberus237.adaptationactionsbase.exceptions.DockerActionException;

import java.time.Duration;

import static org.junit.Assert.*;

public class AbstractDockerActionTest {

    // Subclass for starting the container
    class StartDockerAction extends AbstractDockerAction {
        public StartDockerAction(String containerId) {
            super(containerId, DockerActionType.START_CONTAINER);
        }

        @Override
        public AdaptationActionResult perform() {
            logActionStart();
            // Logic for starting the container (simulation)
            logActionSuccess();
            return AdaptationActionResult.SUCCESS;
        }
    }

    // Subclass for stopping the container
    class StopDockerAction extends AbstractDockerAction {
        public StopDockerAction(String containerId) {
            super(containerId, DockerActionType.STOP_CONTAINER);
        }

        @Override
        public AdaptationActionResult perform() {
            logActionStart();
            // Logic for stopping the container (simulation)
            logActionSuccess();
            return AdaptationActionResult.SUCCESS;
        }
    }

    private DockerClient dockerClient;
    private StartDockerAction startAction;
    private StopDockerAction stopAction;

    // Initial setup for the mocked DockerClient
    @Before
    public void setUp() {
        dockerClient = createMockDockerClient();
        startAction = new StartDockerAction("mockContainerId");
        stopAction = new StopDockerAction("mockContainerId");
    }

    // Tests if the start action works
    @Test
    public void testStartActionPerform() {
        AdaptationActionResult result = startAction.perform();
        assertEquals(AdaptationActionResult.SUCCESS, result);
    }

    // Tests if the stop action works
    @Test
    public void testStopActionPerform() {
        AdaptationActionResult result = stopAction.perform();
        assertEquals(AdaptationActionResult.SUCCESS, result);
    }

    // Mock Docker client
    private DockerClient createMockDockerClient() {
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost("unix:///var/run/docker.sock")
                .build();

        return DockerClientImpl.getInstance(config,
                new ApacheDockerHttpClient.Builder()
                        .dockerHost(config.getDockerHost())
                        .sslConfig(config.getSSLConfig())
                        .maxConnections(100)
                        .connectionTimeout(Duration.ofSeconds(30))
                        .responseTimeout(Duration.ofSeconds(45))
                        .build());
    }

    // Tests the findContainer method (mock)
    @Test
    public void testFindContainer_ThrowException() {
        try {
            startAction.findContainer("nonExistentContainer");
            fail("Expected DockerActionException");
        } catch (DockerActionException e) {
            assertEquals("Container not found: nonExistentContainer", e.getMessage());
        }
    }

    // Tests the isContainerRunning method (mock)
    @Test
    public void testIsContainerRunning() {
        assertFalse(startAction.isContainerRunning("mockContainerId"));  // Simulate the container not being found
    }
}