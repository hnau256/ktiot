package hnau.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

class HnauKMPLibPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        target.config(
            androidMode = null,
        )
    }
}
