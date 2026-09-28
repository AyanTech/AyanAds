pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // Use the Google Maven mirror for dependencies returning 404 from Google here.
        maven {
            url = uri("https://maven.aliyun.com/repository/google")
            content {
                includeGroup("androidx.lifecycle")
                includeGroup("androidx.privacysandbox.ads")
                includeGroup("androidx.startup")
                includeGroup("com.google.android.gms")
            }
        }
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "AyanAdManager"
include(":app")
include(":ayanadmanager")
