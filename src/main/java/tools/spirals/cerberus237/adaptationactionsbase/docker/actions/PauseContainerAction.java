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
 * Adaptation action that pauses a running Docker container.
 * <p>
 * This action suspends all processes in a container using the SIGSTOP signal.
 * The container remains in memory and can be resumed with an unpause action.
 * </p>
 *
 * @author Arléon Zemtsop (Cerberus)
 */
public class PauseContainerAction extends AbstractDockerAction {

    private boolean wasPaused = false;

    /**
     * Constructs a new PauseContainerAction for the specified container.
     *
     * @param containerId the ID or name of the container to pause
     */
    public PauseContainerAction(String containerId) {
        super(containerId, DockerActionType.PAUSE_CONTAINER);
    }

    /**
     * Constructs a new PauseContainerAction with a custom timeout.
     *
     * @param containerId    the ID or name of the container to pause
     * @param timeoutSeconds the timeout in seconds
     */
    public PauseContainerAction(String containerId, int timeoutSeconds) {
        super(containerId, DockerActionType.PAUSE_CONTAINER, timeoutSeconds);
    }

    /**
     * Constructs a new PauseContainerAction with a custom Docker client.
     *
     * @param containerId    the ID or name of the container to pause
     * @param timeoutSeconds the timeout in seconds
     * @param dockerClient   the Docker client to use
     */
    public PauseContainerAction(String containerId, int timeoutSeconds, DockerClient dockerClient) {
        super(containerId, DockerActionType.PAUSE_CONTAINER, timeoutSeconds, dockerClient);
    }

    @Override
    public boolean canPerform() {
        if (!super.canPerform()) {
            return false;
        }
        try {
            // Container must be running to be paused
            return isContainerRunning(containerId);
        } catch (Exception e) {
            logger.warn("Cannot perform pause action: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public AdaptationActionResult perform() {
        logActionStart();

        try {
            // Check if container is already paused
            if (isContainerPaused(containerId)) {
                logger.info("Container {} is already paused", containerId);
                return AdaptationActionResult.SKIPPED;
            }

            // Check if container is running
            if (!isContainerRunning(containerId)) {
                logger.warn("Container {} is not running, cannot pause", containerId);
                return AdaptationActionResult.SKIPPED;
            }

            // Find and pause the container
            findContainer(containerId);
            dockerClient.pauseContainerCmd(containerId).exec();
            wasPaused = true;

            // Verify container is paused
            Thread.sleep(500); // Brief wait for state change
            if (!isContainerPaused(containerId)) {
                throw new DockerActionException(
                        "Container state did not change to paused",
                        containerId,
                        DockerActionType.PAUSE_CONTAINER
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
                    "Failed to pause container: " + e.getMessage(),
                    e,
                    containerId,
                    DockerActionType.PAUSE_CONTAINER
            );
        }
    }

    @Override
    public boolean supportsRollback() {
        return true;
    }

    @Override
    public AdaptationActionResult rollback() {
        if (!wasPaused) {
            logger.info("No rollback needed - container was not paused by this action");
            return AdaptationActionResult.SKIPPED;
        }

        logger.info("Rolling back pause action - unpausing container: {}", containerId);

        try {
            dockerClient.unpauseContainerCmd(containerId).exec();

            // Verify container is running
            Thread.sleep(500);
            if (!isContainerRunning(containerId)) {
                logger.warn("Container did not resume running state during rollback");
                return AdaptationActionResult.ROLLBACK_FAILED;
            }

            wasPaused = false;
            logger.info("Successfully rolled back pause action for container: {}", containerId);
            return AdaptationActionResult.ROLLED_BACK;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Rollback interrupted for container: {}", containerId);
            return AdaptationActionResult.ROLLBACK_FAILED;
        } catch (Exception e) {
            logger.error("Failed to rollback pause action: {}", e.getMessage(), e);
            return AdaptationActionResult.ROLLBACK_FAILED;
        }
    }
}
