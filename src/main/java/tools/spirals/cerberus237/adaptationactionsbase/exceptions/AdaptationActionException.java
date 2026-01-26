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

/**
 * Exception thrown when an adaptation action fails to execute properly.
 *
 * @author Arléon Zemtsop (Cerberus)
 */
public class AdaptationActionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String actionId;
    private final String actionType;

    /**
     * Constructs a new AdaptationActionException with the specified message.
     *
     * @param message the detail message
     */
    public AdaptationActionException(String message) {
        super(message);
        this.actionId = null;
        this.actionType = null;
    }

    /**
     * Constructs a new AdaptationActionException with the specified message and cause.
     *
     * @param message the detail message
     * @param cause   the cause of the exception
     */
    public AdaptationActionException(String message, Throwable cause) {
        super(message, cause);
        this.actionId = null;
        this.actionType = null;
    }

    /**
     * Constructs a new AdaptationActionException with action context.
     *
     * @param message    the detail message
     * @param actionId   the identifier of the action that failed
     * @param actionType the type of the action that failed
     */
    public AdaptationActionException(String message, String actionId, String actionType) {
        super(message);
        this.actionId = actionId;
        this.actionType = actionType;
    }

    /**
     * Constructs a new AdaptationActionException with action context and cause.
     *
     * @param message    the detail message
     * @param cause      the cause of the exception
     * @param actionId   the identifier of the action that failed
     * @param actionType the type of the action that failed
     */
    public AdaptationActionException(String message, Throwable cause, String actionId, String actionType) {
        super(message, cause);
        this.actionId = actionId;
        this.actionType = actionType;
    }

    /**
     * Returns the identifier of the action that caused this exception.
     *
     * @return the action identifier, or null if not specified
     */
    public String getActionId() {
        return actionId;
    }

    /**
     * Returns the type of the action that caused this exception.
     *
     * @return the action type, or null if not specified
     */
    public String getActionType() {
        return actionType;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        if (actionId != null) {
            sb.append(" [actionId=").append(actionId);
            if (actionType != null) {
                sb.append(", actionType=").append(actionType);
            }
            sb.append("]");
        }
        return sb.toString();
    }
}
