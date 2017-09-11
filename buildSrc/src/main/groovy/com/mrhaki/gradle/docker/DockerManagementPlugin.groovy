package com.mrhaki.gradle.docker

import de.gesellix.gradle.docker.DockerPlugin
import de.gesellix.gradle.docker.tasks.DockerBuildTask
import de.gesellix.gradle.docker.tasks.DockerPushTask
import de.gesellix.gradle.docker.tasks.DockerRmiTask
import de.gesellix.gradle.docker.tasks.DockerTagTask
import groovy.transform.TypeChecked
import groovy.transform.TypeCheckingMode
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.tasks.Copy

import java.util.regex.Pattern

import static de.gesellix.gradle.docker.tasks.DockerContainerTask.State.ABSENT
import static de.gesellix.gradle.docker.tasks.DockerContainerTask.State.PRESENT
import static de.gesellix.gradle.docker.tasks.DockerContainerTask.State.RELOADED
import static de.gesellix.gradle.docker.tasks.DockerContainerTask.State.STARTED
import static de.gesellix.gradle.docker.tasks.DockerContainerTask.State.STOPPED

/**
 * Gradle plugin to add support for working with Docker. 
 * The plugins adds tasks to build and remove a Docker image.
 * Also tasks to handle the lifecycle of a Docker container are added.
 */
@TypeChecked
class DockerManagementPlugin implements Plugin<Project> {

    private static final String GROUP_NAME = 'Docker'

    private Project project

    void apply(final Project project) {
        this.project = project
        applyPlugins()
        addDockerContainerExtension()
        addDockerImageTasks()
        addDockerContainerTasks()
    }

    private applyPlugins() {
        project.apply plugin: DockerPlugin
    }

    private void addDockerContainerExtension() {
        project.extensions.create(DockerManagementPluginExtension.NAME, DockerManagementPluginExtension, project)
    }

    private void addDockerImageTasks() {
        final DockerManagementPluginExtension extension = findExtension()

        final prepareImage = project.tasks.create('prepareImage', Copy) { Copy task ->
            task.group = GROUP_NAME
            task.description = "Prepare Docker image."

            task.into("${project.buildDir}/docker")
            task.from("${project.projectDir}/src/docker")
        }

        final tagImage = project.tasks.create('tagImage', DockerTagTask) { DockerTagTask task ->
            task.group = GROUP_NAME
            project.afterEvaluate {
                task.imageId = extension.imageName
                task.tag = createImageTagName(extension)

                task.description = "Tag image '${task.imageId}' as ${task.tag}."
            }
        }

        final buildImage = project.tasks.create('buildImage', DockerBuildTask) { DockerBuildTask task ->
            task.group = GROUP_NAME
            task.dependsOn prepareImage

            task.buildContextDirectory = prepareImage.destinationDir

            task.enableBuildLog = true

            task.finalizedBy tagImage

            project.afterEvaluate {
                final extraProperties = task.extensions.extraProperties
                final buildArgs = extraProperties.has('buildArgs') ? extraProperties.get('buildArgs') : [:]
                task.buildParams = [rm: true, buildargs: buildArgs]

                task.imageName = extension.imageName

                task.description = "Build image '${task.imageName}' from Dockerfile at '${project.relativePath(task.buildContextDirectory)}'."
            }
        }

        final removeLatestImage = project.tasks.create('removeLatestImage', DockerRmiTask) { DockerRmiTask task ->
            task.group = GROUP_NAME

            project.afterEvaluate {
                task.imageId = extension.imageName
                task.description = "Remove image '${task.imageId}'."
            }
        }

        project.tasks.create('removeImage', DockerRmiTask) { DockerRmiTask task ->
            task.group = GROUP_NAME
            task.dependsOn removeLatestImage

            project.afterEvaluate {
                task.imageId = createImageTagName(extension)
                task.description = "Remove image '${task.imageId}'."
            }
        }
    }
    
    private String createImageTagName(final DockerManagementPluginExtension extension) {
        "${extension.imageName}:${extension.imageTag}"
    }

    private void addDockerContainerTasks() {
        final DockerManagementPluginExtension extension = findExtension()

        project.tasks.create("createContainer", DockerConfigContainerTask) { DockerConfigContainerTask task ->
            task.description = "Create container"
            task.targetState = PRESENT
        }

        project.tasks.create("startContainer", DockerConfigContainerTask) { DockerConfigContainerTask task ->
            task.description = "Start container"
            task.targetState = STARTED
        }

        project.tasks.create("stopContainer", DockerConfigContainerTask) { DockerConfigContainerTask task ->
            task.description = "Stop container"
            task.targetState = STOPPED
        }

        project.tasks.create("removeContainer", DockerConfigContainerTask) { DockerConfigContainerTask task ->
            task.description = "Remove container"
            task.targetState = ABSENT
        }

        project.tasks.create("reloadContainer", DockerConfigContainerTask) { DockerConfigContainerTask task ->
            task.description = "Reload container"
            task.targetState = RELOADED
        }

        project.afterEvaluate {
            project.tasks.withType(DockerConfigContainerTask) { DockerConfigContainerTask task ->
                task.image = extension.imageName
                task.containerName = extension.containerName

                // Use actual container name in task description.
                task.description = "${task.description} ${task.containerName}."

                task.ignoredEnvKeys = ['no_proxy']
            }
            project.tasks.withType(DockerConfigContainerTask, extension.containerConfiguration)
        }
    }

    private DockerManagementPluginExtension findExtension() {
        project.extensions.findByType(DockerManagementPluginExtension)
    }
}
