package tools.spirals.cerberus237.adaptationactionsbase.docker.actions;

import tools.spirals.cerberus237.adaptationactionsbase.docker.DockerUtils;
import tools.spirals.cerberus237.adaptationactionsbase.exceptions.DockerActionException;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.PullResponseItem;

import static org.junit.Assert.*;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ExecCommandActionIT {

    protected static final Logger logger = LoggerFactory.getLogger(ExecCommandActionIT.class);

    private ExecCommandAction execCommandAction;

    private static final String TEST_CONTAINER_NAME = "adaptable-teastore-webui-" + UUID.randomUUID();;
    private static final String TEST_IMAGE_NAME = "cerberus237/adaptable-teastore-webui";
    private static boolean dockerIsAvailable;
    private static DockerClient dockerClient;

    @Before
    public void assumeDockerAvailable() {
        org.junit.Assume.assumeTrue(dockerIsAvailable);
    }

    @BeforeClass
    public static void setUp() {
        dockerClient = DockerUtils.createDefaultClient();
        checkIfDockerIsAvailable();
        setupTestContainer();
    }

    @AfterClass
    public static void tearsDown() {
        cleanupTestContainer();
    }

    private static void checkIfDockerIsAvailable() {
        dockerIsAvailable = DockerUtils.isDockerAvailable(dockerClient);
        if (!dockerIsAvailable)
            logger.warn("Docker is not available. You must install docker to run integration tests");
        logger.info("Docker is available running integration tests ...");
    }

    private static void setupTestContainer() {
        if (dockerIsAvailable) {
            try {
                // Check if the image already exists
                boolean imageExists = dockerClient.listImagesCmd()
                        .withImageNameFilter(TEST_IMAGE_NAME)
                        .exec().stream()
                        .anyMatch(
                                image -> image.getRepoTags().length > 0
                                        && image.getRepoTags()[0].equals(TEST_IMAGE_NAME));

                // If the image does not exist, pull it from Docker Hub
                if (!imageExists) {
                    logger.info("Image '{} does not exist; downloading...", TEST_IMAGE_NAME);
                    CountDownLatch latch = new CountDownLatch(1);
                    dockerClient.pullImageCmd(TEST_IMAGE_NAME)
                            .exec(new PullImageResultCallback() {
                                @Override
                                public void onNext(PullResponseItem item) {
                                    super.onNext(item);
                                }

                                @Override
                                public void onComplete() {
                                    super.onComplete();
                                    latch.countDown();
                                }
                            });
                    latch.await(2, TimeUnit.MINUTES); // Wait for the download to complete
                    logger.info("Image '{}' has been downloaded.", TEST_IMAGE_NAME);
                }

                // Check if the container already exists
                boolean containerExists = dockerClient.listContainersCmd()
                        .withShowAll(true) // Show all containers, including stopped ones
                        .exec().stream()
                        .anyMatch(container -> container.getNames()[0].equals("/" + TEST_CONTAINER_NAME));

                if (!containerExists) {
                    // Create the container using the hello-world image
                    CreateContainerResponse container = dockerClient.createContainerCmd(TEST_IMAGE_NAME)
                            .withName(TEST_CONTAINER_NAME) // Assign a name to the container
                            .exec();

                    // Start the container
                    dockerClient.startContainerCmd(container.getId()).exec();
                    logger.info("Container created and started with ID: {}", container.getId());
                } else {
                    logger.info("The container '{}' already exists.", TEST_CONTAINER_NAME);
                }

            } catch (DockerException | InterruptedException e) {
                logger.error("An error occurred: {}", e.getMessage(), e);
            }
        }
    }

    private static void cleanupTestContainer() {
        if (dockerIsAvailable) {
            try {
                // Check if the container exists
                boolean containerExists = dockerClient.listContainersCmd()
                        .withShowAll(true) // Show all containers, including stopped ones
                        .exec().stream()
                        .anyMatch(container -> container.getNames()[0].equals("/" + TEST_CONTAINER_NAME));

                if (containerExists) {
                    // Stop the container if it's running
                    dockerClient.stopContainerCmd(TEST_CONTAINER_NAME).exec();
                    logger.info("Stopped the container '{}'.", TEST_CONTAINER_NAME);

                    // Remove the container
                    dockerClient.removeContainerCmd(TEST_CONTAINER_NAME).exec();
                    logger.info("Removed the container '{}'.", TEST_CONTAINER_NAME);
                } else {
                    logger.info("The container '{}' does not exist; nothing to clean up.", TEST_CONTAINER_NAME);
                }
            } catch (DockerException e) {
                logger.error("Error during container cleanup: {}", e.getMessage(), e);
            }
        }
    }

    // Test constructor with command only
    @Test
    public void testConstructorWithCommand() {
        String containerId = "testContainerId";
        String[] command = { "echo", "Hello" };
        execCommandAction = new ExecCommandAction(containerId, command);
        assertNotNull(execCommandAction);
        assertArrayEquals(command, execCommandAction.getCommand());
    }

    // Test that canPerform returns true when conditions are met
    @Test
    public void testCanPerform() {
        String containerId = TEST_CONTAINER_NAME;
        execCommandAction = new ExecCommandAction(containerId, new String[] { "ls" });
        assertTrue(execCommandAction.canPerform());
    }

    // Test that perform executes the command successfully
    @Test
    public void testPerformSuccess() throws Exception {
        String containerId = TEST_CONTAINER_NAME;
        String[] command = { "echo", "Hello" };
        execCommandAction = new ExecCommandAction(containerId, command);

        // Perform the action
        execCommandAction.perform();

        // Verify the outputs
        assertEquals("Hello\n", execCommandAction.getStdout());
        assertEquals("", execCommandAction.getStderr());
        assertEquals(Long.valueOf(0), execCommandAction.getExitCode()); // Simulate success with exit code 0
        assertTrue(execCommandAction.isCommandSuccessful());
    }

    // Test that perform handles exceptions
    @Test
    public void testPerformHandlesExceptions() {
        String containerId = "testContainerId";
        String[] command = { "invalidCommand" };
        execCommandAction = new ExecCommandAction(containerId, command);

        try {
            execCommandAction.perform();
            fail("Expected DockerActionException to be thrown");
        } catch (DockerActionException e) {
            assertTrue(e.getMessage().contains("Container not found"));
        }
    }

    // Test stdout capture
    @Test
    public void testGetStdout() throws Exception {
        String containerId = TEST_CONTAINER_NAME;
        String[] command = { "echo", "Hello World" };
        execCommandAction = new ExecCommandAction(containerId, command);

        // Simulate perform method running with mocked behavior
        execCommandAction.perform();

        assertEquals("Hello World\n", execCommandAction.getStdout());
    }

    // Test stderr capture
    @Test
    public void testGetStderr() throws Exception {
        String containerId = TEST_CONTAINER_NAME;
        String[] command = { "commandThatFails" };
        execCommandAction = new ExecCommandAction(containerId, command);

        execCommandAction.perform();

        assertNotNull(execCommandAction.getStdout());
        assertTrue(execCommandAction.getStdout().contains("not found"));
    }

    // Test exit code capture
    @Test
    public void testGetExitCode() throws Exception {
        String containerId = TEST_CONTAINER_NAME;
        String[] command = { "echo", "Hello" };
        execCommandAction = new ExecCommandAction(containerId, command);

        execCommandAction.perform();

        assertNotNull(execCommandAction.getExitCode());
    }
}