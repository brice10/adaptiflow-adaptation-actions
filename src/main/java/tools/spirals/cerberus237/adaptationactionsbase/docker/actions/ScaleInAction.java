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
import tools.spirals.cerberus237.adaptationactionsbase.docker.AbstractDockerAction;
import tools.spirals.cerberus237.adaptationactionsbase.enums.AdaptationActionResult;
import tools.spirals.cerberus237.adaptationactionsbase.enums.DockerActionType;
import tools.spirals.cerberus237.adaptationactionsbase.exceptions.DockerActionException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Adaptation action that scales in by removing container replicas.
 * <p>
 * This action removes containers that match a specified pattern or image.
 * It implements a "youngest first" strategy by default, removing the most
 * recently created containers first.
 * </p>
 *
 * @author Arléon Zemtsop (Cerberus)
 */
public class ScaleInAction extends AbstractDockerAction {

    private final int removeCount;
    private final String containerNamePattern;
    private final String imageName;
    private final boolean gracefulStop;
    private final List<String> removedContainerIds = new ArrayList<>();

    /**
     * Constructs a new ScaleInAction to remove one container matching the pattern.
     *
     * @param containerNamePattern the pattern to match container names (supports wildcards)
     */
    public ScaleInAction(String containerNamePattern) {
        this(containerNamePattern, 1);
    }

    /**
     * Constructs a new ScaleInAction with specified removal count.
     *
     * @param containerNamePattern the pattern to match container names
     * @param removeCount          the number of containers to remove
     */
    public ScaleInAction(String containerNamePattern, int removeCount) {
        this(containerNamePattern, removeCount, null, true);
    }

    /**
     * Constructs a new ScaleInAction with all options.
     *
     * @param containerNamePattern the pattern to match container names
     * @param removeCount          the number of containers to remove
     * @param imageName            the image name to filter by (optional)
     * @param gracefulStop         if true, stop containers gracefully before removal
     */
    public ScaleInAction(String containerNamePattern, int removeCount, String imageName, boolean gracefulStop) {
        super(containerNamePattern, DockerActionType.SCALE_IN);
        this.containerNamePattern = containerNamePattern;
        this.removeCount = Math.max(1, removeCount);
        this.imageName = imageName;
        this.gracefulStop = gracefulStop;
    }

    /**
     * Constructs a new ScaleInAction with custom timeout.
     *
     * @param containerNamePattern the pattern to match container names
     * @param removeCount          the number of containers to remove
     * @param imageName            the image name to filter by (optional)
     * @param gracefulStop         if true, stop containers gracefully before removal
     * @param timeoutSeconds       the timeout in seconds
     */
    public ScaleInAction(String containerNamePattern, int removeCount, String imageName,
                         boolean gracefulStop, int timeoutSeconds) {
        super(containerNamePattern, DockerActionType.SCALE_IN, timeoutSeconds);
        this.containerNamePattern = containerNamePattern;
        this.removeCount = Math.max(1, removeCount);
        this.imageName = imageName;
        this.gracefulStop = gracefulStop;
    }

    /**
     * Constructs a new ScaleInAction with a custom Docker client.
     *
     * @param containerNamePattern the pattern to match container names
     * @param removeCount          the number of containers to remove
     * @param imageName            the image name to filter by (optional)
     * @param gracefulStop         if true, stop containers gracefully before removal
     * @param timeoutSeconds       the timeout in seconds
     * @param dockerClient         the Docker client to use
     */
    public ScaleInAction(String containerNamePattern, int removeCount, String imageName,
                         boolean gracefulStop, int timeoutSeconds, DockerClient dockerClient) {
        super(containerNamePattern, DockerActionType.SCALE_IN, timeoutSeconds, dockerClient);
        this.containerNamePattern = containerNamePattern;
        this.removeCount = Math.max(1, removeCount);
        this.imageName = imageName;
        this.gracefulStop = gracefulStop;
    }

