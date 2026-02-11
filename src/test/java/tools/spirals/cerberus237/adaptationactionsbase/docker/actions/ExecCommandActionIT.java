package tools.spirals.cerberus237.adaptationactionsbase.docker.actions;

import tools.spirals.cerberus237.adaptationactionsbase.exceptions.DockerActionException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExecCommandActionIT extends AbstractDockerActionIT {

    protected static final Logger logger = LoggerFactory.getLogger(ExecCommandActionIT.class);

    private ExecCommandAction execCommandAction;

    
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
        String containerId = AbstractDockerActionIT.TEST_CONTAINER_NAME;
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