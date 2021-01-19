buildscript {
    repositories {
        mavenLocal()
        jcenter()
    }
    dependencies {
        classpath("org.springframework.boot:spring-boot-gradle-plugin:$springBootVersion")
    }
}

plugins {
    id "io.spring.dependency-management" version "1.0.9.RELEASE"
    id "com.adarshr.test-logger"
}

apply plugin: "org.springframework.boot"

repositories {
    mavenLocal()
    jcenter()
}

dependencies {
    implementation(project(":graphql-datetime-spring-boot-starter-webflux"))

    implementation("org.springframework.boot:spring-boot-starter-webflux:$springBootVersion")
    implementation("com.graphql-java-kickstart:graphql-kickstart-spring-boot-starter-webflux:$graphqlSpringBootVersion")
    implementation("com.graphql-java-kickstart:graphql-kickstart-spring-boot-starter-tools:$graphqlSpringBootVersion")
    implementation("io.projectreactor:reactor-core:3.3.6.RELEASE")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

jar.enabled = false
uploadArchives.enabled = false
