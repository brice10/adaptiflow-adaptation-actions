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
 * The {@link IRollbackableAdaptationAction} interface extends the
 * {@link IAdaptationAction} contract to include the capability for rollback.
 * <p>
 * Classes implementing this interface must provide a way to revert the
 * effects of the adaptation action previously performed. This is useful
 * in scenarios where changes need to be undone to restore the state
 * to a previous condition.
 * </p>
 * <p>
 * Implementing classes must provide an implementation of the
 * {@code rollback} method, which should encapsulate the logic for
 * reverting the effects of the action.
 * </p>
 *
 * @author Arléon Zemtsop (Cerberus)
 * @see IAdaptationAction
 */
public interface IRollbackableAdaptationAction extends IAdaptationAction {
    
    /**
     * Rolls back the effects of the adaptation action.
     * <p>
     * This method should contain the logic to undo changes made by the
     * adaptation action performed earlier. The rollback should be
     * executed in a safe and consistent manner to ensure that system state
     * is restored correctly.
     * </p>
     *
     * @return the result of the rollback operation, represented as an
     * {@link AdaptationActionResult}. This result indicates whether the
     * rollback was successful or if any errors occurred during the process.
     */
    AdaptationActionResult rollback();
}