    @Override
    public boolean canPerform() {
        if (!super.canPerform()) {
            return false;
        }
        try {
            // At least one matching container must exist
            List<Container> matchingContainers = findMatchingContainers();
            return !matchingContainers.isEmpty();
        } catch (Exception e) {
            logger.warn("Cannot perform scale in action: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public AdaptationActionResult perform() {
        logActionStart();
        logger.info("Scaling in - removing up to {} container(s) matching pattern: {}", removeCount, containerNamePattern);

        try {
            List<Container> matchingContainers = findMatchingContainers();

            if (matchingContainers.isEmpty()) {
                logger.info("No containers found matching pattern: {}", containerNamePattern);
                return AdaptationActionResult.SKIPPED;
            }

            // Sort by creation time (newest first) - "youngest first" strategy
            matchingContainers.sort(Comparator.comparingLong(Container::getCreated).reversed());

            int actualRemoveCount = Math.min(removeCount, matchingContainers.size());
            int successCount = 0;

            for (int i = 0; i < actualRemoveCount; i++) {
                Container container = matchingContainers.get(i);
                String targetContainerId = container.getId();

                try {
                    // Stop the container if running and graceful stop is enabled
                    if (gracefulStop && "running".equalsIgnoreCase(container.getState())) {
                        dockerClient.stopContainerCmd(targetContainerId)
                                .withTimeout(timeoutSeconds)
                                .exec();
                        logger.debug("Stopped container: {}", targetContainerId);
                    }

                    // Remove the container
                    dockerClient.removeContainerCmd(targetContainerId)
                            .withForce(!gracefulStop)
                            .exec();

                    removedContainerIds.add(targetContainerId);
                    logger.info("Removed container: {} ({})", getContainerName(container), targetContainerId);
                    successCount++;

                } catch (Exception e) {
                    logger.error("Failed to remove container {}: {}", targetContainerId, e.getMessage());
                }
            }

            if (successCount == 0) {
                throw new DockerActionException(
                        "Failed to remove any containers",
                        containerNamePattern,
                        DockerActionType.SCALE_IN
                );
            }

            if (successCount < actualRemoveCount) {
                logger.warn("Only {} out of {} containers were removed successfully", successCount, actualRemoveCount);
            }

            logActionSuccess();
            return AdaptationActionResult.SUCCESS;

        } catch (DockerActionException e) {
            logActionFailure(e);
            throw e;
        } catch (Exception e) {
            logActionFailure(e);
            throw new DockerActionException(
                    "Failed to scale in: " + e.getMessage(),
                    e,
                    containerNamePattern,
                    DockerActionType.SCALE_IN
            );
        }
    }

    /**
     * Finds containers matching the name pattern and optional image filter.
     *
     * @return the list of matching containers
     */
    private List<Container> findMatchingContainers() {
        List<Container> allContainers = dockerClient.listContainersCmd()
                .withShowAll(true)
                .exec();

        List<Container> matching = new ArrayList<>();
        for (Container container : allContainers) {
            if (matchesPattern(container)) {
                matching.add(container);
            }
        }
        return matching;
    }

    /**
     * Checks if a container matches the pattern and optional image filter.
     *
     * @param container the container to check
     * @return true if the container matches
     */
    private boolean matchesPattern(Container container) {
        // Check name pattern
        boolean nameMatches = false;
        for (String name : container.getNames()) {
            String normalizedName = name.startsWith("/") ? name.substring(1) : name;
            if (matchesWildcard(normalizedName, containerNamePattern)) {
                nameMatches = true;
                break;
            }
        }

        if (!nameMatches) {
            return false;
        }

        // Check image filter if specified
        if (imageName != null && !imageName.isEmpty()) {
            return container.getImage().equals(imageName) || 
                   container.getImage().startsWith(imageName + ":");
        }

        return true;
    }

    /**
     * Performs wildcard matching (* matches any sequence of characters).
     *
     * @param text    the text to match
     * @param pattern the pattern with optional wildcards
     * @return true if the text matches the pattern
     */
    private boolean matchesWildcard(String text, String pattern) {
        // Convert wildcard pattern to regex
        String regex = pattern
                .replace(".", "\\.")
                .replace("*", ".*")
                .replace("?", ".");
        return text.matches(regex);
    }

    /**
     * Gets the first name of a container.
     *
     * @param container the container
     * @return the container name
     */
    private String getContainerName(Container container) {
        String[] names = container.getNames();
        if (names != null && names.length > 0) {
            String name = names[0];
            return name.startsWith("/") ? name.substring(1) : name;
        }
        return container.getId().substring(0, 12);
    }

    /**
     * Returns the number of containers to remove.
     *
     * @return the removal count
     */
    public int getRemoveCount() {
        return removeCount;
    }

    /**
     * Returns the container name pattern.
     *
     * @return the name pattern
     */
    public String getContainerNamePattern() {
        return containerNamePattern;
    }

    /**
     * Returns whether graceful stop is enabled.
     *
     * @return true if graceful stop is enabled
     */
    public boolean isGracefulStop() {
        return gracefulStop;
    }

    /**
     * Returns the list of removed container IDs.
     *
     * @return the list of removed container IDs (unmodifiable copy)
     */
    public List<String> getRemovedContainerIds() {
        return new ArrayList<>(removedContainerIds);
    }
}
