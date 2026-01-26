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
package tools.spirals.cerberus237.adaptationactionsbase.docker.actions;

import com.github.dockerjava.api.DockerClient;
import tools.spirals.cerberus237.adaptationactionsbase.docker.AbstractDockerAction;
import tools.spirals.cerberus237.adaptationactionsbase.enums.AdaptationActionResult;
import tools.spirals.cerberus237.adaptationactionsbase.enums.DockerActionType;
import tools.spirals.cerberus237.adaptationactionsbase.exceptions.DockerActionException;

/**
 * Adaptation action that restarts a Docker container.
 * <p>
 * This action stops and then starts the container. It can be useful for
 * applying configuration changes or recovering from certain error states.
 * </p>
 *
 * @author Arléon Zemtsop (Cerberus)
 */
public class RestartContainerAction extends AbstractDockerAction {

    private boolean wasRestarted = false;
    private String previousState = null;

    /**
     * Constructs a new RestartContainerAction for the specified container.
     *
     * @param containerId the ID or name of the container to restart
     */
    public RestartContainerAction(String containerId) {
        super(containerId, DockerActionType.RESTART_CONTAINER);
    }

    /**
     * Constructs a new RestartContainerAction with a custom timeout.
     *
     * @param containerId    the ID or name of the container to restart
     * @param timeoutSeconds the timeout in seconds for the restart operation
     */
    public RestartContainerAction(String containerId, int timeoutSeconds) {
        super(containerId, DockerActionType.RESTART_CONTAINER, timeoutSeconds);
    }

    /**
     * Constructs a new RestartContainerAction with a custom Docker client.
     *
     * @param containerId    the ID or name of the container to restart
     * @param timeoutSeconds the timeout in seconds
     * @param dockerClient   the Docker client to use
     */
    public RestartContainerAction(String containerId, int timeoutSeconds, DockerClient dockerClient) {
        super(containerId, DockerActionType.RESTART_CONTAINER, timeoutSeconds, dockerClient);
    }

    @Override
    public boolean canPerform() {
        if (!super.canPerform()) {
            return false;
        }
        try {
            // Container must exist to be restarted
            findContainer(containerId);
            return true;
        } catch (Exception e) {
            logger.warn("Cannot perform restart action: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public AdaptationActionResult perform() {
        logActionStart();

        try {
            // Store previous state for potential rollback/logging
            if (isContainerRunning(containerId)) {
                previousState = "running";
            } else if (isContainerPaused(containerId)) {
                previousState = "paused";
            } else {
                previousState = "stopped";
            }

            // Find and restart the container
            findContainer(containerId);
            dockerClient.restartContainerCmd(containerId)
                    .withTimeout(timeoutSeconds)
                    .exec();
            wasRestarted = true;

            // Wait for container to be running
            int attempts = 0;
            int maxAttempts = timeoutSeconds * 2; // Allow time for stop + start
            while (attempts < maxAttempts && !isContainerRunning(containerId)) {
                Thread.sleep(1000);
                attempts++;
            }

            if (!isContainerRunning(containerId)) {
                throw new DockerActionException(
                        "Container failed to restart within timeout",
                        containerId,
                        DockerActionType.RESTART_CONTAINER
                );
            }

            logActionSuccess();
            return AdaptationActionResult.SUCCESS;

        } catch (DockerActionException e) {
            logActionFailure(e);
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logActionFailure(e);
            return AdaptationActionResult.FAILURE;
        } catch (Exception e) {
            logActionFailure(e);
            throw new DockerActionException(
                    "Failed to restart container: " + e.getMessage(),
                    e,
                    containerId,
                    DockerActionType.RESTART_CONTAINER
            );
        }
    }

    @Override
    public boolean supportsRollback() {
        // Restart cannot truly be rolled back, but we can attempt to restore previous state
        return true;
    }

    @Override
    public AdaptationActionResult rollback() {
        if (!wasRestarted || previousState == null) {
            logger.info("No rollback needed - container was not restarted by this action");
            return AdaptationActionResult.SKIPPED;
        }

        logger.info("Attempting to restore container {} to previous state: {}", containerId, previousState);

        try {
            switch (previousState) {
                case "stopped":
                    dockerClient.stopContainerCmd(containerId)
                            .withTimeout(timeoutSeconds)
                            .exec();
                    break;
                case "paused":
                    dockerClient.pauseContainerCmd(containerId).exec();
                    break;
                case "running":
                    // Container should already be running after restart
                    if (!isContainerRunning(containerId)) {
                        dockerClient.startContainerCmd(containerId).exec();
                    }
                    break;
                default:
                    logger.warn("Unknown previous state: {}", previousState);
                    return AdaptationActionResult.ROLLBACK_FAILED;
            }

            wasRestarted = false;
            logger.info("Successfully restored container to previous state: {}", previousState);
            return AdaptationActionResult.ROLLED_BACK;

        } catch (Exception e) {
            logger.error("Failed to rollback restart action: {}", e.getMessage(), e);
            return AdaptationActionResult.ROLLBACK_FAILED;
        }
    }

    /**
     * Returns the previous state of the container before restart.
     *
     * @return the previous state or null if not yet executed
     */
    public String getPreviousState() {
        return previousState;
    }
}
