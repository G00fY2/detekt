plugins {
    module
}

dependencies {
    api(kotlin("stdlib-jdk8"))
    compileOnly("org.spekframework.spek2:spek-dsl-jvm:2.0.15")
    implementation(project(":detekt-parser"))
    implementation(project(":detekt-psi-utils"))
    implementation(kotlin("script-runtime"))
    implementation(kotlin("script-util"))
    implementation(kotlin("scripting-compiler-embeddable"))
}
