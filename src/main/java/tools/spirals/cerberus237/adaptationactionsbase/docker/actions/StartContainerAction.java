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
import com.github.dockerjava.api.model.Container;

import tools.spirals.cerberus237.adaptationactionsbase.core.IRollbackableAdaptationAction;
import tools.spirals.cerberus237.adaptationactionsbase.docker.AbstractDockerAction;
import tools.spirals.cerberus237.adaptationactionsbase.docker.DockerUtils;
import tools.spirals.cerberus237.adaptationactionsbase.enums.AdaptationActionResult;
import tools.spirals.cerberus237.adaptationactionsbase.enums.DockerActionType;
import tools.spirals.cerberus237.adaptationactionsbase.exceptions.DockerActionException;

/**
 * Adaptation action that starts a stopped Docker container.
 * <p>
 * This action supports rollback by stopping the container if it was started.
 * </p>
 *
 * @author Arléon Zemtsop (Cerberus)
 */
public class StartContainerAction extends AbstractDockerAction implements IRollbackableAdaptationAction {

    private boolean wasStarted = false;

    /**
     * Constructs a new StartContainerAction for the specified container.
     *
     * @param containerId the ID or name of the container to start
     */
    public StartContainerAction(String containerId) {
        super(containerId, DockerActionType.START_CONTAINER);
    }

    /**
     * Constructs a new StartContainerAction with a custom timeout.
     *
     * @param containerId    the ID or name of the container to start
     * @param timeoutSeconds the timeout in seconds
     */
    public StartContainerAction(String containerId, int timeoutSeconds) {
        super(containerId, DockerActionType.START_CONTAINER, timeoutSeconds);
    }

    /**
     * Constructs a new StartContainerAction with a custom Docker client.
     *
     * @param containerId    the ID or name of the container to start
     * @param timeoutSeconds the timeout in seconds
     * @param dockerClient   the Docker client to use
     */
    public StartContainerAction(String containerId, int timeoutSeconds, DockerClient dockerClient) {
        super(containerId, DockerActionType.START_CONTAINER, timeoutSeconds, dockerClient);
    }

    @Override
    public boolean canPerform() {
        if (!super.canPerform()) {
            return false;
        }
        try {
            // Check if container exists and is not already running
            return DockerUtils.isContainerStopped(dockerClient, containerId);
        } catch (Exception e) {
            logger.warn("Cannot perform start action: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public AdaptationActionResult perform() {
        logActionStart();

        try {
            Container container = DockerUtils.findContainer(dockerClient, containerId);
            if (container == null)
                throw new DockerActionException("Container not found: " + containerId, containerId, actionType);
            // Check if container is already running
            if (DockerUtils.isContainerRunning(container)) {
                logger.info("Container {} is already running", containerId);
                return AdaptationActionResult.SKIPPED;
            }

            // Start the container
            dockerClient.startContainerCmd(container.getId()).exec();
            wasStarted = true;

            // Wait for container to be running
            int attempts = 0;
            int maxAttempts = timeoutSeconds;
            while (attempts < maxAttempts && !DockerUtils.isContainerRunning(dockerClient, containerId)) {
                Thread.sleep(1000);
                attempts++;
            }

            if (!DockerUtils.isContainerRunning(dockerClient, containerId)) {
                throw new DockerActionException(
                        "Container failed to start within timeout",
                        containerId,
                        DockerActionType.START_CONTAINER);
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
                    "Failed to start container: " + e.getMessage(),
                    e,
                    containerId,
                    DockerActionType.START_CONTAINER);
        }
    }

    @Override
    public AdaptationActionResult rollback() {
        if (!wasStarted) {
            logger.info("No rollback needed - container was not started by this action");
            return AdaptationActionResult.SKIPPED;
        }

        logger.info("Rolling back start action - stopping container: {}", containerId);

        try {
            dockerClient.stopContainerCmd(containerId)
                    .withTimeout(timeoutSeconds)
                    .exec();

            // Wait for container to stop
            int attempts = 0;
            int maxAttempts = timeoutSeconds;
            while (attempts < maxAttempts && DockerUtils.isContainerRunning(dockerClient, containerId)) {
                Thread.sleep(1000);
                attempts++;
            }

            if (DockerUtils.isContainerRunning(dockerClient, containerId)) {
                logger.warn("Container did not stop within timeout during rollback");
                return AdaptationActionResult.ROLLBACK_FAILED;
            }

            wasStarted = false;
            logger.info("Successfully rolled back start action for container: {}", containerId);
            return AdaptationActionResult.ROLLED_BACK;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Rollback interrupted for container: {}", containerId);
            return AdaptationActionResult.ROLLBACK_FAILED;
        } catch (Exception e) {
            logger.error("Failed to rollback start action: {}", e.getMessage(), e);
            return AdaptationActionResult.ROLLBACK_FAILED;
        }
    }
}
