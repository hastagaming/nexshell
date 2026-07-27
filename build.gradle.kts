plugins {
    id("com.android.application") version "8.6.0" apply false
    id("org.jetbrains.kotlin.android") version "2.0.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.20" apply false
}

allprojects {
    configurations.all {
        resolutionStrategy {
            force("com.google.guava:guava:32.1.3-android")
        }
        exclude(group = "com.google.guava", module = "listenablefuture")
    }
}