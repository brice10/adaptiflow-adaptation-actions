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
package tools.spirals.cerberus237.adaptationactionsbase.exceptions;

import tools.spirals.cerberus237.adaptationactionsbase.enums.DockerActionType;

/**
 * Exception thrown when a Docker-specific adaptation action fails.
 *
 * @author Arléon Zemtsop (Cerberus)
 */
public class DockerActionException extends AdaptationActionException {

    private static final long serialVersionUID = 1L;

    private final String containerId;
    private final DockerActionType dockerActionType;

    /**
     * Constructs a new DockerActionException with the specified message.
     *
     * @param message the detail message
     */
    public DockerActionException(String message) {
        super(message);
        this.containerId = null;
        this.dockerActionType = null;
    }

    /**
     * Constructs a new DockerActionException with the specified message and cause.
     *
     * @param message the detail message
     * @param cause   the cause of the exception
     */
    public DockerActionException(String message, Throwable cause) {
        super(message, cause);
        this.containerId = null;
        this.dockerActionType = null;
    }

    /**
     * Constructs a new DockerActionException with container context.
     *
     * @param message          the detail message
     * @param containerId      the identifier of the container involved
     * @param dockerActionType the type of Docker action that failed
     */
    public DockerActionException(String message, String containerId, DockerActionType dockerActionType) {
        super(message, containerId, dockerActionType != null ? dockerActionType.name() : null);
        this.containerId = containerId;
        this.dockerActionType = dockerActionType;
    }

    /**
     * Constructs a new DockerActionException with container context and cause.
     *
     * @param message          the detail message
     * @param cause            the cause of the exception
     * @param containerId      the identifier of the container involved
     * @param dockerActionType the type of Docker action that failed
     */
    public DockerActionException(String message, Throwable cause, String containerId, DockerActionType dockerActionType) {
        super(message, cause, containerId, dockerActionType != null ? dockerActionType.name() : null);
        this.containerId = containerId;
        this.dockerActionType = dockerActionType;
    }

    /**
     * Returns the identifier of the container involved in this exception.
     *
     * @return the container identifier, or null if not specified
     */
    public String getContainerId() {
        return containerId;
    }

    /**
     * Returns the type of Docker action that caused this exception.
     *
     * @return the Docker action type, or null if not specified
     */
    public DockerActionType getDockerActionType() {
        return dockerActionType;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("DockerActionException: ").append(getMessage());
        if (containerId != null) {
            sb.append(" [containerId=").append(containerId);
            if (dockerActionType != null) {
                sb.append(", dockerActionType=").append(dockerActionType.name());
            }
            sb.append("]");
        }
        return sb.toString();
    }
}
