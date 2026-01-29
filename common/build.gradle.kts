plugins {
    java
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.1")

    compileOnly("org.projectlombok:lombok:1.18.32")
    annotationProcessor("org.projectlombok:lombok:1.18.32")
}