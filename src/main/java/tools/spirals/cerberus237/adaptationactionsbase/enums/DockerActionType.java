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
package tools.spirals.cerberus237.adaptationactionsbase.enums;

/**
 * Enumeration representing the types of Docker adaptation actions available.
 *
 * @author Arléon Zemtsop (Cerberus)
 */
public enum DockerActionType {

    /**
     * Start a stopped container.
     */
    START_CONTAINER("Start a Docker container"),

    /**
     * Stop a running container.
     */
    STOP_CONTAINER("Stop a Docker container"),

    /**
     * Restart a container (stop then start).
     */
    RESTART_CONTAINER("Restart a Docker container"),

    /**
     * Pause a running container.
     */
    PAUSE_CONTAINER("Pause a Docker container"),

    /**
     * Unpause a paused container.
     */
    UNPAUSE_CONTAINER("Unpause a Docker container"),

    /**
     * Kill a container (forceful stop).
     */
    KILL_CONTAINER("Kill a Docker container"),

    /**
     * Remove a container.
     */
    REMOVE_CONTAINER("Remove a Docker container"),

    /**
     * Create a new container from an image.
     */
    CREATE_CONTAINER("Create a new Docker container"),

    /**
     * Scale out by creating additional container replicas.
     */
    SCALE_OUT("Scale out by adding container replicas"),

    /**
     * Scale in by removing container replicas.
     */
    SCALE_IN("Scale in by removing container replicas"),

    /**
     * Update container resources (CPU, memory limits).
     */
    UPDATE_RESOURCES("Update container resource limits"),

    /**
     * Pull a Docker image from a registry.
     */
    PULL_IMAGE("Pull a Docker image"),

    /**
     * Execute a command inside a running container.
     */
    EXEC_COMMAND("Execute command in container"),

    /**
     * Rename a container.
     */
    RENAME_CONTAINER("Rename a Docker container"),

    /**
     * Inspect container details.
     */
    INSPECT_CONTAINER("Inspect container details"),

    /**
     * Copy files to/from a container.
     */
    COPY_FILES("Copy files to/from container"),

    /**
     * Connect a container to a network.
     */
    CONNECT_NETWORK("Connect container to network"),

    /**
     * Disconnect a container from a network.
     */
    DISCONNECT_NETWORK("Disconnect container from network");

    private final String description;

    DockerActionType(String description) {
        this.description = description;
    }

    /**
     * Returns the description of this action type.
     *
     * @return the action type description
     */
    public String getDescription() {
        return description;
    }
}
