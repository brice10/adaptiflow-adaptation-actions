package tools.spirals.cerberus237.adaptationactionsbase.docker.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.spirals.cerberus237.adaptationactionsbase.docker.DockerUtils;
import tools.spirals.cerberus237.adaptationactionsbase.enums.AdaptationActionResult;

public class PauseContainerActionIT extends AbstractDockerActionIT {

    protected static final Logger logger = LoggerFactory.getLogger(PauseContainerActionIT.class);

    private PauseContainerAction pauseContainerAction;

    @Before
    public void setup() {
        // Start the container if it's not already running
        if (DockerUtils.isContainerPaused(dockerClient, TEST_CONTAINER_NAME)) {
            dockerClient.unpauseContainerCmd(TEST_CONTAINER_NAME).exec();
        }
    }

    @Test
    public void testCanPerform_whenRunning() {
        pauseContainerAction = new PauseContainerAction(TEST_CONTAINER_NAME);
        assertTrue(pauseContainerAction.canPerform());
    }

    @Test
    public void testPerformPauseSuccess() throws Exception {
        pauseContainerAction = new PauseContainerAction(TEST_CONTAINER_NAME);

        AdaptationActionResult result = pauseContainerAction.perform();

        assertEquals(AdaptationActionResult.SUCCESS, result);
        assertTrue(DockerUtils.isContainerPaused(dockerClient, TEST_CONTAINER_NAME));
    }

    @Test
    public void testPerformSkipped_whenAlreadyPaused() throws Exception {
        pauseContainerAction = new PauseContainerAction(TEST_CONTAINER_NAME);
        
        // First pause
        pauseContainerAction.perform();

        // Second pause should be skipped
        AdaptationActionResult result = pauseContainerAction.perform();

        assertEquals(AdaptationActionResult.SKIPPED, result);
    }

    @Test
    public void testRollbackAfterPause() throws Exception {
        pauseContainerAction = new PauseContainerAction(TEST_CONTAINER_NAME);

        AdaptationActionResult pauseResult = pauseContainerAction.perform();
        assertEquals(AdaptationActionResult.SUCCESS, pauseResult);

        AdaptationActionResult rollbackResult = pauseContainerAction.rollback();

        assertEquals(AdaptationActionResult.ROLLED_BACK, rollbackResult);
        assertTrue(DockerUtils.isContainerRunning(dockerClient, TEST_CONTAINER_NAME));
    }

    @Test
    public void testRollbackSkippedIfNotPaused() {
        pauseContainerAction = new PauseContainerAction(TEST_CONTAINER_NAME);

        AdaptationActionResult rollbackResult = pauseContainerAction.rollback();

        assertEquals(AdaptationActionResult.SKIPPED, rollbackResult);
    }
}