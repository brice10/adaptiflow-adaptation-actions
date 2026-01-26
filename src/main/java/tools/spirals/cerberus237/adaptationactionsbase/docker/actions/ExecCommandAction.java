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
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import tools.spirals.cerberus237.adaptationactionsbase.docker.AbstractDockerAction;
import tools.spirals.cerberus237.adaptationactionsbase.enums.AdaptationActionResult;
import tools.spirals.cerberus237.adaptationactionsbase.enums.DockerActionType;
import tools.spirals.cerberus237.adaptationactionsbase.exceptions.DockerActionException;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Adaptation action that executes a command inside a running Docker container.
 * <p>
 * This action creates an exec instance and runs a command within the container's
 * context. The output can be captured and inspected.
 * </p>
 *
 * @author Arléon Zemtsop (Cerberus)
 */
public class ExecCommandAction extends AbstractDockerAction {

    private final String[] command;
    private final String workingDir;
    private final String[] environmentVariables;
    private final boolean attachStdout;
    private final boolean attachStderr;
    private final boolean privileged;

    private String stdout;
    private String stderr;
    private Long exitCode;

    /**
     * Constructs a new ExecCommandAction with the specified command.
     *
     * @param containerId the ID or name of the container
     * @param command     the command to execute (e.g., "ls", "-la")
     */
    public ExecCommandAction(String containerId, String... command) {
        this(containerId, null, null, true, true, false, command);
    }

    /**
     * Constructs a new ExecCommandAction with options.
     *
     * @param containerId          the ID or name of the container
     * @param workingDir           the working directory for the command
     * @param environmentVariables environment variables to set
     * @param attachStdout         if true, capture stdout
     * @param attachStderr         if true, capture stderr
     * @param privileged           if true, run with extended privileges
     * @param command              the command to execute
     */
    public ExecCommandAction(String containerId, String workingDir, String[] environmentVariables,
                             boolean attachStdout, boolean attachStderr, boolean privileged,
                             String... command) {
        super(containerId, DockerActionType.EXEC_COMMAND);
        this.command = command;
        this.workingDir = workingDir;
        this.environmentVariables = environmentVariables;
        this.attachStdout = attachStdout;
        this.attachStderr = attachStderr;
        this.privileged = privileged;
    }

    /**
     * Constructs a new ExecCommandAction with custom timeout.
     *
     * @param containerId          the ID or name of the container
     * @param timeoutSeconds       the timeout in seconds
     * @param workingDir           the working directory for the command
     * @param environmentVariables environment variables to set
     * @param attachStdout         if true, capture stdout
     * @param attachStderr         if true, capture stderr
     * @param privileged           if true, run with extended privileges
     * @param command              the command to execute
     */
    public ExecCommandAction(String containerId, int timeoutSeconds, String workingDir,
                             String[] environmentVariables, boolean attachStdout, boolean attachStderr,
                             boolean privileged, String... command) {
        super(containerId, DockerActionType.EXEC_COMMAND, timeoutSeconds);
        this.command = command;
        this.workingDir = workingDir;
        this.environmentVariables = environmentVariables;
        this.attachStdout = attachStdout;
        this.attachStderr = attachStderr;
        this.privileged = privileged;
    }

    /**
     * Constructs a new ExecCommandAction with a custom Docker client.
     *
     * @param containerId          the ID or name of the container
     * @param timeoutSeconds       the timeout in seconds
     * @param dockerClient         the Docker client to use
     * @param workingDir           the working directory for the command
     * @param environmentVariables environment variables to set
     * @param attachStdout         if true, capture stdout
     * @param attachStderr         if true, capture stderr
     * @param privileged           if true, run with extended privileges
     * @param command              the command to execute
     */
    public ExecCommandAction(String containerId, int timeoutSeconds, DockerClient dockerClient,
                             String workingDir, String[] environmentVariables, boolean attachStdout,
                             boolean attachStderr, boolean privileged, String... command) {
        super(containerId, DockerActionType.EXEC_COMMAND, timeoutSeconds, dockerClient);
        this.command = command;
        this.workingDir = workingDir;
        this.environmentVariables = environmentVariables;
        this.attachStdout = attachStdout;
        this.attachStderr = attachStderr;
        this.privileged = privileged;
    }

