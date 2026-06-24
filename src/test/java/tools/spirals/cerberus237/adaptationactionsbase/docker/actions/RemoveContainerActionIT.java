package tools.spirals.cerberus237.adaptationactionsbase.docker.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Container;

import tools.spirals.cerberus237.adaptationactionsbase.docker.DockerUtils;
import tools.spirals.cerberus237.adaptationactionsbase.enums.AdaptationActionResult;
import tools.spirals.cerberus237.adaptationactionsbase.exceptions.DockerActionException;

public class RemoveContainerActionIT extends AbstractDockerActionIT {

    protected static final Logger logger = LoggerFactory.getLogger(RemoveContainerActionIT.class);

    private RemoveContainerAction removeContainerAction;

    @Before
    public void setup() {
        // Start the container if it's not already running
        if (DockerUtils.findContainer(dockerClient, TEST_CONTAINER_NAME) == null) {
            // Create the container using the hello-world image
            CreateContainerResponse container = dockerClient.createContainerCmd(TEST_IMAGE_NAME)
                    .withName(TEST_CONTAINER_NAME) // Assign a name to the container
                    .exec();

            // Start the container
            dockerClient.startContainerCmd(container.getId()).exec();
        }
        if (DockerUtils.isContainerStopped(dockerClient, TEST_CONTAINER_NAME)) {
            Container container = DockerUtils.findContainer(dockerClient, TEST_CONTAINER_NAME);
            // Start the container
            dockerClient.startContainerCmd(container.getId()).exec();
        }
    }

    @Test
    public void testCanPerform_whenStopped() throws Exception {
        // Stop the container to test removal
        dockerClient.stopContainerCmd(TEST_CONTAINER_NAME).exec();
        removeContainerAction = new RemoveContainerAction(TEST_CONTAINER_NAME);
        assertTrue(removeContainerAction.canPerform());
    }

    @Test
    public void testCanPerform_whenRunning_withForce() {
        removeContainerAction = new RemoveContainerAction(TEST_CONTAINER_NAME, true); // Force removal
        assertTrue(removeContainerAction.canPerform());
    }

    @Test
    public void testPerformRemoveSuccess() throws Exception {
        removeContainerAction = new RemoveContainerAction(TEST_CONTAINER_NAME);

        // First stop the container
        dockerClient.stopContainerCmd(TEST_CONTAINER_NAME).exec();

        AdaptationActionResult result = removeContainerAction.perform();

        assertEquals(AdaptationActionResult.SUCCESS, result);
        assertFalse(DockerUtils.isContainerRunning(dockerClient, TEST_CONTAINER_NAME));
        assertNull(DockerUtils.findContainer(dockerClient, TEST_CONTAINER_NAME));
    }

    @Test
    public void testPerformRemoveSuccess_withForce() throws Exception {
        removeContainerAction = new RemoveContainerAction(TEST_CONTAINER_NAME, true); // Force removal

        AdaptationActionResult result = removeContainerAction.perform();

        assertEquals(AdaptationActionResult.SUCCESS, result);
        assertNull(DockerUtils.findContainer(dockerClient, TEST_CONTAINER_NAME));
    }

    @Test
    public void testPerformSkipped_whenAlreadyRemoved() throws Exception {
        removeContainerAction = new RemoveContainerAction(TEST_CONTAINER_NAME, true);

        // First remove
        removeContainerAction.perform();

        // Second remove should fail
        try {
            removeContainerAction.perform();
            fail("Expected DockerActionException to be thrown");
        } catch (DockerActionException e) {
            assertTrue(e.getMessage().contains("Container not found"));
        }

        assertFalse(DockerUtils.isContainerRunning(dockerClient, TEST_CONTAINER_NAME)); // removal success.
        assertNull(DockerUtils.findContainer(dockerClient, TEST_CONTAINER_NAME));
    }

    @Test
    public void testCanPerform_whenPaused_withForce() throws Exception {
        // Pause the container to test removal with force
        dockerClient.pauseContainerCmd(TEST_CONTAINER_NAME).exec();
        removeContainerAction = new RemoveContainerAction(TEST_CONTAINER_NAME, true); // Force removal
        assertTrue(removeContainerAction.canPerform());
    }

    @Test
    public void testPerformRemoveSkipped_whenRunning_withoutForce() throws Exception {
        removeContainerAction = new RemoveContainerAction(TEST_CONTAINER_NAME); // No force

        AdaptationActionResult result = removeContainerAction.perform();

        assertEquals(AdaptationActionResult.SKIPPED, result);
        assertTrue(DockerUtils.isContainerRunning(dockerClient, TEST_CONTAINER_NAME));
    }
}