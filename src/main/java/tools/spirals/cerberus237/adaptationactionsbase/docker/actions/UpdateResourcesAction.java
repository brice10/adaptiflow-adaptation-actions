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
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.HostConfig;
import tools.spirals.cerberus237.adaptationactionsbase.docker.AbstractDockerAction;
import tools.spirals.cerberus237.adaptationactionsbase.enums.AdaptationActionResult;
import tools.spirals.cerberus237.adaptationactionsbase.enums.DockerActionType;
import tools.spirals.cerberus237.adaptationactionsbase.exceptions.DockerActionException;

/**
 * Adaptation action that updates a container's resource limits (CPU, memory).
 * <p>
 * This action allows dynamic adjustment of container resource constraints without
 * stopping the container. Supported resource updates include:
 * <ul>
 *     <li>Memory limit</li>
 *     <li>Memory swap limit</li>
 *     <li>CPU shares (relative weight)</li>
 *     <li>CPU period and quota (for CFS scheduler)</li>
 *     <li>CPU set (pinning to specific CPUs)</li>
 * </ul>
 * </p>
 *
 * @author Arléon Zemtsop (Cerberus)
 */
public class UpdateResourcesAction extends AbstractDockerAction {

    private Long memoryLimit;
    private Long memorySwapLimit;
    private Integer cpuShares;
    private Long cpuPeriod;
    private Long cpuQuota;
    private String cpusetCpus;

    // Store previous values for rollback
    private Long previousMemoryLimit;
    private Long previousMemorySwapLimit;
    private Integer previousCpuShares;
    private Long previousCpuPeriod;
    private Long previousCpuQuota;
    private String previousCpusetCpus;
    private boolean wasUpdated = false;

    /**
     * Constructs a new UpdateResourcesAction for the specified container.
     *
     * @param containerId the ID or name of the container to update
     */
    public UpdateResourcesAction(String containerId) {
        super(containerId, DockerActionType.UPDATE_RESOURCES);
    }

    /**
     * Constructs a new UpdateResourcesAction with custom timeout.
     *
     * @param containerId    the ID or name of the container to update
     * @param timeoutSeconds the timeout in seconds
     */
    public UpdateResourcesAction(String containerId, int timeoutSeconds) {
        super(containerId, DockerActionType.UPDATE_RESOURCES, timeoutSeconds);
    }

    /**
     * Constructs a new UpdateResourcesAction with a custom Docker client.
     *
     * @param containerId    the ID or name of the container to update
     * @param timeoutSeconds the timeout in seconds
     * @param dockerClient   the Docker client to use
     */
    public UpdateResourcesAction(String containerId, int timeoutSeconds, DockerClient dockerClient) {
        super(containerId, DockerActionType.UPDATE_RESOURCES, timeoutSeconds, dockerClient);
    }

    /**
     * Sets the memory limit (in bytes).
     *
     * @param memoryLimit the memory limit in bytes
     * @return this action for method chaining
     */
    public UpdateResourcesAction withMemoryLimit(Long memoryLimit) {
        this.memoryLimit = memoryLimit;
        return this;
    }

    /**
     * Sets the memory limit using human-readable format.
     *
     * @param memoryMB the memory limit in megabytes
     * @return this action for method chaining
     */
    public UpdateResourcesAction withMemoryLimitMB(int memoryMB) {
        this.memoryLimit = (long) memoryMB * 1024 * 1024;
        return this;
    }

    /**
     * Sets the memory swap limit (in bytes).
     *
     * @param memorySwapLimit the memory swap limit in bytes
     * @return this action for method chaining
     */
    public UpdateResourcesAction withMemorySwapLimit(Long memorySwapLimit) {
        this.memorySwapLimit = memorySwapLimit;
        return this;
    }

    /**
     * Sets the CPU shares (relative weight).
     *
     * @param cpuShares the CPU shares (default is 1024)
     * @return this action for method chaining
     */
    public UpdateResourcesAction withCpuShares(Integer cpuShares) {
        this.cpuShares = cpuShares;
        return this;
    }

    /**
     * Sets the CPU period for the CFS scheduler (in microseconds).
     *
     * @param cpuPeriod the CPU period in microseconds
     * @return this action for method chaining
     */
    public UpdateResourcesAction withCpuPeriod(Long cpuPeriod) {
        this.cpuPeriod = cpuPeriod;
        return this;
    }

    /**
     * Sets the CPU quota for the CFS scheduler (in microseconds).
     *
     * @param cpuQuota the CPU quota in microseconds
     * @return this action for method chaining
     */
    public UpdateResourcesAction withCpuQuota(Long cpuQuota) {
        this.cpuQuota = cpuQuota;
        return this;
    }

