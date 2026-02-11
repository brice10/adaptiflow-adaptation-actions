package tools.spirals.cerberus237.adaptationactionsbase.docker.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.spirals.cerberus237.adaptationactionsbase.docker.DockerUtils;
import tools.spirals.cerberus237.adaptationactionsbase.enums.AdaptationActionResult;

public class KillContainerActionIT extends AbstractDockertActionIT {

    protected static final Logger logger = LoggerFactory.getLogger(KillContainerActionIT.class);

    private KillContainerAction killContainerAction;

    @Before
    public void setup() {
        // Start the container
        if (!DockerUtils.isContainerRunning(dockerClient, TEST_CONTAINER_NAME))
            dockerClient.startContainerCmd(TEST_CONTAINER_NAME).exec();
    }

    @Test
    public void testCanPerform_whenRunning() {
        killContainerAction = new KillContainerAction(TEST_CONTAINER_NAME);
        assertTrue(killContainerAction.canPerform());
    }

    @Test
    public void testPerformKillSuccess() throws Exception {
        killContainerAction = new KillContainerAction(TEST_CONTAINER_NAME);

        AdaptationActionResult result = killContainerAction.perform();

        assertEquals(AdaptationActionResult.SUCCESS, result);
        assertFalse(DockerUtils.isContainerRunning(dockerClient, TEST_CONTAINER_NAME));
    }

    @Test
    public void testPerformSkipped_whenAlreadyStopped() throws Exception {
        killContainerAction = new KillContainerAction(TEST_CONTAINER_NAME);

        // First kill
        killContainerAction.perform();

        // Second kill should be skipped
        AdaptationActionResult result = killContainerAction.perform();

        assertEquals(AdaptationActionResult.SKIPPED, result);
    }

    @Test
    public void testRollbackAfterKill() throws Exception {
        killContainerAction = new KillContainerAction(TEST_CONTAINER_NAME);

        AdaptationActionResult killResult = killContainerAction.perform();
        assertEquals(AdaptationActionResult.SUCCESS, killResult);

        AdaptationActionResult rollbackResult = killContainerAction.rollback();

        assertEquals(AdaptationActionResult.ROLLED_BACK, rollbackResult);
        assertTrue(DockerUtils.isContainerRunning(dockerClient, TEST_CONTAINER_NAME));
    }

    @Test
    public void testRollbackSkippedIfNotKilled() {
        killContainerAction = new KillContainerAction(TEST_CONTAINER_NAME);

        AdaptationActionResult rollbackResult = killContainerAction.rollback();

        assertEquals(AdaptationActionResult.SKIPPED, rollbackResult);
    }

    @Test
    public void testCustomSignalConstructor() {
        killContainerAction = new KillContainerAction(TEST_CONTAINER_NAME, "SIGTERM");
        assertEquals("SIGTERM", killContainerAction.getSignal());
    }
}
