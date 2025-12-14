// Jenkins Job DSL Seed Script
// This script automatically creates the CI/CD pipeline jobs for Currency Converter services

def repoUrl = 'https://github.com/your-org/Spring-Boot-Currency-Converter.git'
def credentialsId = 'github-credentials'
def services = ['main-service', 'rate-service']

// Create multibranch pipeline for each service
services.each { service ->
    multibranchPipelineJob("currency-converter-${service}") {
        displayName("Currency Converter - ${service.capitalize()}")
        description("${service.capitalize()} for currency converter application")

        branchSources {
            git {
                id("${service}-git-source")
                remote(repoUrl)
                credentialsId(credentialsId)

                // Build strategy
                includes('master main develop feature/* bugfix/* hotfix/*')
                excludes('experimental/*')
            }
        }

        // Branch discovery
        configure { node ->
            node / sources / 'data' / 'jenkins.branch.BranchSource' / source / traits {
                'jenkins.plugins.git.traits.BranchDiscoveryTrait' {}
                'jenkins.plugins.git.traits.TagDiscoveryTrait' {}
            }
        }

        // Jenkinsfile location
        factory {
            workflowBranchProjectFactory {
                scriptPath("${service}/Jenkinsfile")
            }
        }

        // Orphaned item strategy
        orphanedItemStrategy {
            discardOldItems {
                daysToKeep(7)
                numToKeep(20)
            }
        }

        // Triggers
        triggers {
            periodicFolderTrigger {
                interval('1h')
            }
        }

        // Properties
        configure { project ->
            project / 'properties' << 'org.jenkinsci.plugins.pipeline.modeldefinition.config.FolderConfig' {
                'dockerLabel'()
                'registry'()
            }
        }
    }
}

// Create a folder for monitoring views
folder('currency-converter-views') {
    displayName('Currency Converter - Monitoring Views')
    description('Dashboard views for monitoring pipeline health')
}

// Create dashboard view
listView('currency-converter-views/Pipeline Dashboard') {
    description('Overview of all currency converter pipelines')

    jobs {
        regex(/currency-converter-.*/)
    }

    columns {
        status()
        weather()
        name()
        lastSuccess()
        lastFailure()
        lastDuration()
        buildButton()
    }

    statusFilter(StatusFilter.ALL)
    recurse(true)
}

// Create build monitor view (requires Build Monitor Plugin)
buildMonitorView('currency-converter-views/Build Monitor') {
    description('Build monitor for currency converter services')

    jobs {
        regex(/currency-converter-.*/)
    }

    recurse(true)
}

// Create a pipeline for integration tests
pipelineJob('currency-converter-integration-tests') {
    displayName('Currency Converter - Integration Tests')
    description('End-to-end integration tests for all services')

    definition {
        cpsScm {
            scm {
                git {
                    remote {
                        url(repoUrl)
                        credentials(credentialsId)
                    }
                    branches('main', 'master', 'develop')
                }
            }
            scriptPath('scripts/integration-tests.Jenkinsfile')
        }
    }

    triggers {
        cron('H 2 * * *') // Run daily at 2 AM
    }
}

// Create a pipeline for performance tests
pipelineJob('currency-converter-performance-tests') {
    displayName('Currency Converter - Performance Tests')
    description('Performance and load testing for services')

    definition {
        cpsScm {
            scm {
                git {
                    remote {
                        url(repoUrl)
                        credentials(credentialsId)
                    }
                    branches('main', 'master', 'develop')
                }
            }
            scriptPath('scripts/performance-tests.Jenkinsfile')
        }
    }

    triggers {
        cron('H 4 * * 0') // Run weekly on Sunday at 4 AM
    }
}

// Create a pipeline for security scans
pipelineJob('currency-converter-security-scan') {
    displayName('Currency Converter - Security Scan')
    description('Comprehensive security scanning for all services')

    definition {
        cpsScm {
            scm {
                git {
                    remote {
                        url(repoUrl)
                        credentials(credentialsId)
                    }
                    branches('main', 'master', 'develop')
                }
            }
            scriptPath('scripts/security-scan.Jenkinsfile')
        }
    }

    triggers {
        cron('H 3 * * *') // Run daily at 3 AM
    }
}

// Create cleanup job
pipelineJob('currency-converter-cleanup') {
    displayName('Currency Converter - Cleanup')
    description('Cleanup old builds, artifacts, and Docker images')

    definition {
        cps {
            script('''
                pipeline {
                    agent any

                    options {
                        buildDiscarder(logRotator(numToKeepStr: '10'))
                        timeout(time: 30, unit: 'MINUTES')
                    }

                    stages {
                        stage('Cleanup Old Builds') {
                            steps {
                                script {
                                    def jobs = Jenkins.instance.getAllItems(Job.class)
                                    jobs.each { job ->
                                        if (job.fullName.contains('currency-converter')) {
                                            job.builds.findAll {
                                                it.number < job.nextBuildNumber - 20
                                            }.each {
                                                it.delete()
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        stage('Cleanup Docker Images') {
                            steps {
                                sh '''
                                    docker image prune -af --filter "until=168h"
                                    docker volume prune -f
                                '''
                            }
                        }
                    }
                }
            '''.stripIndent())
            sandbox(true)
        }
    }

    triggers {
        cron('H 1 * * 0') // Run weekly on Sunday at 1 AM
    }
}

println "Job DSL seed script completed successfully!"
println "Created jobs for services: ${services}"
