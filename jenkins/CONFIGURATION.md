# Jenkins Configuration Guide

This document explains how to configure the Jenkins pipelines for the Currency Converter services.

## Pipeline Configuration

The Jenkinsfiles use environment variables with sensible defaults, making them portable across different Jenkins installations.

### Environment Variables

You can customize the pipeline behavior by setting these environment variables in Jenkins:

| Variable | Description | Default Value | Example Values |
|----------|-------------|---------------|----------------|
| `JENKINS_AGENT` | Jenkins agent label for builds | `maven-instances` | `java-builder`, `maven-agent`, `docker-maven` |
| `DEPLOYMENT_ENV` | Target deployment environment | `aws` | `aws`, `azure`, `gcp`, `on-prem` |
| `K8S_NAMESPACE` | Kubernetes namespace | `currency-converter` | `dev`, `staging`, `production` |
| `ACCOUNT_NAME` | Cloud account/environment name | `production` | `dev-account`, `prod-aws`, `staging` |
| `MAVEN_VERSION` | Maven tool identifier in Jenkins | `M390` | `Maven-3.9`, `M3`, `maven-latest` |

### Configuration Methods

#### Method 1: Job-Level Environment Variables (Recommended)

1. Go to your Jenkins job
2. Click "Configure"
3. Check "This build is parameterized"
4. Add String Parameters for each variable you want to customize
5. Or use "Environment Variables" section

Example job configuration:

```groovy
pipeline {
    environment {
        JENKINS_AGENT = 'my-maven-agent'
        DEPLOYMENT_ENV = 'azure'
        K8S_NAMESPACE = 'dev-currency-converter'
        MAVEN_VERSION = 'Maven-3.9'
    }
    // ... rest of pipeline
}
```

#### Method 2: Global Environment Variables

1. Go to "Manage Jenkins" → "Configure System"
2. Scroll to "Global properties"
3. Check "Environment variables"
4. Add your variables

#### Method 3: Jenkins Configuration as Code (JCasC)

```yaml
unclassified:
  globalEnvironmentVariables:
    env:
      - key: JENKINS_AGENT
        value: maven-instances
      - key: DEPLOYMENT_ENV
        value: aws
      - key: K8S_NAMESPACE
        value: currency-converter
      - key: MAVEN_VERSION
        value: M390
```

## Setting Up for Different Environments

### Development Environment

```bash
JENKINS_AGENT=dev-agents
DEPLOYMENT_ENV=on-prem
K8S_NAMESPACE=dev-currency-converter
ACCOUNT_NAME=development
MAVEN_VERSION=Maven-3.9
```

### Staging Environment

```bash
JENKINS_AGENT=staging-agents
DEPLOYMENT_ENV=aws
K8S_NAMESPACE=staging-currency-converter
ACCOUNT_NAME=staging-account
MAVEN_VERSION=M390
```

### Production Environment

```bash
JENKINS_AGENT=prod-agents
DEPLOYMENT_ENV=aws
K8S_NAMESPACE=currency-converter
ACCOUNT_NAME=production
MAVEN_VERSION=M390
```

### Multi-Cloud Setup

For organizations using multiple cloud providers:

**AWS:**

```bash
DEPLOYMENT_ENV=aws
ACCOUNT_NAME=aws-prod-east
```

**Azure:**

```bash
DEPLOYMENT_ENV=azure
ACCOUNT_NAME=azure-prod-west
```

**GCP:**

```bash
DEPLOYMENT_ENV=gcp
ACCOUNT_NAME=gcp-prod-us
```

**On-Premise:**

```bash
DEPLOYMENT_ENV=on-prem
ACCOUNT_NAME=datacenter-01
```

## Prerequisites

### 1. Jenkins Shared Library

The pipeline requires a Jenkins shared library named `global_shared_library`.

**Setup:**

1. Go to "Manage Jenkins" → "Configure System"
2. Scroll to "Global Pipeline Libraries"
3. Add library:
   - Name: `global_shared_library`
   - Default version: `main`
   - Retrieval method: Modern SCM
   - Source: Your shared library Git repository

**Alternative:** If you don't have a shared library, you can replace the Jenkinsfile with a standard declarative pipeline. See `jenkins/STANDARD_PIPELINE.md` for examples.

### 2. Maven Configuration

Ensure Maven is configured in Jenkins:

