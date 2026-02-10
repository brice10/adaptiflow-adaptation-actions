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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.spirals.cerberus237.adaptationactionsbase.core.IAdaptationAction;
import tools.spirals.cerberus237.adaptationactionsbase.enums.DockerActionType;

import java.time.Duration;
import java.util.UUID;

/**
 * Abstract base class for Docker-based adaptation actions.
 * <p>
 * This class provides common functionality for interacting with Docker containers
 * including client initialization, container lookup, and state management.
 * </p>
 *
 * @author Arléon Zemtsop (Cerberus)
 */
public abstract class AbstractDockerAction implements IAdaptationAction {

    protected static final Logger logger = LoggerFactory.getLogger(AbstractDockerAction.class);

    protected final String actionId;
    protected String containerId;
    protected String containerName;
    protected final DockerActionType actionType;
    protected final DockerClient dockerClient;
    protected final int timeoutSeconds;

    private static final int DEFAULT_TIMEOUT_SECONDS = 30;
    private static final String DEFAULT_DOCKER_HOST = "unix:///var/run/docker.sock";

    /**
     * Constructs a new AbstractDockerAction with the specified container ID and action type.
     *
     * @param containerId the ID or name of the target container
     * @param actionType  the type of Docker action to perform
     */
    protected AbstractDockerAction(String containerId, DockerActionType actionType) {
        this(containerId, actionType, DEFAULT_TIMEOUT_SECONDS);
    }

    /**
     * Constructs a new AbstractDockerAction with the specified parameters.
     *
     * @param containerId    the ID or name of the target container
     * @param actionType     the type of Docker action to perform
     * @param timeoutSeconds the timeout in seconds for the action
     */
    protected AbstractDockerAction(String containerId, DockerActionType actionType, int timeoutSeconds) {
        this(containerId, actionType, timeoutSeconds, createDefaultDockerClient());
    }

    /**
     * Constructs a new AbstractDockerAction with a custom Docker client.
     *
     * @param containerId    the ID or name of the target container
     * @param actionType     the type of Docker action to perform
     * @param timeoutSeconds the timeout in seconds for the action
     * @param dockerClient   the Docker client to use
     */
    protected AbstractDockerAction(String containerId, DockerActionType actionType, 
                                   int timeoutSeconds, DockerClient dockerClient) {
        this.actionId = UUID.randomUUID().toString();
        this.containerId = containerId;
        this.actionType = actionType;
        this.timeoutSeconds = timeoutSeconds;
        this.dockerClient = dockerClient;
    }

    /**
     * Creates the default Docker client configured to connect to the local Docker daemon.
     *
     * @return a configured DockerClient instance
     */
    protected static DockerClient createDefaultDockerClient() {
        return createDockerClient(DEFAULT_DOCKER_HOST);
    }

    /**
     * Creates a Docker client configured to connect to the specified Docker host.
     *
     * @param dockerHost the Docker host URI
     * @return a configured DockerClient instance
     */
    protected static DockerClient createDockerClient(String dockerHost) {
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

    @Override
    public String getActionId() {
        return actionId;
    }

    @Override
    public String getDescription() {
        return String.format("%s for container: %s", actionType.getDescription(), containerId);
    }

    /**
     * Returns the Docker action type.
     *
     * @return the Docker action type
     */
    public DockerActionType getActionType() {
        return actionType;
    }

    /**
     * Returns the target container ID.
     *
     * @return the container ID
     */
    public String getContainerId() {
        return containerId;
    }

    /**
     * Returns the timeout in seconds for this action.
     *
     * @return the timeout in seconds
     */
    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    @Override
    public boolean canPerform() {
        try {
            // Check if Docker is available
            dockerClient.pingCmd().exec();
            return true;
        } catch (Exception e) {
            logger.warn("Docker is not available: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Logs the start of an action execution.
     */
    protected void logActionStart() {
        logger.info("Starting {} action [actionId={}, containerId={}]", 
                actionType.name(), actionId, containerId);
    }

    /**
     * Logs the successful completion of an action.
     */
    protected void logActionSuccess() {
        logger.info("Successfully completed {} action [actionId={}, containerId={}]", 
                actionType.name(), actionId, containerId);
    }

    /**
     * Logs an action failure.
     *
     * @param e the exception that caused the failure
     */
    protected void logActionFailure(Exception e) {
        logger.error("Failed to execute {} action [actionId={}, containerId={}]: {}", 
                actionType.name(), actionId, containerId, e.getMessage(), e);
    }
}
