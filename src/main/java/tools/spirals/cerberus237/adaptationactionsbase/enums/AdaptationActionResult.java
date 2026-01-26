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
 * Enumeration representing the possible results of an adaptation action execution.
 *
 * @author Arléon Zemtsop (Cerberus)
 */
public enum AdaptationActionResult {

    /**
     * The adaptation action was executed successfully.
     */
    SUCCESS("Action completed successfully"),

    /**
     * The adaptation action failed during execution.
     */
    FAILURE("Action execution failed"),

    /**
     * The adaptation action is currently in progress.
     */
    IN_PROGRESS("Action is being executed"),

    /**
     * The adaptation action was skipped because preconditions were not met.
     */
    SKIPPED("Action was skipped due to unmet preconditions"),

    /**
     * The adaptation action timed out during execution.
     */
    TIMEOUT("Action execution timed out"),

    /**
     * The adaptation action was rolled back successfully.
     */
    ROLLED_BACK("Action was rolled back successfully"),

    /**
     * The rollback of the adaptation action failed.
     */
    ROLLBACK_FAILED("Action rollback failed"),

    /**
     * The adaptation action is not supported in the current environment.
     */
    NOT_SUPPORTED("Action is not supported in this environment"),

    /**
     * The adaptation action requires additional resources that are unavailable.
     */
    RESOURCE_UNAVAILABLE("Required resources are unavailable"),

    /**
     * The adaptation action execution result is unknown.
     */
    UNKNOWN("Action result is unknown");

    private final String description;

    AdaptationActionResult(String description) {
        this.description = description;
    }

    /**
     * Returns the description of this result.
     *
     * @return the result description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Checks if this result indicates a successful outcome.
     *
     * @return true if the result indicates success
     */
    public boolean isSuccessful() {
        return this == SUCCESS || this == ROLLED_BACK;
    }

    /**
     * Checks if this result indicates a failure.
     *
     * @return true if the result indicates failure
     */
    public boolean isFailure() {
        return this == FAILURE || this == TIMEOUT || this == ROLLBACK_FAILED;
    }
}