1. Go to "Manage Jenkins" → "Global Tool Configuration"
2. Find "Maven" section
3. Add Maven installation:
   - Name: Should match `MAVEN_VERSION` (e.g., `M390`, `Maven-3.9`)
   - Install automatically: Yes (or specify MAVEN_HOME)
   - Version: 3.9.0 or higher

### 3. Jenkins Agent Labels

Configure agents with appropriate labels:

1. Go to "Manage Jenkins" → "Manage Nodes and Clouds"
2. Select your agent
3. Add label matching `JENKINS_AGENT` value
4. Ensure agent has:
   - Java 17 installed
   - Maven installed (or use tool installation)
   - Docker installed (for building images)

## Troubleshooting

### Error: "No agent with label 'maven-instances' found"

**Solution:** Either:

- Create/configure an agent with label `maven-instances`, OR
- Set `JENKINS_AGENT` environment variable to match your actual agent label

### Error: "Maven tool 'M390' does not exist"

**Solution:** Either:

- Configure Maven tool in Jenkins with name `M390`, OR
- Set `MAVEN_VERSION` environment variable to match your Maven tool name

### Error: "Shared library 'global_shared_library' not found"

**Solution:** Either:

- Configure the shared library in Jenkins (see Prerequisites section), OR
- Replace Jenkinsfile with standard pipeline (see `jenkins/STANDARD_PIPELINE.md`)

## Customization for Your Organization

### Using Different Agent Labels

If your organization uses different agent naming:

```bash
# For agents labeled 'java-build-agents'
JENKINS_AGENT=java-build-agents

# For agents labeled 'docker-maven'
JENKINS_AGENT=docker-maven
```

### Using Different Maven Versions

If you have multiple Maven versions:

```bash
# For Maven 3.8.x
MAVEN_VERSION=Maven-3.8

# For Maven 3.9.x
MAVEN_VERSION=Maven-3.9

# For latest Maven
MAVEN_VERSION=maven-latest
```

### Custom Namespaces per Branch

You can use Jenkins environment variables to customize per branch:

```groovy
K8S_NAMESPACE = "${env.BRANCH_NAME}-currency-converter"
// main → main-currency-converter
// develop → develop-currency-converter
```

## Best Practices

1. **Use Job Parameters**: Make configuration explicit and visible in job UI
2. **Document Defaults**: Keep this documentation updated with your defaults
3. **Environment-Specific Jobs**: Create separate jobs for dev/staging/prod with appropriate configs
4. **Version Control**: Store job configurations in JCasC or Job DSL
5. **Test Configuration**: Always test pipeline changes in non-production first

## Example: Complete Job Configuration

Here's a complete example configuring a Jenkins multibranch pipeline:

```groovy
// In Jenkins Job DSL or JCasC
multibranchPipelineJob('currency-converter-main-service') {
    branchSources {
        git {
            remote('https://github.com/your-org/Spring-Boot-Currency-Converter.git')
            credentialsId('github-credentials')
        }
    }

    factory {
        workflowBranchProjectFactory {
            scriptPath('main-service/Jenkinsfile')
        }
    }

    configure { node ->
        node / sources / 'data' / 'jenkins.branch.BranchSource' / source / traits {
            // Add environment variables
            'org.jenkinsci.plugins.workflow.multibranch.extended.EnvironmentFilterTrait' {
                environmentFilter {
                    // Development branches
                    environmentContributor {
                        name('develop')
                        environmentVariables {
                            entry {
                                key('JENKINS_AGENT')
                                value('dev-agents')
                            }
                            entry {
                                key('K8S_NAMESPACE')
                                value('dev-currency-converter')
                            }
                            entry {
                                key('ACCOUNT_NAME')
                                value('development')
                            }
                        }
                    }
                    // Production branches
                    environmentContributor {
                        name('main')
                        environmentVariables {
                            entry {
                                key('JENKINS_AGENT')
                                value('prod-agents')
                            }
                            entry {
                                key('K8S_NAMESPACE')
                                value('currency-converter')
                            }
                            entry {
                                key('ACCOUNT_NAME')
                                value('production')
                            }
                        }
                    }
                }
            }
        }
    }
}
```

## Support

For issues or questions:

- Check Jenkins logs: `$JENKINS_HOME/jobs/<job-name>/builds/<build-number>/log`
- Verify environment variables: Add `printenv` step in Jenkinsfile
- Review shared library code: Check your `global_shared_library` repository
