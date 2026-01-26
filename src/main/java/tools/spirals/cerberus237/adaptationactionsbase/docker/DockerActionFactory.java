/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package tools.spirals.cerberus237.adaptationactionsbase.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import tools.spirals.cerberus237.adaptationactionsbase.docker.actions.*;

import java.time.Duration;

/**
 * Factory class for creating Docker adaptation actions.
 * <p>
 * This factory provides convenient methods for creating various Docker
 * adaptation actions with shared Docker client instances.
 * </p>
 *
 * @author Arléon Zemtsop (Cerberus)
 */
public class DockerActionFactory {

    private static final String DEFAULT_DOCKER_HOST = "unix:///var/run/docker.sock";
    private static final int DEFAULT_TIMEOUT_SECONDS = 30;

    private final DockerClient dockerClient;
    private final int defaultTimeoutSeconds;

    /**
     * Creates a new DockerActionFactory with default settings.
     */
    public DockerActionFactory() {
        this(DEFAULT_DOCKER_HOST, DEFAULT_TIMEOUT_SECONDS);
    }

    /**
     * Creates a new DockerActionFactory with the specified Docker host.
     *
     * @param dockerHost the Docker host URI
     */
    public DockerActionFactory(String dockerHost) {
        this(dockerHost, DEFAULT_TIMEOUT_SECONDS);
    }

    /**
     * Creates a new DockerActionFactory with the specified settings.
     *
     * @param dockerHost            the Docker host URI
     * @param defaultTimeoutSeconds the default timeout for actions
     */
    public DockerActionFactory(String dockerHost, int defaultTimeoutSeconds) {
        this.dockerClient = createDockerClient(dockerHost);
        this.defaultTimeoutSeconds = defaultTimeoutSeconds;
    }

    /**
     * Creates a new DockerActionFactory with an existing Docker client.
     *
     * @param dockerClient          the Docker client to use
     * @param defaultTimeoutSeconds the default timeout for actions
     */
    public DockerActionFactory(DockerClient dockerClient, int defaultTimeoutSeconds) {
        this.dockerClient = dockerClient;
        this.defaultTimeoutSeconds = defaultTimeoutSeconds;
    }

    /**
     * Creates a Docker client for the specified host.
     *
     * @param dockerHost the Docker host URI
     * @return the configured Docker client
     */
    private DockerClient createDockerClient(String dockerHost) {
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(dockerHost)
                .build();

        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(100)
                .connectionTimeout(Duration.ofSeconds(30))
                .responseTimeout(Duration.ofSeconds(45))
                .build();

        return DockerClientImpl.getInstance(config, httpClient);
    }

    // ========== Container Lifecycle Actions ==========

    /**
     * Creates a StartContainerAction.
     *
     * @param containerId the container to start
     * @return the action instance
     */
    public StartContainerAction startContainer(String containerId) {
        return new StartContainerAction(containerId, defaultTimeoutSeconds, dockerClient);
    }

    /**
     * Creates a StopContainerAction.
     *
     * @param containerId the container to stop
     * @return the action instance
     */
    public StopContainerAction stopContainer(String containerId) {
        return new StopContainerAction(containerId, defaultTimeoutSeconds, dockerClient);
    }

    /**
     * Creates a RestartContainerAction.
     *
     * @param containerId the container to restart
     * @return the action instance
     */
    public RestartContainerAction restartContainer(String containerId) {
        return new RestartContainerAction(containerId, defaultTimeoutSeconds, dockerClient);
    }

    /**
     * Creates a PauseContainerAction.
     *
     * @param containerId the container to pause
     * @return the action instance
     */
    public PauseContainerAction pauseContainer(String containerId) {
        return new PauseContainerAction(containerId, defaultTimeoutSeconds, dockerClient);
    }

    /**
     * Creates an UnpauseContainerAction.
     *
     * @param containerId the container to unpause
     * @return the action instance
     */
    public UnpauseContainerAction unpauseContainer(String containerId) {
        return new UnpauseContainerAction(containerId, defaultTimeoutSeconds, dockerClient);
    }

    /**
     * Creates a KillContainerAction.
     *
     * @param containerId the container to kill
     * @return the action instance
     */
    public KillContainerAction killContainer(String containerId) {
        return new KillContainerAction(containerId, "SIGKILL", defaultTimeoutSeconds, dockerClient);
    }

    /**
     * Creates a KillContainerAction with a specific signal.
     *
     * @param containerId the container to kill
     * @param signal      the signal to send
     * @return the action instance
     */
    public KillContainerAction killContainer(String containerId, String signal) {
        return new KillContainerAction(containerId, signal, defaultTimeoutSeconds, dockerClient);
    }

