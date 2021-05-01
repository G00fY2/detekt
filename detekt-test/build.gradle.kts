plugins {
    module
}

dependencies {
    api(project(":detekt-api"))
    api(project(":detekt-test-utils"))
    compileOnly("org.assertj:assertj-core:3.19.0")
    implementation(project(":detekt-core"))
    implementation(project(":detekt-parser"))
}