    /**
     * Sets the CPUs to which the container is pinned.
     *
     * @param cpusetCpus the CPU set (e.g., "0-3" or "0,1,2")
     * @return this action for method chaining
     */
    public UpdateResourcesAction withCpusetCpus(String cpusetCpus) {
        this.cpusetCpus = cpusetCpus;
        return this;
    }

    @Override
    public boolean canPerform() {
        if (!super.canPerform()) {
            return false;
        }
        try {
            // Container must exist
            findContainer(containerId);
            // At least one resource update must be specified
            return memoryLimit != null || memorySwapLimit != null || 
                   cpuShares != null || cpuPeriod != null || 
                   cpuQuota != null || cpusetCpus != null;
        } catch (Exception e) {
            logger.warn("Cannot perform update resources action: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public AdaptationActionResult perform() {
        logActionStart();
        logger.info("Updating resources for container: {}", containerId);

        try {
            // Store current values for potential rollback
            findContainer(containerId);
            InspectContainerResponse inspection = dockerClient.inspectContainerCmd(containerId).exec();
            HostConfig currentConfig = inspection.getHostConfig();

            previousMemoryLimit = currentConfig.getMemory();
            previousMemorySwapLimit = currentConfig.getMemorySwap();
            previousCpuShares = currentConfig.getCpuShares();
            previousCpuPeriod = currentConfig.getCpuPeriod();
            previousCpuQuota = currentConfig.getCpuQuota();
            previousCpusetCpus = currentConfig.getCpusetCpus();

            // Build and execute the update command
            var updateCmd = dockerClient.updateContainerCmd(containerId);

            if (memoryLimit != null) {
                updateCmd.withMemory(memoryLimit);
                logger.debug("Setting memory limit: {} bytes", memoryLimit);
            }
            if (memorySwapLimit != null) {
                updateCmd.withMemorySwap(memorySwapLimit);
                logger.debug("Setting memory swap limit: {} bytes", memorySwapLimit);
            }
            if (cpuShares != null) {
                updateCmd.withCpuShares(cpuShares);
                logger.debug("Setting CPU shares: {}", cpuShares);
            }
            if (cpuPeriod != null) {
                updateCmd.withCpuPeriod(cpuPeriod.intValue());
                logger.debug("Setting CPU period: {} microseconds", cpuPeriod);
            }
            if (cpuQuota != null) {
                updateCmd.withCpuQuota(cpuQuota.intValue());
                logger.debug("Setting CPU quota: {} microseconds", cpuQuota);
            }
            if (cpusetCpus != null) {
                updateCmd.withCpusetCpus(cpusetCpus);
                logger.debug("Setting CPU set: {}", cpusetCpus);
            }

            updateCmd.exec();
            wasUpdated = true;

            logActionSuccess();
            return AdaptationActionResult.SUCCESS;

        } catch (DockerActionException e) {
            logActionFailure(e);
            throw e;
        } catch (Exception e) {
            logActionFailure(e);
            throw new DockerActionException(
                    "Failed to update container resources: " + e.getMessage(),
                    e,
                    containerId,
                    DockerActionType.UPDATE_RESOURCES
            );
        }
    }

    @Override
    public boolean supportsRollback() {
        return true;
    }

    @Override
    public AdaptationActionResult rollback() {
        if (!wasUpdated) {
            logger.info("No rollback needed - resources were not updated");
            return AdaptationActionResult.SKIPPED;
        }

        logger.info("Rolling back resource updates for container: {}", containerId);

        try {
            var updateCmd = dockerClient.updateContainerCmd(containerId);

            if (previousMemoryLimit != null) {
                updateCmd.withMemory(previousMemoryLimit);
            }
            if (previousMemorySwapLimit != null) {
                updateCmd.withMemorySwap(previousMemorySwapLimit);
            }
            if (previousCpuShares != null) {
                updateCmd.withCpuShares(previousCpuShares);
            }
            if (previousCpuPeriod != null) {
                updateCmd.withCpuPeriod(previousCpuPeriod.intValue());
            }
            if (previousCpuQuota != null) {
                updateCmd.withCpuQuota(previousCpuQuota.intValue());
            }
            if (previousCpusetCpus != null) {
                updateCmd.withCpusetCpus(previousCpusetCpus);
            }

            updateCmd.exec();
            wasUpdated = false;

            logger.info("Successfully rolled back resource updates for container: {}", containerId);
            return AdaptationActionResult.ROLLED_BACK;

        } catch (Exception e) {
            logger.error("Failed to rollback resource updates: {}", e.getMessage(), e);
            return AdaptationActionResult.ROLLBACK_FAILED;
        }
    }

    /**
     * Returns the memory limit setting.
     *
     * @return the memory limit in bytes, or null if not set
     */
    public Long getMemoryLimit() {
        return memoryLimit;
    }

    /**
     * Returns the CPU shares setting.
     *
     * @return the CPU shares, or null if not set
     */
    public Integer getCpuShares() {
        return cpuShares;
    }
}
