pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()

        // Must be added when used as an Xposed Module, otherwise optional
        maven { url = uri("https://api.xposed.info/") }
        // MavenCentral has a 2-hour cache, if the latest version cannot be integrated, please add this URL
        maven { url = uri("https://s01.oss.sonatype.org/content/repositories/releases/") }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Must be added when used as an Xposed Module, otherwise optional
        maven { url = uri("https://api.xposed.info/") }
        // MavenCentral has a 2-hour cache, if the latest version cannot be integrated, please add this URL
        maven { url = uri("https://s01.oss.sonatype.org/content/repositories/releases/") }
    }
}

rootProject.name = "HyperPods"
include(":app")
 