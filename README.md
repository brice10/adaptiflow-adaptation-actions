# AdaptiFlow Adaptation Actions Library

A Java Maven library providing infrastructure-level adaptation actions for self-adaptive systems.

## Overview

This library is part of the **AdaptiFlow** framework, providing adaptation actions for Docker container management.

## Docker Adaptation Actions

| Action | Description | Rollback |
|--------|-------------|----------|
| `StartContainerAction` | Start a stopped container | Yes |
| `StopContainerAction` | Stop a running container | Yes |
| `RestartContainerAction` | Restart a container | Yes |
| `PauseContainerAction` | Pause a running container | Yes |
| `UnpauseContainerAction` | Unpause a paused container | Yes |
| `KillContainerAction` | Force kill a container | Yes |
| `RemoveContainerAction` | Remove a container | No |
| `ScaleOutAction` | Create container replicas | Yes |
| `ScaleInAction` | Remove container replicas | No |
| `UpdateResourcesAction` | Update CPU/memory limits | Yes |
| `ExecCommandAction` | Execute command in container | No |

## Installation

```xml
<dependency>
    <groupId>io.github.brice10</groupId>
    <artifactId>adaptationactionsbase</artifactId>
    <version>0.0.1</version>
</dependency>
```

## Usage

```java
import tools.spirals.cerberus237.adaptationactionsbase.docker.DockerActionFactory;
import tools.spirals.cerberus237.adaptationactionsbase.enums.AdaptationActionResult;

DockerActionFactory factory = new DockerActionFactory();

if (factory.isDockerAvailable()) {
    // Start a container
    AdaptationActionResult result = factory.startContainer("my-container").perform();
    
    // Scale out
    factory.scaleOut("source-container", 3).perform();
    
    // Update resources
    factory.updateResources("my-container")
        .withMemoryLimitMB(512)
        .withCpuShares(1024)
        .perform();
}
```

## Requirements

- Java 11+
- Docker Engine
- Mount Docker socket: `-v /var/run/docker.sock:/var/run/docker.sock`

## License

Apache License, Version 2.0

## Author

**Arléon Zemtsop (Cerberus237)** - University of Lille
