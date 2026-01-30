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
 * Adaptation action that removes a Docker container.
 * <p>
 * This action removes a container from the Docker host. By default, it will
 * only remove stopped containers. The force option can be used to remove
 * running containers.
 * </p>
 * <p>
 * Note: This action does NOT support rollback as removal is permanent.
 * </p>
 *
 * @author Arléon Zemtsop (Cerberus)
 */
public class RemoveContainerAction extends AbstractDockerAction {

    private final boolean force;
    private final boolean removeVolumes;

    /**
     * Constructs a new RemoveContainerAction for the specified container.
     *
     * @param containerId the ID or name of the container to remove
     */
    public RemoveContainerAction(String containerId) {
        this(containerId, false, false);
    }

    /**
     * Constructs a new RemoveContainerAction with force option.
     *
     * @param containerId the ID or name of the container to remove
     * @param force       if true, force removal of running container
     */
    public RemoveContainerAction(String containerId, boolean force) {
        this(containerId, force, false);
    }

    /**
     * Constructs a new RemoveContainerAction with options.
     *
     * @param containerId   the ID or name of the container to remove
     * @param force         if true, force removal of running container
     * @param removeVolumes if true, remove associated volumes
     */
    public RemoveContainerAction(String containerId, boolean force, boolean removeVolumes) {
        super(containerId, DockerActionType.REMOVE_CONTAINER);
        this.force = force;
        this.removeVolumes = removeVolumes;
    }

    /**
     * Constructs a new RemoveContainerAction with all options.
     *
     * @param containerId    the ID or name of the container to remove
     * @param force          if true, force removal of running container
     * @param removeVolumes  if true, remove associated volumes
     * @param timeoutSeconds the timeout in seconds
     */
    public RemoveContainerAction(String containerId, boolean force, boolean removeVolumes, int timeoutSeconds) {
        super(containerId, DockerActionType.REMOVE_CONTAINER, timeoutSeconds);
        this.force = force;
        this.removeVolumes = removeVolumes;
    }

    /**
     * Constructs a new RemoveContainerAction with a custom Docker client.
     *
     * @param containerId    the ID or name of the container to remove
     * @param force          if true, force removal of running container
     * @param removeVolumes  if true, remove associated volumes
     * @param timeoutSeconds the timeout in seconds
     * @param dockerClient   the Docker client to use
     */
    public RemoveContainerAction(String containerId, boolean force, boolean removeVolumes,
                                 int timeoutSeconds, DockerClient dockerClient) {
        super(containerId, DockerActionType.REMOVE_CONTAINER, timeoutSeconds, dockerClient);
        this.force = force;
        this.removeVolumes = removeVolumes;
    }

    @Override
    public boolean canPerform() {
        if (!super.canPerform()) {
            return false;
        }
        try {
            // Container must exist
            findContainer(containerId);
            // If not forcing, container must be stopped
            if (!force && (isContainerRunning(containerId) || isContainerPaused(containerId))) {
                logger.warn("Container {} is running and force is not enabled", containerId);
                return false;
            }
            return true;
        } catch (Exception e) {
            logger.warn("Cannot perform remove action: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public AdaptationActionResult perform() {
        logActionStart();
        logger.info("Removing container {} (force={}, removeVolumes={})", containerId, force, removeVolumes);

        try {
            // Find the container
            findContainer(containerId);

            // Remove the container
            dockerClient.removeContainerCmd(containerId)
                    .withForce(force)
                    .withRemoveVolumes(removeVolumes)
                    .exec();

            // Verify container is removed
            try {
                findContainer(containerId);
                // If we get here, container still exists
                throw new DockerActionException(
                        "Container still exists after removal",
                        containerId,
                        DockerActionType.REMOVE_CONTAINER
                );
            } catch (DockerActionException e) {
                // Expected - container not found means it was successfully removed
                if (e.getMessage().contains("not found")) {
                    logActionSuccess();
                    return AdaptationActionResult.SUCCESS;
                }
                throw e;
            }

        } catch (DockerActionException e) {
            logActionFailure(e);
            throw e;
        } catch (Exception e) {
            logActionFailure(e);
            throw new DockerActionException(
                    "Failed to remove container: " + e.getMessage(),
                    e,
                    containerId,
                    DockerActionType.REMOVE_CONTAINER
            );
        }
    }

    /**
     * Returns whether force removal is enabled.
     *
     * @return true if force removal is enabled
     */
    public boolean isForce() {
        return force;
    }

    /**
     * Returns whether volume removal is enabled.
     *
     * @return true if volumes will be removed
     */
    public boolean isRemoveVolumes() {
        return removeVolumes;
    }
}