    /**
     * Creates a RemoveContainerAction.
     *
     * @param containerId the container to remove
     * @return the action instance
     */
    public RemoveContainerAction removeContainer(String containerId) {
        return new RemoveContainerAction(containerId, false, false, defaultTimeoutSeconds, dockerClient);
    }

    /**
     * Creates a RemoveContainerAction with options.
     *
     * @param containerId   the container to remove
     * @param force         if true, force removal of running container
     * @param removeVolumes if true, remove associated volumes
     * @return the action instance
     */
    public RemoveContainerAction removeContainer(String containerId, boolean force, boolean removeVolumes) {
        return new RemoveContainerAction(containerId, force, removeVolumes, defaultTimeoutSeconds, dockerClient);
    }

    // ========== Scaling Actions ==========

    /**
     * Creates a ScaleOutAction to add one replica.
     *
     * @param sourceContainerId the container to replicate
     * @return the action instance
     */
    public ScaleOutAction scaleOut(String sourceContainerId) {
        return new ScaleOutAction(sourceContainerId, 1, true, "replica", defaultTimeoutSeconds, dockerClient);
    }

    /**
     * Creates a ScaleOutAction with specified replica count.
     *
     * @param sourceContainerId the container to replicate
     * @param replicaCount      the number of replicas to create
     * @return the action instance
     */
    public ScaleOutAction scaleOut(String sourceContainerId, int replicaCount) {
        return new ScaleOutAction(sourceContainerId, replicaCount, true, "replica", defaultTimeoutSeconds, dockerClient);
    }

    /**
     * Creates a ScaleOutAction with full options.
     *
     * @param sourceContainerId   the container to replicate
     * @param replicaCount        the number of replicas to create
     * @param autoStart           if true, start the replicas after creation
     * @param containerNamePrefix prefix for the new container names
     * @return the action instance
     */
    public ScaleOutAction scaleOut(String sourceContainerId, int replicaCount, 
                                    boolean autoStart, String containerNamePrefix) {
        return new ScaleOutAction(sourceContainerId, replicaCount, autoStart, 
                                   containerNamePrefix, defaultTimeoutSeconds, dockerClient);
    }

    /**
     * Creates a ScaleInAction to remove one container.
     *
     * @param containerNamePattern the pattern to match container names
     * @return the action instance
     */
    public ScaleInAction scaleIn(String containerNamePattern) {
        return new ScaleInAction(containerNamePattern, 1, null, true, defaultTimeoutSeconds, dockerClient);
    }

    /**
     * Creates a ScaleInAction with specified removal count.
     *
     * @param containerNamePattern the pattern to match container names
     * @param removeCount          the number of containers to remove
     * @return the action instance
     */
    public ScaleInAction scaleIn(String containerNamePattern, int removeCount) {
        return new ScaleInAction(containerNamePattern, removeCount, null, true, defaultTimeoutSeconds, dockerClient);
    }

    // ========== Resource Management Actions ==========

    /**
     * Creates an UpdateResourcesAction.
     *
     * @param containerId the container to update
     * @return the action instance
     */
    public UpdateResourcesAction updateResources(String containerId) {
        return new UpdateResourcesAction(containerId, defaultTimeoutSeconds, dockerClient);
    }

    // ========== Command Execution Actions ==========

    /**
     * Creates an ExecCommandAction.
     *
     * @param containerId the container to execute the command in
     * @param command     the command to execute
     * @return the action instance
     */
    public ExecCommandAction execCommand(String containerId, String... command) {
        return new ExecCommandAction(containerId, defaultTimeoutSeconds, dockerClient, 
                                      null, null, true, true, false, command);
    }

    /**
     * Creates an ExecCommandAction with a working directory.
     *
     * @param containerId the container to execute the command in
     * @param workingDir  the working directory for the command
     * @param command     the command to execute
     * @return the action instance
     */
    public ExecCommandAction execCommand(String containerId, String workingDir, String... command) {
        return new ExecCommandAction(containerId, defaultTimeoutSeconds, dockerClient, 
                                      workingDir, null, true, true, false, command);
    }

    // ========== Utility Methods ==========

    /**
     * Returns the Docker client used by this factory.
     *
     * @return the Docker client
     */
    public DockerClient getDockerClient() {
        return dockerClient;
    }

    /**
     * Returns the default timeout in seconds.
     *
     * @return the default timeout
     */
    public int getDefaultTimeoutSeconds() {
        return defaultTimeoutSeconds;
    }

    /**
     * Tests the connection to Docker.
     *
     * @return true if Docker is reachable
     */
    public boolean isDockerAvailable() {
        try {
            dockerClient.pingCmd().exec();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Closes the Docker client connection.
     */
    public void close() {
        try {
            dockerClient.close();
        } catch (Exception e) {
            // Ignore close errors
        }
    }
}