    @Override
    public boolean canPerform() {
        if (!super.canPerform()) {
            return false;
        }
        try {
            // Container must be running
            return isContainerRunning(containerId) && command != null && command.length > 0;
        } catch (Exception e) {
            logger.warn("Cannot perform exec command action: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getDescription() {
        return String.format("%s for container %s: %s",
                actionType.getDescription(), containerId, Arrays.toString(command));
    }

    @Override
    public AdaptationActionResult perform() {
        logActionStart();
        logger.info("Executing command in container {}: {}", containerId, Arrays.toString(command));

        try {
            // Create exec instance
            var execCreateCmd = dockerClient.execCreateCmd(containerId)
                    .withAttachStdout(attachStdout)
                    .withAttachStderr(attachStderr)
                    .withCmd(command);

            if (workingDir != null) {
                execCreateCmd.withWorkingDir(workingDir);
            }
            if (environmentVariables != null) {
                execCreateCmd.withEnv(Arrays.asList(environmentVariables));
            }
            if (privileged) {
                execCreateCmd.withPrivileged(true);
            }

            ExecCreateCmdResponse execResponse = execCreateCmd.exec();
            String execId = execResponse.getId();

            // Start exec and capture output
            ByteArrayOutputStream stdoutStream = new ByteArrayOutputStream();
            ByteArrayOutputStream stderrStream = new ByteArrayOutputStream();

            ExecStartResultCallback callback = new ExecStartResultCallback() {
                @Override
                public void onNext(Frame frame) {
                    try {
                        if (frame.getStreamType() == com.github.dockerjava.api.model.StreamType.STDOUT) {
                            stdoutStream.write(frame.getPayload());
                        } else if (frame.getStreamType() == com.github.dockerjava.api.model.StreamType.STDERR) {
                            stderrStream.write(frame.getPayload());
                        }
                    } catch (Exception e) {
                        logger.warn("Error capturing exec output: {}", e.getMessage());
                    }
                    super.onNext(frame);
                }
            };

            dockerClient.execStartCmd(execId)
                    .exec(callback)
                    .awaitCompletion(timeoutSeconds, TimeUnit.SECONDS);

            // Capture output
            stdout = stdoutStream.toString();
            stderr = stderrStream.toString();

            // Get exit code
            var inspectExecResponse = dockerClient.inspectExecCmd(execId).exec();
            exitCode = inspectExecResponse.getExitCodeLong();

            if (exitCode != null && exitCode != 0) {
                logger.warn("Command exited with non-zero code: {}", exitCode);
                logger.warn("stderr: {}", stderr);
            }

            logActionSuccess();
            return AdaptationActionResult.SUCCESS;

        } catch (DockerActionException e) {
            logActionFailure(e);
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logActionFailure(e);
            return AdaptationActionResult.TIMEOUT;
        } catch (Exception e) {
            logActionFailure(e);
            throw new DockerActionException(
                    "Failed to execute command: " + e.getMessage(),
                    e,
                    containerId,
                    DockerActionType.EXEC_COMMAND
            );
        }
    }

    @Override
    public boolean supportsRollback() {
        // Command execution cannot be rolled back
        return false;
    }

    @Override
    public AdaptationActionResult rollback() {
        logger.warn("Rollback is not supported for exec command actions");
        return AdaptationActionResult.NOT_SUPPORTED;
    }

    /**
     * Returns the command to execute.
     *
     * @return the command array
     */
    public String[] getCommand() {
        return command != null ? command.clone() : null;
    }

    /**
     * Returns the captured stdout output.
     *
     * @return the stdout output, or null if not yet executed
     */
    public String getStdout() {
        return stdout;
    }

    /**
     * Returns the captured stderr output.
     *
     * @return the stderr output, or null if not yet executed
     */
    public String getStderr() {
        return stderr;
    }

    /**
     * Returns the exit code of the command.
     *
     * @return the exit code, or null if not yet executed
     */
    public Long getExitCode() {
        return exitCode;
    }

    /**
     * Checks if the command executed successfully (exit code 0).
     *
     * @return true if the command succeeded
     */
    public boolean isCommandSuccessful() {
        return exitCode != null && exitCode == 0;
    }
}
