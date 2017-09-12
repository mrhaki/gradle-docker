package com.mrhaki.gradle.docker

import de.gesellix.gradle.docker.tasks.DockerContainerTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional


class DockerConfigContainerTask extends DockerContainerTask {

    @Input
    @Optional
    List<String> volumesFrom = []
    
    @Input
    @Optional
    String networkMode 

    @Override
    Object createConfig() {
        final config =  super.createConfig()

        if (volumesFrom) {
            config.HostConfig.VolumesFrom = volumesFrom
        }
        
        if (networkMode) {
            config.HostConfig.NetworkMode = networkMode
        }
        
        return config
    }
}

