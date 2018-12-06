/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.gradlebuild.distributed.model

import org.gradle.api.Project


data class BuildInvocation(
    val buildType: BuildType,
    val buildEnvironment: BuildEnvironment
) {
    fun findTasksForInvocation(project: Project): List<List<String>> {
        val tasks = mutableListOf<List<String>>()
        if (buildType.tasks.any { it.contains(":") }) {
            // do not expand for each subproject
            val tasksForProject = mutableListOf<String>()
            buildType.tasks.forEach { taskName ->
                if (taskName.startsWith(":")) {
                    tasksForProject.add(findTaskForInvocation(taskName, project).path)
                } else {
                    project.subprojects.forEach { subproject ->
                        if (subproject.tasks.findByName(taskName) != null) {
                            tasksForProject.add(findTaskForInvocation(taskName, subproject).path)
                        }
                    }
                }
            }
            tasks.add(tasksForProject)
        } else {
            //FIXME for testing, exclude projects which are expensive to run integration tests for because they need the docs tasks (currently not cacheable)
            project.subprojects.toList().subList(0, 12).forEach { subproject ->
                val tasksForSubproject = mutableListOf<String>()
                buildType.tasks.forEach { taskName ->
                    if (subproject.tasks.findByName(taskName) != null) {
                        tasksForSubproject.add(findTaskForInvocation(taskName, subproject).path)
                    }
                }
                if (!tasksForSubproject.isEmpty()) {
                    tasks.add(tasksForSubproject)
                }
            }
        }
        return tasks
    }

    private
    fun findTaskForInvocation(taskName: String, project: Project) = project.tasks.getByPath(taskName).let { baseTask ->
        if (buildType.environmentSpecific) {
            project.tasks.getByName(buildEnvironment.asEnvironmentSpecificName(baseTask.name))
        } else {
            baseTask
        }
    }
}
