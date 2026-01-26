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
 * Adaptation action that forcefully kills a Docker container.
 * <p>
 * This action sends a SIGKILL signal (or a specified signal) to the main process
 * in the container. Unlike stop, kill does not wait for graceful shutdown.
 * </p>
 *
 * @author Arléon Zemtsop (Cerberus)
 */
public class KillContainerAction extends AbstractDockerAction {

    private final String signal;
    private boolean wasKilled = false;

    private static final String DEFAULT_SIGNAL = "SIGKILL";

    /**
     * Constructs a new KillContainerAction for the specified container.
     *
     * @param containerId the ID or name of the container to kill
     */
    public KillContainerAction(String containerId) {
        this(containerId, DEFAULT_SIGNAL);
    }

    /**
     * Constructs a new KillContainerAction with a specific signal.
     *
     * @param containerId the ID or name of the container to kill
     * @param signal      the signal to send (e.g., "SIGKILL", "SIGTERM", "SIGINT")
     */
    public KillContainerAction(String containerId, String signal) {
        super(containerId, DockerActionType.KILL_CONTAINER);
        this.signal = signal != null ? signal : DEFAULT_SIGNAL;
    }

    /**
     * Constructs a new KillContainerAction with a custom timeout.
     *
     * @param containerId    the ID or name of the container to kill
     * @param signal         the signal to send
     * @param timeoutSeconds the timeout in seconds
     */
    public KillContainerAction(String containerId, String signal, int timeoutSeconds) {
        super(containerId, DockerActionType.KILL_CONTAINER, timeoutSeconds);
        this.signal = signal != null ? signal : DEFAULT_SIGNAL;
    }

    /**
     * Constructs a new KillContainerAction with a custom Docker client.
     *
     * @param containerId    the ID or name of the container to kill
     * @param signal         the signal to send
     * @param timeoutSeconds the timeout in seconds
     * @param dockerClient   the Docker client to use
     */
    public KillContainerAction(String containerId, String signal, int timeoutSeconds, DockerClient dockerClient) {
        super(containerId, DockerActionType.KILL_CONTAINER, timeoutSeconds, dockerClient);
        this.signal = signal != null ? signal : DEFAULT_SIGNAL;
    }

    @Override
    public boolean canPerform() {
        if (!super.canPerform()) {
            return false;
        }
        try {
            // Container must be running or paused to be killed
            return isContainerRunning(containerId) || isContainerPaused(containerId);
        } catch (Exception e) {
            logger.warn("Cannot perform kill action: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public AdaptationActionResult perform() {
        logActionStart();
        logger.info("Killing container {} with signal {}", containerId, signal);

        try {
            // Check if container is already stopped
            if (isContainerStopped(containerId)) {
                logger.info("Container {} is already stopped", containerId);
                return AdaptationActionResult.SKIPPED;
            }

            // Find and kill the container
            findContainer(containerId);
            dockerClient.killContainerCmd(containerId)
                    .withSignal(signal)
                    .exec();
            wasKilled = true;

            // Wait briefly for container to be killed
            Thread.sleep(1000);
            
            // Verify container is stopped
            if (isContainerRunning(containerId)) {
                throw new DockerActionException(
                        "Container still running after kill signal",
                        containerId,
                        DockerActionType.KILL_CONTAINER
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
                    "Failed to kill container: " + e.getMessage(),
                    e,
                    containerId,
                    DockerActionType.KILL_CONTAINER
            );
        }
    }

    @Override
    public boolean supportsRollback() {
        return true;
    }

    @Override
    public AdaptationActionResult rollback() {
        if (!wasKilled) {
            logger.info("No rollback needed - container was not killed by this action");
            return AdaptationActionResult.SKIPPED;
        }

        logger.info("Rolling back kill action - starting container: {}", containerId);

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

            wasKilled = false;
            logger.info("Successfully rolled back kill action for container: {}", containerId);
            return AdaptationActionResult.ROLLED_BACK;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Rollback interrupted for container: {}", containerId);
            return AdaptationActionResult.ROLLBACK_FAILED;
        } catch (Exception e) {
            logger.error("Failed to rollback kill action: {}", e.getMessage(), e);
            return AdaptationActionResult.ROLLBACK_FAILED;
        }
    }

    /**
     * Returns the signal used by this kill action.
     *
     * @return the signal name
     */
    public String getSignal() {
        return signal;
    }
}
