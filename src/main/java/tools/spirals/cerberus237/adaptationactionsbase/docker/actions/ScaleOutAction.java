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
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.HostConfig;
import tools.spirals.cerberus237.adaptationactionsbase.docker.AbstractDockerAction;
import tools.spirals.cerberus237.adaptationactionsbase.enums.AdaptationActionResult;
import tools.spirals.cerberus237.adaptationactionsbase.enums.DockerActionType;
import tools.spirals.cerberus237.adaptationactionsbase.exceptions.DockerActionException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Adaptation action that scales out by creating additional container replicas.
 * <p>
 * This action creates new containers based on the configuration of an existing
 * source container. The new containers are created with unique names and can
 * optionally be started automatically.
 * </p>
 *
 * @author Arléon Zemtsop (Cerberus)
 */
public class ScaleOutAction extends AbstractDockerAction {

    private final int replicaCount;
    private final boolean autoStart;
    private final String containerNamePrefix;
    private final List<String> createdContainerIds = new ArrayList<>();

    /**
     * Constructs a new ScaleOutAction to create one replica.
     *
     * @param sourceContainerId the ID or name of the container to replicate
     */
    public ScaleOutAction(String sourceContainerId) {
        this(sourceContainerId, 1);
    }

    /**
     * Constructs a new ScaleOutAction with specified replica count.
     *
     * @param sourceContainerId the ID or name of the container to replicate
     * @param replicaCount      the number of replicas to create
     */
    public ScaleOutAction(String sourceContainerId, int replicaCount) {
        this(sourceContainerId, replicaCount, true);
    }

    /**
     * Constructs a new ScaleOutAction with options.
     *
     * @param sourceContainerId the ID or name of the container to replicate
     * @param replicaCount      the number of replicas to create
     * @param autoStart         if true, start the replicas after creation
     */
    public ScaleOutAction(String sourceContainerId, int replicaCount, boolean autoStart) {
        this(sourceContainerId, replicaCount, autoStart, "replica");
    }

    /**
     * Constructs a new ScaleOutAction with all options.
     *
     * @param sourceContainerId   the ID or name of the container to replicate
     * @param replicaCount        the number of replicas to create
     * @param autoStart           if true, start the replicas after creation
     * @param containerNamePrefix prefix for the new container names
     */
    public ScaleOutAction(String sourceContainerId, int replicaCount, boolean autoStart, String containerNamePrefix) {
        super(sourceContainerId, DockerActionType.SCALE_OUT);
        this.replicaCount = Math.max(1, replicaCount);
        this.autoStart = autoStart;
        this.containerNamePrefix = containerNamePrefix != null ? containerNamePrefix : "replica";
    }

    /**
     * Constructs a new ScaleOutAction with custom timeout.
     *
     * @param sourceContainerId   the ID or name of the container to replicate
     * @param replicaCount        the number of replicas to create
     * @param autoStart           if true, start the replicas after creation
     * @param containerNamePrefix prefix for the new container names
     * @param timeoutSeconds      the timeout in seconds
     */
    public ScaleOutAction(String sourceContainerId, int replicaCount, boolean autoStart,
                          String containerNamePrefix, int timeoutSeconds) {
        super(sourceContainerId, DockerActionType.SCALE_OUT, timeoutSeconds);
        this.replicaCount = Math.max(1, replicaCount);
        this.autoStart = autoStart;
        this.containerNamePrefix = containerNamePrefix != null ? containerNamePrefix : "replica";
    }

    /**
     * Constructs a new ScaleOutAction with a custom Docker client.
     *
     * @param sourceContainerId   the ID or name of the container to replicate
     * @param replicaCount        the number of replicas to create
     * @param autoStart           if true, start the replicas after creation
     * @param containerNamePrefix prefix for the new container names
     * @param timeoutSeconds      the timeout in seconds
     * @param dockerClient        the Docker client to use
     */
    public ScaleOutAction(String sourceContainerId, int replicaCount, boolean autoStart,
                          String containerNamePrefix, int timeoutSeconds, DockerClient dockerClient) {
        super(sourceContainerId, DockerActionType.SCALE_OUT, timeoutSeconds, dockerClient);
        this.replicaCount = Math.max(1, replicaCount);
        this.autoStart = autoStart;
        this.containerNamePrefix = containerNamePrefix != null ? containerNamePrefix : "replica";
    }

