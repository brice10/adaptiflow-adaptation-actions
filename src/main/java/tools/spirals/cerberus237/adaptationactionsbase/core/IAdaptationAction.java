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
package tools.spirals.cerberus237.adaptationactionsbase.core;

import tools.spirals.cerberus237.adaptationactionsbase.enums.AdaptationActionResult;

/**
 * The {@link IAdaptationAction} interface defines a contract for classes
 * that represent an action to be performed as part of an adaptation process.
 * <p>
 * Implementing classes must provide an implementation of the {@code perform}
 * method to define the specific action to be executed.
 * </p>
 *
 * @author Arléon Zemtsop (Cerberus)
 */
public interface IAdaptationAction {

    /**
     * Performs the adaptation action.
     * <p>
     * This method contains the logic to execute the specific action defined by
     * the implementing class.
     * </p>
     *
     * @return the result of the adaptation action execution
     */
    AdaptationActionResult perform();

    /**
     * Returns the unique identifier of this adaptation action.
     *
     * @return the action identifier
     */
    String getActionId();

    /**
     * Returns a human-readable description of this adaptation action.
     *
     * @return the action description
     */
    String getDescription();

    /**
     * Checks if this action can be performed given the current system state.
     *
     * @return true if the action can be performed, false otherwise
     */
    boolean canPerform();
}
