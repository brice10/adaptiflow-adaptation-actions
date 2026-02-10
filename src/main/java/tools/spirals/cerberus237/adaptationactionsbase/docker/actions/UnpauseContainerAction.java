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
 * Adaptation action that unpauses a paused Docker container.
 * <p>
 * This action resumes all processes in a paused container by sending the SIGCONT signal.
 * </p>
 *
 * @author Arléon Zemtsop (Cerberus)
 */
public class UnpauseContainerAction extends AbstractDockerAction implements IRollbackableAdaptationAction  {

    private boolean wasUnpaused = false;

    /**
     * Constructs a new UnpauseContainerAction for the specified container.
     *
     * @param containerId the ID or name of the container to unpause
     */
    public UnpauseContainerAction(String containerId) {
        super(containerId, DockerActionType.UNPAUSE_CONTAINER);
    }

    /**
     * Constructs a new UnpauseContainerAction with a custom timeout.
     *
     * @param containerId    the ID or name of the container to unpause
     * @param timeoutSeconds the timeout in seconds
     */
    public UnpauseContainerAction(String containerId, int timeoutSeconds) {
        super(containerId, DockerActionType.UNPAUSE_CONTAINER, timeoutSeconds);
    }

    /**
     * Constructs a new UnpauseContainerAction with a custom Docker client.
     *
     * @param containerId    the ID or name of the container to unpause
     * @param timeoutSeconds the timeout in seconds
     * @param dockerClient   the Docker client to use
     */
    public UnpauseContainerAction(String containerId, int timeoutSeconds, DockerClient dockerClient) {
        super(containerId, DockerActionType.UNPAUSE_CONTAINER, timeoutSeconds, dockerClient);
    }

    @Override
    public boolean canPerform() {
        if (!super.canPerform()) {
            return false;
        }
        try {
            Container container = DockerUtils.findContainer(dockerClient, containerId);
            if (container == null)
                throw new DockerActionException("Container not found: " + containerId, containerId, actionType);
            // Container must be paused to be unpaused
            return DockerUtils.isContainerPaused(container);
        } catch (Exception e) {
            logger.warn("Cannot perform unpause action: {}", e.getMessage());
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
            // Check if container is already running (not paused)
            if (DockerUtils.isContainerRunning(container)) {
                logger.info("Container {} is already running", container.getId());
                return AdaptationActionResult.SKIPPED;
            }

            // Check if container is paused
            if (!DockerUtils.isContainerPaused(container)) {
                logger.warn("Container {} is not paused, cannot unpause", container.getId());
                return AdaptationActionResult.SKIPPED;
            }

            // Unpause the container
            dockerClient.unpauseContainerCmd(container.getId()).exec();
            wasUnpaused = true;

            // Verify container is running
            Thread.sleep(500); // Brief wait for state change
            if (!DockerUtils.isContainerRunning(dockerClient, container.getId())) {
                throw new DockerActionException(
                        "Container state did not change to running after unpause",
                        containerId,
                        DockerActionType.UNPAUSE_CONTAINER
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
                    "Failed to unpause container: " + e.getMessage(),
                    e,
                    containerId,
                    DockerActionType.UNPAUSE_CONTAINER
            );
        }
    }

    @Override
    public AdaptationActionResult rollback() {
        if (!wasUnpaused) {
            logger.info("No rollback needed - container was not unpaused by this action");
            return AdaptationActionResult.SKIPPED;
        }

        logger.info("Rolling back unpause action - pausing container: {}", containerId);

        try {
            Container container = DockerUtils.findContainer(dockerClient, containerId);
            if (container == null)
                throw new DockerActionException("Container not found: " + containerId, containerId, actionType);

            dockerClient.pauseContainerCmd(containerId).exec();


            // Verify container is paused
            Thread.sleep(500);
            if (!DockerUtils.isContainerPaused(dockerClient, container.getId())) {
                logger.warn("Container did not return to paused state during rollback");
                return AdaptationActionResult.ROLLBACK_FAILED;
            }

            wasUnpaused = false;
            logger.info("Successfully rolled back unpause action for container: {}", containerId);
            return AdaptationActionResult.ROLLED_BACK;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Rollback interrupted for container: {}", containerId);
            return AdaptationActionResult.ROLLBACK_FAILED;
        } catch (Exception e) {
            logger.error("Failed to rollback unpause action: {}", e.getMessage(), e);
            return AdaptationActionResult.ROLLBACK_FAILED;
        }
    }
}
