/*
 *  Copyright 2017 Alexey Zhokhov
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

buildscript {
    repositories {
        mavenLocal()
        jcenter()
        mavenCentral()
        maven { url 'https://plugins.gradle.org/m2/' }
    }
    dependencies {
        classpath "com.github.ben-manes:gradle-versions-plugin:$gradleVersionsPluginVersion"
        classpath "org.jfrog.buildinfo:build-info-extractor-gradle:$gradleArtifactoryPluginVersion"
        classpath "com.jfrog.bintray.gradle:gradle-bintray-plugin:$gradleBintrayPluginVersion"
    }
}

plugins {
    id "idea"
    id "com.adarshr.test-logger" version "2.0.0" apply false
    id "net.rdrei.android.buildtimetracker" version "0.11.0"
}

allprojects {
    apply plugin: 'idea'
    apply plugin: 'java'

    idea {
        module {
            downloadJavadoc = false
            downloadSources = false
        }
    }

    compileJava {
        sourceCompatibility = 1.8
        targetCompatibility = 1.8
    }
}

subprojects {
    apply plugin: 'maven'
    apply plugin: 'maven-publish'
    apply plugin: 'com.jfrog.bintray'
    if (System.getenv('ARTIFACTORY_CONTEXT_URL')) {
        apply plugin: 'com.jfrog.artifactory'
    }
    apply plugin: 'groovy'
    apply plugin: 'com.github.ben-manes.versions'
    apply plugin: 'com.jfrog.artifactory'
    apply plugin: 'com.jfrog.bintray'

    version = projectVersion
    group = 'com.zhokhov.graphql'

    repositories {
        mavenLocal()
        jcenter()
        maven { url 'https://dl.bintray.com/andimarek/graphql-java/' }
    }

    dependencies {
        testCompile "org.spockframework:spock-core:$spockVersion"
        testCompile "org.codehaus.groovy:groovy:$groovyVersion"
    }

    if (!it.name.startsWith('graphql-datetime-sample-app')) {
        jar {
            from 'LICENSE'
        }

        task sourcesJar(type: Jar) {
            dependsOn classes
            classifier 'sources'
            from sourceSets.main.allSource
        }

        task javadocJar(type: Jar, dependsOn: javadoc) {
            classifier = 'javadoc'
            from javadoc.destinationDir
        }

        artifacts {
            archives jar
            archives sourcesJar
            archives javadocJar
        }

        publishing {
            publications {
                mainProjectPublication(MavenPublication) {
                    version project.version
                    from components.java

                    artifact sourcesJar {
                        classifier 'sources'
                    }
                    artifact javadocJar {
                        classifier 'javadoc'
                    }

                    pom.withXml {
                        // Fix dependency scoping.
                        asNode().dependencies.'*'.findAll() {
                            it.scope.text() == 'runtime' && project.configurations.compile.allDependencies.find { dep ->
                                dep.name == it.artifactId.text()
                            }
                        }.each() {
                            it.scope*.value = 'compile'
                        }

                        asNode().children().last() + {
                            resolveStrategy = Closure.DELEGATE_FIRST
                            name projectName
                            description projectDescription
                            url projectGitRepoUrl
                            scm {
                                url projectGitRepoUrl
                                connection projectGitRepoUrl
                                developerConnection projectGitRepoUrl
                            }
                            licenses {
                                license {
                                    name projectLicense
                                    url projectLicenseUrl
                                    distribution 'repo'
                                }
                            }
                            developers {
                                developer {
                                    id 'donbeave'
                                    name 'Alexey Zhokhov'
                                }
                            }
                        }
                    }
                }
            }
        }

        bintray {
            user = System.getenv('BINTRAY_USER') ?: project.findProperty('BINTRAY_USER') ?: ''
            key = System.getenv('BINTRAY_KEY') ?: project.findProperty('BINTRAY_KEY') ?: ''
            publications = ['mainProjectPublication']
            publish = System.getenv('BINTRAY_PUBLISH') != null ? System.getenv('BINTRAY_PUBLISH') == 'true' : !projectVersion.contains('SNAPSHOT')
            override = System.getenv('BINTRAY_OVERRIDE') == 'true'
            pkg {
                repo = 'maven'
                name = projectName
                desc = projectDescription
                licenses = [projectLicense]
                vcsUrl = projectGitRepoUrl
                version {
                    name = project.version
                    gpg {
                        sign = true
                    }
                    mavenCentralSync {
                        sync = System.getenv('MAVEN_CENTRAL_SYNC') != null ? System.getenv('MAVEN_CENTRAL_SYNC') == 'true' : !projectVersion.contains('SNAPSHOT')
                        user = System.getenv('SONATYPE_USER')
                        password = System.getenv('SONATYPE_PASSWORD')
                        close = '1'
                    }
                }
            }
        }

        if (System.getenv('ARTIFACTORY_CONTEXT_URL')) {
            artifactory {
                contextUrl = System.getenv('ARTIFACTORY_CONTEXT_URL')

                publish {
                    defaults {
                        publications('mainProjectPublication')
                        publishArtifacts = true
                        publishPom = true

                    }
                    repository {
                        repoKey = "${version.contains('SNAPSHOT') ? (System.getenv('ARTIFACTORY_SNAPSHOT_REPO_KEY') ?: 'libs-snapshot-local') : (System.getenv('ARTIFACTORY_RELEASE_REPO_KEY') ?: 'libs-release-local')}"
                        username = System.getenv('ARTIFACTORY_USERNAME') ?: 'admin'
                        password = System.getenv('ARTIFACTORY_PASSWORD') ?: 'password'
                    }
                }
            }
        }
    }
}
