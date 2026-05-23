plugins {
    id("org.springframework.boot") version "3.2.4" apply false
    id("io.spring.dependency-management") version "1.1.4" apply false
}

allprojects {
    group = "com.cosmoscan"
    version = "1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}