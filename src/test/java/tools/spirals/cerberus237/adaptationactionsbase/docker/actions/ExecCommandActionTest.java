package tools.spirals.cerberus237.adaptationactionsbase.docker.actions;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.ExecCreateCmd;
import com.github.dockerjava.api.command.ExecStartCmd;
import com.github.dockerjava.api.command.InspectContainerCmd;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse.ContainerState;
import com.github.dockerjava.api.command.InspectExecCmd;

import tools.spirals.cerberus237.adaptationactionsbase.exceptions.DockerActionException;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class ExecCommandActionTest {

    private DockerClient mockDockerClient;
    private ExecCreateCmd mockExecCreateCmd;
    private ExecStartCmd mockExecStartCmd;
    private InspectExecCmd mockInspectExecCmd;
    private InspectContainerCmd mockInspectContainerCmd;
    private InspectContainerResponse mockInspectContainerResponse;
    private ContainerState mockContainerState;

    private ExecCommandAction execCommandAction;

    @Before
    public void setUp() {
        mockDockerClient = mock(DockerClient.class);
        mockExecCreateCmd = mock(ExecCreateCmd.class);
        mockExecStartCmd = mock(ExecStartCmd.class);
        mockInspectExecCmd = mock(InspectExecCmd.class);
        mockInspectContainerCmd = mock(InspectContainerCmd.class);
        mockInspectContainerResponse = mock(InspectContainerResponse.class);
        mockContainerState = mock(ContainerState.class);

        when(mockDockerClient.execCreateCmd(anyString())).thenReturn(mockExecCreateCmd);
        when(mockExecCreateCmd.withAttachStdout(anyBoolean())).thenReturn(mockExecCreateCmd);
        when(mockExecCreateCmd.withAttachStderr(anyBoolean())).thenReturn(mockExecCreateCmd);
        when(mockExecCreateCmd.withCmd(any(String[].class))).thenReturn(mockExecCreateCmd);
        when(mockExecCreateCmd.withWorkingDir(anyString())).thenReturn(mockExecCreateCmd);
        when(mockExecCreateCmd.withEnv(anyList())).thenReturn(mockExecCreateCmd);
        when(mockExecCreateCmd.withPrivileged(anyBoolean())).thenReturn(mockExecCreateCmd);

        when(mockDockerClient.execStartCmd(anyString())).thenReturn(mockExecStartCmd);
        when(mockDockerClient.inspectExecCmd(anyString())).thenReturn(mockInspectExecCmd);
        when(mockDockerClient.inspectContainerCmd(anyString())).thenReturn(mockInspectContainerCmd);
        when(mockInspectContainerCmd.exec()).thenReturn(mockInspectContainerResponse);
        when(mockInspectContainerResponse.getState()).thenReturn(mockContainerState);

        // dockerUtils = mockStatic(DockerUtils.class);
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
        String containerId = "adaptable_teastore_recommender";
        execCommandAction = new ExecCommandAction(containerId, new String[] { "ls" });
        assertTrue(execCommandAction.canPerform());
    }

    // Test that perform executes the command successfully
    @Test
    public void testPerformSuccess() throws Exception {
        String containerId = "adaptable_teastore_recommender";
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
            assertEquals("Container not found: testContainerId", e.getMessage());
        }
    }

    // Test stdout capture
    @Test
    public void testGetStdout() throws Exception {
        String containerId = "adaptable_teastore_recommender";
        String[] command = { "echo", "Hello World" };
        execCommandAction = new ExecCommandAction(containerId, command);

        // Simulate perform method running with mocked behavior
        execCommandAction.perform();

        assertEquals("Hello World\n", execCommandAction.getStdout());
    }

    // Test stderr capture
    @Test
    public void testGetStderr() throws Exception {
        String containerId = "adaptable_teastore_recommender";
        String[] command = { "commandThatFails" };
        execCommandAction = new ExecCommandAction(containerId, command);

        execCommandAction.perform();

        assertNotNull(execCommandAction.getStdout());
        assertEquals("OCI runtime exec failed: exec failed: unable to start container process: exec: \"commandThatFails\": executable file not found in $PATH\r\n",
                execCommandAction.getStdout());
    }

    // Test exit code capture
    @Test
    public void testGetExitCode() throws Exception {
        String containerId = "adaptable_teastore_recommender";
        String[] command = { "echo", "Hello" };
        execCommandAction = new ExecCommandAction(containerId, command);

        execCommandAction.perform();

        assertNotNull(execCommandAction.getExitCode());
    }
}