/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package tools.spirals.cerberus237.adaptationactionsbase.core;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.spirals.cerberus237.adaptationactionsbase.enums.AdaptationActionResult;

/**
 * A REST-based implementation of the {@link IAdaptationAction}.
 * It executes a batch of adaptation actions by sending an HTTP POST request 
 * containing a JSON array of action strings to a specific target microservice.
 *
 * @author Arléon Zemtsop (Cerberus)
 */
public class RestAdaptationAction implements IAdaptationAction {

    protected static final Logger logger = LoggerFactory.getLogger(RestAdaptationAction.class);

    private final String actionId;
    private final List<String> adaptationActions;
    private final String targetServiceUrl;
    private final String description;
    private final HttpClient httpClient;

    /**
     * Constructs a new REST adaptation action representing a batch of operations.
     * The unique actionId is automatically generated using a UUID.
     *
     * @param adaptationActions A list of string representing the actions to perform (e.g., ["DisableMaintenanceMode", "ClearCache"])
     * @param targetServiceUrl  The exact endpoint URL to trigger the adaptation (e.g., "http://service:8080/adapt")
     * @param description       A human-readable description of what this batch of actions does
     */
    public RestAdaptationAction(List<String> adaptationActions, String targetServiceUrl, String description) {
        this.actionId = UUID.randomUUID().toString();
        this.adaptationActions = adaptationActions;
        this.targetServiceUrl = targetServiceUrl;
        this.description = description;
        
        // Initialize a lightweight HTTP client with a timeout
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    @Override
    public AdaptationActionResult perform() {
        if (adaptationActions == null || adaptationActions.isEmpty()) {
            logger.warn("[RestAdaptationAction] The adaptation action list is empty. Nothing to perform.");
            return AdaptationActionResult.SUCCESS; // Or FAILED, depending on your business logic
        }

        logger.info("[RestAdaptationAction] Performing batch action '{}' towards {}", actionId, targetServiceUrl);

        try {
            // Convert the List<String> into a valid JSON array format. 
            // e.g., ["Action1", "Action2"]
            String jsonPayload = "[" + adaptationActions.stream()
                    .map(action -> "\"" + action + "\"")
                    .collect(Collectors.joining(", ")) + "]";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(targetServiceUrl))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            logger.debug("[RestAdaptationAction] Sending payload: {}", jsonPayload);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            // Check if the HTTP status indicates success (200 OK, 202 Accepted, 204 No Content)
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                logger.info("[RestAdaptationAction] Action '{}' executed successfully.", actionId);
                return AdaptationActionResult.SUCCESS;
            } else {
                logger.error("[RestAdaptationAction] Action '{}' failed. HTTP Status: {}, Response: {}", 
                        actionId, response.statusCode(), response.body());
                return AdaptationActionResult.FAILURE;
            }

        } catch (InterruptedException e) {
            logger.error("[RestAdaptationAction] Action '{}' was interrupted.", actionId);
            Thread.currentThread().interrupt();
            return AdaptationActionResult.FAILURE;
        } catch (Exception e) {
            logger.error("[RestAdaptationAction] Action '{}' encountered an error: {}", actionId, e.getMessage(), e);
            return AdaptationActionResult.FAILURE;
        }
    }

    @Override
    public String getActionId() {
        return this.actionId;
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    @Override
    public boolean canPerform() {
        return adaptationActions != null && !adaptationActions.isEmpty();
    }
}