    @Override
    public boolean canPerform() {
        if (!super.canPerform()) {
            return false;
        }
        try {
            // Source container must exist
            findContainer(containerId);
            return true;
        } catch (Exception e) {
            logger.warn("Cannot perform scale out action: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public AdaptationActionResult perform() {
        logActionStart();
        logger.info("Scaling out from container {} - creating {} replica(s)", containerId, replicaCount);

        try {
            // Get the source container's configuration
            findContainer(containerId);
            InspectContainerResponse sourceContainer = dockerClient.inspectContainerCmd(containerId).exec();
            String imageName = sourceContainer.getConfig().getImage();
            HostConfig sourceHostConfig = sourceContainer.getHostConfig();

            int successCount = 0;
            for (int i = 0; i < replicaCount; i++) {
                try {
                    String replicaName = generateReplicaName(i);
                    
                    // Create the replica container
                    CreateContainerResponse response = dockerClient.createContainerCmd(imageName)
                            .withName(replicaName)
                            .withEnv(sourceContainer.getConfig().getEnv())
                            .withHostConfig(sourceHostConfig)
                            .exec();

                    String newContainerId = response.getId();
                    createdContainerIds.add(newContainerId);
                    logger.info("Created replica container: {} ({})", replicaName, newContainerId);

                    // Start the container if autoStart is enabled
                    if (autoStart) {
                        dockerClient.startContainerCmd(newContainerId).exec();
                        logger.info("Started replica container: {}", replicaName);
                    }

                    successCount++;

                } catch (Exception e) {
                    logger.error("Failed to create replica {}: {}", i, e.getMessage());
                }
            }

            if (successCount == 0) {
                throw new DockerActionException(
                        "Failed to create any replicas",
                        containerId,
                        DockerActionType.SCALE_OUT
                );
            }

            if (successCount < replicaCount) {
                logger.warn("Only {} out of {} replicas were created successfully", successCount, replicaCount);
            }

            logActionSuccess();
            return AdaptationActionResult.SUCCESS;

        } catch (DockerActionException e) {
            logActionFailure(e);
            throw e;
        } catch (Exception e) {
            logActionFailure(e);
            throw new DockerActionException(
                    "Failed to scale out: " + e.getMessage(),
                    e,
                    containerId,
                    DockerActionType.SCALE_OUT
            );
        }
    }

    @Override
    public boolean supportsRollback() {
        return true;
    }

    @Override
    public AdaptationActionResult rollback() {
        if (createdContainerIds.isEmpty()) {
            logger.info("No rollback needed - no containers were created");
            return AdaptationActionResult.SKIPPED;
        }

        logger.info("Rolling back scale out action - removing {} created container(s)", createdContainerIds.size());

        int removeCount = 0;
        for (String createdId : createdContainerIds) {
            try {
                // Stop if running
                try {
                    dockerClient.stopContainerCmd(createdId).withTimeout(5).exec();
                } catch (Exception e) {
                    // Container might already be stopped
                }

                // Remove the container
                dockerClient.removeContainerCmd(createdId).withForce(true).exec();
                logger.info("Removed replica container: {}", createdId);
                removeCount++;
            } catch (Exception e) {
                logger.error("Failed to remove replica container {}: {}", createdId, e.getMessage());
            }
        }

        createdContainerIds.clear();

        if (removeCount > 0) {
            logger.info("Successfully rolled back scale out action - removed {} container(s)", removeCount);
            return AdaptationActionResult.ROLLED_BACK;
        } else {
            return AdaptationActionResult.ROLLBACK_FAILED;
        }
    }

    /**
     * Generates a unique name for a replica container.
     *
     * @param index the replica index
     * @return the generated container name
     */
    private String generateReplicaName(int index) {
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        return String.format("%s-%s-%d-%s", containerNamePrefix, containerId.substring(0, 8), index, uniqueId);
    }

    /**
     * Returns the number of replicas to create.
     *
     * @return the replica count
     */
    public int getReplicaCount() {
        return replicaCount;
    }

    /**
     * Returns whether auto-start is enabled.
     *
     * @return true if replicas are started automatically
     */
    public boolean isAutoStart() {
        return autoStart;
    }

    /**
     * Returns the list of created container IDs.
     *
     * @return the list of created container IDs (unmodifiable copy)
     */
    public List<String> getCreatedContainerIds() {
        return new ArrayList<>(createdContainerIds);
    }
}
