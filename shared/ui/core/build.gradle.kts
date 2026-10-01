plugins {
    id("ivy.feature")
}

android {
    namespace = "com.ivy.ui"
}

dependencies {
    implementation(projects.shared.base)
    implementation(projects.shared.domain)

    // Frosted-glass panels (FrostedGlass.kt exposes Haze types)
    api(libs.haze)
}