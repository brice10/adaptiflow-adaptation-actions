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
 * Adaptation action that stops a running Docker container.
 * <p>
 * This action gracefully stops a container, allowing it to perform cleanup operations
 * before termination. It supports rollback by restarting the container.
 * </p>
 *
 * @author Arléon Zemtsop (Cerberus)
 */
public class StopContainerAction extends AbstractDockerAction {

    private boolean wasStopped = false;

    /**
     * Constructs a new StopContainerAction for the specified container.
     *
     * @param containerId the ID or name of the container to stop
     */
    public StopContainerAction(String containerId) {
        super(containerId, DockerActionType.STOP_CONTAINER);
    }

    /**
     * Constructs a new StopContainerAction with a custom timeout.
     *
     * @param containerId    the ID or name of the container to stop
     * @param timeoutSeconds the timeout in seconds for graceful stop
     */
    public StopContainerAction(String containerId, int timeoutSeconds) {
        super(containerId, DockerActionType.STOP_CONTAINER, timeoutSeconds);
    }

    /**
     * Constructs a new StopContainerAction with a custom Docker client.
     *
     * @param containerId    the ID or name of the container to stop
     * @param timeoutSeconds the timeout in seconds for graceful stop
     * @param dockerClient   the Docker client to use
     */
    public StopContainerAction(String containerId, int timeoutSeconds, DockerClient dockerClient) {
        super(containerId, DockerActionType.STOP_CONTAINER, timeoutSeconds, dockerClient);
    }

    @Override
    public boolean canPerform() {
        if (!super.canPerform()) {
            return false;
        }
        try {
            // Check if container exists and is running
            return isContainerRunning(containerId);
        } catch (Exception e) {
            logger.warn("Cannot perform stop action: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public AdaptationActionResult perform() {
        logActionStart();

        try {
            // Check if container is already stopped
            if (isContainerStopped(containerId)) {
                logger.info("Container {} is already stopped", containerId);
                return AdaptationActionResult.SKIPPED;
            }

            // Find and stop the container
            findContainer(containerId);
            dockerClient.stopContainerCmd(containerId)
                    .withTimeout(timeoutSeconds)
                    .exec();
            wasStopped = true;

            // Wait for container to stop
            int attempts = 0;
            int maxAttempts = timeoutSeconds + 5; // Extra buffer for stop command
            while (attempts < maxAttempts && isContainerRunning(containerId)) {
                Thread.sleep(1000);
                attempts++;
            }

            if (isContainerRunning(containerId)) {
                throw new DockerActionException(
                        "Container failed to stop within timeout",
                        containerId,
                        DockerActionType.STOP_CONTAINER
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
                    "Failed to stop container: " + e.getMessage(),
                    e,
                    containerId,
                    DockerActionType.STOP_CONTAINER
            );
        }
    }

    @Override
    public boolean supportsRollback() {
        return true;
    }

    @Override
    public AdaptationActionResult rollback() {
        if (!wasStopped) {
            logger.info("No rollback needed - container was not stopped by this action");
            return AdaptationActionResult.SKIPPED;
        }

        logger.info("Rolling back stop action - starting container: {}", containerId);

        try {
            dockerClient.startContainerCmd(containerId).exec();

            // Wait for container to start
            int attempts = 0;
            int maxAttempts = timeoutSeconds;
            while (attempts < maxAttempts && !isContainerRunning(containerId)) {
                Thread.sleep(1000);
                attempts++;
            }

            if (!isContainerRunning(containerId)) {
                logger.warn("Container did not start within timeout during rollback");
                return AdaptationActionResult.ROLLBACK_FAILED;
            }

            wasStopped = false;
            logger.info("Successfully rolled back stop action for container: {}", containerId);
            return AdaptationActionResult.ROLLED_BACK;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Rollback interrupted for container: {}", containerId);
            return AdaptationActionResult.ROLLBACK_FAILED;
        } catch (Exception e) {
            logger.error("Failed to rollback stop action: {}", e.getMessage(), e);
            return AdaptationActionResult.ROLLBACK_FAILED;
        }
    }
}
