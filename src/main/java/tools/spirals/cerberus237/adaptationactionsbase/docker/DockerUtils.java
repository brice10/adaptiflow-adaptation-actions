package tools.spirals.cerberus237.adaptationactionsbase.docker;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;

import tools.spirals.cerberus237.adaptationactionsbase.exceptions.DockerActionException;

public class DockerUtils {

    private static final String DEFAULT_DOCKER_HOST = "unix:///var/run/docker.sock";

    private DockerUtils() {
    }

    public static DockerClient createDefaultClient() {
        return createClient(DEFAULT_DOCKER_HOST);
    }

    public static DockerClient createClient(String dockerHost) {
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(dockerHost)
                .build();

        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(100)
                .connectionTimeout(Duration.ofSeconds(30))
                .responseTimeout(Duration.ofSeconds(45))
                .build();

        return DockerClientImpl.getInstance(config, httpClient);
    }

    public static boolean isDockerAvailable(DockerClient client) {
        try {
            client.pingCmd().exec();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static List<Container> listRunningContainers(DockerClient client) {
        return client.listContainersCmd().withShowAll(false).exec();
    }

    public static List<Container> listAllContainers(DockerClient client) {
        return client.listContainersCmd().withShowAll(true).exec();
    }

    public static List<Container> findContainersByImage(DockerClient client, String imageName) {
        return listAllContainers(client).stream()
                .filter(c -> c.getImage().equals(imageName) || c.getImage().startsWith(imageName + ":"))
                .collect(Collectors.toList());
    }

    public static boolean containerExists(DockerClient client, String containerId) {
        return listAllContainers(client).stream()
                .anyMatch(c -> c.getId().equals(containerId) || c.getId().startsWith(containerId));
    }

    public static Optional<String> getContainerState(DockerClient client, String containerId) {
        return listAllContainers(client).stream()
                .filter(c -> c.getId().equals(containerId) || c.getId().startsWith(containerId))
                .map(Container::getState)
                .findFirst();
    }

    /**
     * Finds a container by its ID or name.
     *
     * @param dockerClient the docker client
     * @param containerIdOrName the container ID or name to search for
     * @return the Container object if found
     * @throws DockerActionException if the container is not found
     */
    public static Container findContainer(DockerClient dockerClient, String containerIdOrName) {
        List<Container> containers = dockerClient.listContainersCmd()
                .withShowAll(true)
                .exec();

        for (Container container : containers) {
            if (container.getId().equals(containerIdOrName) ||
                    container.getId().startsWith(containerIdOrName)) {
                return container;
            }
            for (String name : container.getNames()) {
                // Container names are prefixed with '/'
                String normalizedName = name.startsWith("/") ? name.substring(1) : name;
                if (normalizedName.equals(containerIdOrName) ||
                        normalizedName.startsWith(containerIdOrName)) {
                    return container;
                }
            }
        }
        return null;
    }

    /**
     * Checks if a container is in a running state.
     *
     * @param dockerClient the docker client
     * @param containerIdOrName the container ID or name
     * @return true if the container is running
     */
    public static boolean isContainerRunning(DockerClient dockerClient, String containerIdOrName) {
        Container container = findContainer(dockerClient, containerIdOrName);
        return container != null && "running".equalsIgnoreCase(container.getState());
    }

    /**
     * Checks if a container is in a running state.
     *
     * @param container the container
     * @return true if the container is running
     */
    public static boolean isContainerRunning(Container container) {
        return container != null && "running".equalsIgnoreCase(container.getState());
    }

    /**
     * Checks if a container is in a paused state.
     * 
     * @param dockerClient the docker client
     * @param containerIdOrName the container ID or name
     * @return true if the container is paused
     */
    public static boolean isContainerPaused(DockerClient dockerClient, String containerIdOrName) {
        Container container = findContainer(dockerClient, containerIdOrName);
        return container != null && "paused".equalsIgnoreCase(container.getState());
    }

    /**
     * Checks if a container is in a paused state.
     * 
     * @param container the container
     * @return true if the container is paused
     */
    public static boolean isContainerPaused(Container container) {
        return container != null && "paused".equalsIgnoreCase(container.getState());
    }

    /**
     * Checks if a container is in a stopped/exited state.
     * 
     * @param dockerClient the docker client
     * @param containerIdOrName the container ID or name
     * @return true if the container is stopped
     */
    public static boolean isContainerStopped(DockerClient dockerClient, String containerIdOrName) {
        Container container = findContainer(dockerClient, containerIdOrName);
        return container != null && ("exited".equalsIgnoreCase(container.getState()) || "created".equalsIgnoreCase(container.getState()));
    }
    
    /**
     * Checks if a container is in a stopped/exited state.
     * 
     * @param container the container ID or name
     * @return true if the container is stopped
     */
    public static boolean isContainerStopped(Container container) {
        return container != null && ("exited".equalsIgnoreCase(container.getState()) || "created".equalsIgnoreCase(container.getState()));
    }
}
