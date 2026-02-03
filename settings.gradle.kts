pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Try the official sherpa-onnx maven repository
        maven { url = uri("https://k2-fsa.github.io/sherpa/onnx/maven-repository/") }
        // Try another one if the above fails
        maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots") }
    }
}

rootProject.name = "chistanLand"
include(":app")
