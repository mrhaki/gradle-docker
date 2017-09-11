package com.mrhaki.gradle.docker

import org.gradle.api.Project

class DockerManagementPluginExtension {
    
    public static final String NAME = 'dockerConfig'

    String imageName
    String imageTag
    String containerName
    Closure containerConfiguration = {}

    DockerManagementPluginExtension(final Project project) {
        imageName = project.name
        imageTag = project.version
        containerName = project.name
    }
}
