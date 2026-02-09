package tools.spirals.cerberus237.adaptationactionsbase.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.PingCmd;

import tools.spirals.cerberus237.adaptationactionsbase.docker.actions.ExecCommandAction;
import tools.spirals.cerberus237.adaptationactionsbase.docker.actions.KillContainerAction;
import tools.spirals.cerberus237.adaptationactionsbase.docker.actions.PauseContainerAction;
import tools.spirals.cerberus237.adaptationactionsbase.docker.actions.RemoveContainerAction;
import tools.spirals.cerberus237.adaptationactionsbase.docker.actions.RestartContainerAction;
import tools.spirals.cerberus237.adaptationactionsbase.docker.actions.ScaleInAction;
import tools.spirals.cerberus237.adaptationactionsbase.docker.actions.ScaleOutAction;
import tools.spirals.cerberus237.adaptationactionsbase.docker.actions.StartContainerAction;
import tools.spirals.cerberus237.adaptationactionsbase.docker.actions.StopContainerAction;
import tools.spirals.cerberus237.adaptationactionsbase.docker.actions.UnpauseContainerAction;
import tools.spirals.cerberus237.adaptationactionsbase.docker.actions.UpdateResourcesAction;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class DockerActionFactoryTest {

    private DockerClient mockDockerClient;
    private DockerActionFactory factory;

    @Before
    public void setUp() {
        mockDockerClient = mock(DockerClient.class);
        factory = new DockerActionFactory(mockDockerClient, 30);
    }

    // Test default constructor creates a DockerActionFactory with default settings
    @Test
    public void testDefaultConstructor() {
        DockerActionFactory defaultFactory = new DockerActionFactory();
        assertNotNull(defaultFactory.getDockerClient());
        assertEquals(30, defaultFactory.getDefaultTimeoutSeconds());
    }

    // Test that startContainer returns a StartContainerAction
    @Test
    public void testStartContainer() {
        String containerId = "testContainerId";
        StartContainerAction action = factory.startContainer(containerId);
        assertNotNull(action);
        assertEquals(containerId, action.getContainerId());
    }

    // Test that stopContainer returns a StopContainerAction
    @Test
    public void testStopContainer() {
        String containerId = "testContainerId";
        StopContainerAction action = factory.stopContainer(containerId);
        assertNotNull(action);
        assertEquals(containerId, action.getContainerId());
    }

    // Test that restartContainer returns a RestartContainerAction
    @Test
    public void testRestartContainer() {
        String containerId = "testContainerId";
        RestartContainerAction action = factory.restartContainer(containerId);
        assertNotNull(action);
        assertEquals(containerId, action.getContainerId());
    }

    // Test that pauseContainer returns a PauseContainerAction
    @Test
    public void testPauseContainer() {
        String containerId = "testContainerId";
        PauseContainerAction action = factory.pauseContainer(containerId);
        assertNotNull(action);
        assertEquals(containerId, action.getContainerId());
    }

    // Test that unpauseContainer returns an UnpauseContainerAction
    @Test
    public void testUnpauseContainer() {
        String containerId = "testContainerId";
        UnpauseContainerAction action = factory.unpauseContainer(containerId);
        assertNotNull(action);
        assertEquals(containerId, action.getContainerId());
    }

    // Test that killContainer returns a KillContainerAction
    @Test
    public void testKillContainer() {
        String containerId = "testContainerId";
        KillContainerAction action = factory.killContainer(containerId);
        assertNotNull(action);
        assertEquals(containerId, action.getContainerId());
    }

    // Test that removeContainer returns a RemoveContainerAction
    @Test
    public void testRemoveContainer() {
        String containerId = "testContainerId";
        RemoveContainerAction action = factory.removeContainer(containerId);
        assertNotNull(action);
        assertEquals(containerId, action.getContainerId());
    }

    // Test that scaleOut returns a ScaleOutAction
    @Test
    public void testScaleOut() {
        String sourceContainerId = "testContainerId";
        ScaleOutAction action = factory.scaleOut(sourceContainerId, 2);
        assertNotNull(action);
        assertEquals(sourceContainerId, action.getContainerId());
        assertEquals(2, action.getReplicaCount());
    }

    // Test that scaleIn returns a ScaleInAction
    @Test
    public void testScaleIn() {
        String containerNamePattern = "testContainer*";
        ScaleInAction action = factory.scaleIn(containerNamePattern, 1);
        assertNotNull(action);
        assertEquals(containerNamePattern, action.getContainerNamePattern());
        assertEquals(1, action.getRemoveCount());
    }

    // Test that updateResources returns an UpdateResourcesAction
    @Test
    public void testUpdateResources() {
        String containerId = "testContainerId";
        UpdateResourcesAction action = factory.updateResources(containerId);
        assertNotNull(action);
        assertEquals(containerId, action.getContainerId());
    }

    // Test that execCommand returns an ExecCommandAction
    @Test
    public void testExecCommand() {
        String containerId = "testContainerId";
        String[] command = { "echo", "Hello" }; // Changez en tableau de String
        ExecCommandAction action = factory.execCommand(containerId, command);
        assertNotNull(action);
        assertEquals(containerId, action.getContainerId());
        assertArrayEquals(command, action.getCommand());
    }

    // Test Docker availability
    @Test
    public void testIsDockerAvailable() {
        when(mockDockerClient.pingCmd()).thenReturn(new PingCmd() {
            @Override
            public Void exec() {
                return null;
            }

            @Override
            public void close() {
            }
        });
        assertTrue(factory.isDockerAvailable());
        when(mockDockerClient.pingCmd().exec()).thenThrow(new RuntimeException("Docker is not available"));
        assertFalse(factory.isDockerAvailable());
    }

    // Test Docker client close method
    @Test
    public void testClose() {
        factory.close();
    }
}