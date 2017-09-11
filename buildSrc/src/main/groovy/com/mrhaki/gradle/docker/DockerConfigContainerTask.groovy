package com.mrhaki.gradle.docker

import de.gesellix.gradle.docker.tasks.DockerContainerTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional


class DockerConfigContainerTask extends DockerContainerTask {

    @Input
    @Optional
    List<String> volumesFrom = []

    @Override
    Object createConfig() {
        final config =  super.createConfig()

        if (volumesFrom) {
            config.HostConfig.VolumesFrom = volumesFrom
        }
        
        return config
    }
}

