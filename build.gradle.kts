// Top-level build file for NotiOff modern rewrite (PR 1 scaffold).
// Plugins are applied in subprojects via the plugins {} block with `apply false` here.

plugins {
    id("com.android.application") version "8.9.1" apply false
    id("org.jetbrains.kotlin.android") version "2.1.10" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.10" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.10" apply false
}
