plugins {
    java
    application
}

dependencies {
    implementation(project(":common"))

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.1")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.1")

    implementation("info.picocli:picocli:4.7.5")

    compileOnly("org.projectlombok:lombok:1.18.32")
    annotationProcessor("org.projectlombok:lombok:1.18.32")
}

application {
    mainClass.set("net.protsenko.benchmarkmodule.BenchmarkRunner")
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}