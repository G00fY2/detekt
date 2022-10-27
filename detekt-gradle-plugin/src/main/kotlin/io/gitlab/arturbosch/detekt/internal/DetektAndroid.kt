package io.gitlab.arturbosch.detekt.internal

import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.ApplicationVariant
import com.android.build.api.variant.DynamicFeatureVariant
import com.android.build.api.variant.LibraryVariant
import com.android.build.api.variant.TestVariant
import com.android.build.api.variant.Variant
import com.android.build.gradle.internal.tasks.factory.dependsOn
import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask
import io.gitlab.arturbosch.detekt.DetektPlugin
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.TaskProvider

internal class DetektAndroid(private val project: Project) {

    private val mainTaskProvider: TaskProvider<Task> by lazy {
        project.tasks.register("${DetektPlugin.DETEKT_TASK_NAME}Main") {
            it.group = "verification"
            it.description = "EXPERIMENTAL: Run detekt analysis for production classes across " +
                "all variants with type resolution"
        }
    }

    private val testTaskProvider: TaskProvider<Task> by lazy {
        project.tasks.register("${DetektPlugin.DETEKT_TASK_NAME}Test") {
            it.group = "verification"
            it.description = "EXPERIMENTAL: Run detekt analysis for test classes across " +
                "all variants with type resolution"
        }
    }

    private val mainBaselineTaskProvider: TaskProvider<Task> by lazy {
        project.tasks.register("${DetektPlugin.BASELINE_TASK_NAME}Main") {
            it.group = "verification"
            it.description = "EXPERIMENTAL: Creates detekt baseline files for production classes across " +
                "all variants with type resolution"
        }
    }

    private val testBaselineTaskProvider: TaskProvider<Task> by lazy {
        project.tasks.register("${DetektPlugin.BASELINE_TASK_NAME}Test") {
            it.group = "verification"
            it.description = "EXPERIMENTAL: Creates detekt baseline files for test classes across " +
                "all variants with type resolution"
        }
    }

    fun registerTasks(extension: DetektExtension) {
        // There is not a single Android plugin, but each registers an extension based on AndroidComponentsExtension,
        // so we catch them all by looking for this one
        project.extensions.findByType(AndroidComponentsExtension::class.java)?.let { androidComponentsExtension ->
            androidComponentsExtension.onVariants { variant ->
                when (variant) {
                    is ApplicationVariant, is LibraryVariant, is DynamicFeatureVariant -> {
                        if (!extension.matchesIgnoredConfiguration(variant)) {
                            project.registerAndroidDetektTask(extension, variant).also { provider ->
                                mainTaskProvider.dependsOn(provider)
                            }
                            project.registerAndroidCreateBaselineTask(extension, variant).also { provider ->
                                mainBaselineTaskProvider.dependsOn(provider)
                            }
                        }
                    }
                    is TestVariant -> {
                        if (!extension.matchesIgnoredConfiguration(variant)) {
                            project.registerAndroidDetektTask(extension, variant).also { provider ->
                                testTaskProvider.dependsOn(provider)
                            }
                            project.registerAndroidCreateBaselineTask(extension, variant).also { provider ->
                                testBaselineTaskProvider.dependsOn(provider)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun DetektExtension.matchesIgnoredConfiguration(variant: Variant): Boolean =
        ignoredVariants.contains(variant.name) ||
            ignoredBuildTypes.contains(variant.buildType) ||
            ignoredFlavors.contains(variant.flavorName)
}

internal fun Project.registerAndroidDetektTask(
    extension: DetektExtension,
    variant: Variant,
    taskName: String = DetektPlugin.DETEKT_TASK_NAME + variant.name.capitalize(),
    extraInputSource: FileCollection? = null
): TaskProvider<Detekt> =
    registerDetektTask(taskName, extension) {
        val sourceDirs = variant.sources.java?.all?.get().orEmpty() + variant.sources.kotlin?.all?.get().orEmpty()
        setSource(sourceDirs.map { it.asFile })
        extraInputSource?.let { source(it) }
        classpath.setFrom(variant.compileClasspath)
        // If a baseline file is configured as input file, it must exist to be configured, otherwise the task fails.
        // We try to find the configured baseline or alternatively a specific variant matching this task.
        extension.baseline?.existingVariantOrBaseFile(variant.name)?.let { baselineFile ->
            baseline.convention(layout.file(project.provider { baselineFile }))
        }
        setReportOutputConventions(reports, extension, variant.name)
        description = "EXPERIMENTAL: Run detekt analysis for ${variant.name} classes with type resolution"
    }

internal fun Project.registerAndroidCreateBaselineTask(
    extension: DetektExtension,
    variant: Variant,
    taskName: String = DetektPlugin.BASELINE_TASK_NAME + variant.name.capitalize(),
    extraInputSource: FileCollection? = null
): TaskProvider<DetektCreateBaselineTask> =
    registerCreateBaselineTask(taskName, extension) {
        val sourceDirs = variant.sources.java?.all?.get().orEmpty() + variant.sources.kotlin?.all?.get().orEmpty()
        setSource(sourceDirs.map { it.asFile })
        extraInputSource?.let { source(it) }
        classpath.setFrom(variant.compileClasspath)
        val variantBaselineFile = extension.baseline?.addVariantName(variant.name)
        baseline.convention(project.layout.file(project.provider { variantBaselineFile }))
        description = "EXPERIMENTAL: Creates detekt baseline for ${variant.name} classes with type resolution"
    }
