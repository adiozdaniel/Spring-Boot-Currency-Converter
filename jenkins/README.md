# Jenkins Infrastructure Configuration

This document provides comprehensive guidance for setting up Jenkins infrastructure for the Currency Converter microservices.

> **📖 For Jenkinsfile Configuration**: See [CONFIGURATION.md](./CONFIGURATION.md) for details on customizing pipeline parameters (agent labels, Maven versions, deployment environments, etc.)

## Prerequisites

- Jenkins 2.400+ (LTS recommended)
- Java 17+ installed on Jenkins server
- Maven 3.9+ installed on Jenkins server
- Docker installed on Jenkins build agents
- Kubernetes access (for deployment)

## Required Jenkins Plugins

### Essential Plugins

Install these plugins via Jenkins Plugin Manager (`Manage Jenkins > Manage Plugins`):

```txt
# Core Plugins
- Pipeline (workflow-aggregator)
- Pipeline: Stage View
- Git plugin
- GitHub plugin
- Credentials Binding
- SSH Agent

# Build Tools
- Maven Integration
- Pipeline Maven Integration

# Code Quality & Security
- SonarQube Scanner
- OWASP Dependency-Check
- JaCoCo

# Containerization
- Docker Pipeline
- Kubernetes
- Kubernetes CLI

# Notifications
- Slack Notification
- Email Extension
- Office 365 Connector (for Teams)

# Reporting
- HTML Publisher
- JUnit
- Performance Plugin
- Gatling Plugin

# Utilities
- Timestamper
- Build Timeout
- Workspace Cleanup
- Pipeline Utility Steps
```

### Install via Jenkins CLI

```bash
# Download Jenkins CLI
wget http://your-jenkins-url/jnlpJars/jenkins-cli.jar

# Install plugins
java -jar jenkins-cli.jar -s http://your-jenkins-url/ -auth admin:password install-plugin \
  workflow-aggregator \
  git \
  github \
  credentials-binding \
  ssh-agent \
  maven-plugin \
  pipeline-maven \
  sonar \
  dependency-check-jenkins-plugin \
  jacoco \
  docker-workflow \
  kubernetes \
  kubernetes-cli \
  slack \
  email-ext \
  htmlpublisher \
  junit \
  performance \
  gatling \
  timestamper \
  build-timeout \
  ws-cleanup \
  pipeline-utility-steps

# Restart Jenkins
java -jar jenkins-cli.jar -s http://your-jenkins-url/ -auth admin:password safe-restart
```

## Jenkins Configuration

### 1. Global Tool Configuration

Navigate to `Manage Jenkins > Global Tool Configuration`

#### JDK Configuration

```sh
Name: JDK-17
JAVA_HOME: /usr/lib/jvm/java-17-openjdk-amd64
Install automatically: No
```

#### Maven Configuration

```sh
Name: Maven-3.9
MAVEN_HOME: /usr/share/maven
Install automatically: No
```

#### Docker Configuration

```sh
Name: docker-latest
Install automatically: Yes
Docker version: latest
```

### 2. System Configuration

Navigate to `Manage Jenkins > System`

#### SonarQube Configuration

```sh
Name: SonarQube
Server URL: https://sonarcloud.io
Server authentication token: <Add from credentials>
```

#### Slack Configuration

```sh
Workspace: your-workspace
Credential: <Add Slack token>
Default channel: #ci-cd-notifications
```

#### Email Configuration

```sh
SMTP server: smtp.gmail.com
SMTP port: 587
Use SSL: No
Use TLS: Yes
SMTP Username: your-email@gmail.com
SMTP Password: <Add from credentials>
```

### 3. Credentials Configuration

Navigate to `Manage Jenkins > Credentials > System > Global credentials`

Add the following credentials:

#### GitHub Credentials

- **Kind**: Username with password or SSH Username with private key
- **ID**: `github-credentials`
- **Description**: GitHub Access Token
- **Username**: Your GitHub username
- **Password/Key**: GitHub Personal Access Token

#### Docker Registry Credentials

- **Kind**: Username with password
- **ID**: `docker-registry-credentials`
- **Description**: GitHub Container Registry
- **Username**: Your GitHub username
- **Password**: GitHub Personal Access Token (with read:packages, write:packages)

#### SonarQube Token

- **Kind**: Secret text
- **ID**: `sonarqube-token`
- **Description**: SonarQube Authentication Token
- **Secret**: Your SonarQube token

#### Slack Token

- **Kind**: Secret text
- **ID**: `slack-token`
- **Description**: Slack Integration Token
- **Secret**: Your Slack webhook URL or token

#### Kubernetes Config

- **Kind**: Secret file
- **ID**: `kubeconfig`
- **Description**: Kubernetes Configuration
- **File**: Upload your kubeconfig file

## Shared Library Configuration

### 1. Configure Shared Library

Navigate to `Manage Jenkins > System > Global Pipeline Libraries`

```txt
Name: global_shared_library
Default version: main
Load implicitly: No
Allow default version to be overridden: Yes
Include @Library changes in job recent changes: Yes

Retrieval method: Modern SCM
Source Code Management: Git
Project Repository: https://github.com/your-org/jenkins-shared-library.git
Credentials: github-credentials
```

### 2. Expected Shared Library Structure

Your shared library should have this structure:

```txt
jenkins-shared-library/
├── vars/
│   └── runPipeline.groovy
├── src/
│   └── com/
│       └── example/
│           └── pipeline/
│               ├── Build.groovy
│               ├── Test.groovy
│               ├── SecurityScan.groovy
│               └── Deploy.groovy
└── resources/
    └── pipeline-templates/
```

## Jenkins Pipeline Jobs

### Create Multibranch Pipeline Jobs

#### Main Service Pipeline

1. Navigate to Jenkins Dashboard
2. Click "New Item"
3. Enter name: `currency-converter-main-service`
4. Select "Multibranch Pipeline"
5. Configure:

```txt
Branch Sources:
  - GitHub
  - Repository: https://github.com/your-org/Spring-Boot-Currency-Converter
  - Credentials: github-credentials
  - Build Strategies:
    - Regular branches
    - Pull requests from origin and forks

Build Configuration:
  - Mode: by Jenkinsfile
  - Script Path: main-service/Jenkinsfile

Scan Repository Triggers:
  - Periodically if not otherwise run: 1 hour

Orphaned Item Strategy:
  - Discard old items: 7 days
```

#### Rate Service Pipeline

Repeat the above steps with:

- Name: `currency-converter-rate-service`
- Script Path: `rate-service/Jenkinsfile`

## Jenkins Agents Configuration

### 1. Maven Build Agent

Create a Jenkins agent with label `maven-instances`:

```yaml
# jenkins-maven-agent.yaml
apiVersion: v1
kind: Pod
metadata:
  labels:
    jenkins: agent
    jenkins/label: maven-instances
spec:
  containers:
  - name: maven
    image: maven:3.9-eclipse-temurin-17
    command:
    - cat
    tty: true
    volumeMounts:
    - name: maven-cache
      mountPath: /root/.m2
    resources:
      requests:
        memory: "2Gi"
        cpu: "1000m"
      limits:
        memory: "4Gi"
        cpu: "2000m"

  - name: docker
    image: docker:24-dind
    command:
    - dockerd
    - --host=unix:///var/run/docker.sock
    - --host=tcp://0.0.0.0:2375
    securityContext:
      privileged: true
    volumeMounts:
    - name: docker-storage
      mountPath: /var/lib/docker

  volumes:
  - name: maven-cache
    persistentVolumeClaim:
      claimName: maven-cache-pvc
  - name: docker-storage
    emptyDir: {}
```

Apply to Kubernetes:

```bash
kubectl apply -f jenkins-maven-agent.yaml -n jenkins
```

### 2. Configure Kubernetes Cloud in Jenkins

Navigate to `Manage Jenkins > Manage Nodes and Clouds > Configure Clouds`

```txt
Name: kubernetes
Kubernetes URL: https://kubernetes.default.svc.cluster.local
Kubernetes Namespace: jenkins
Credentials: kubeconfig
Jenkins URL: http://jenkins.jenkins.svc.cluster.local:8080

Pod Templates:
  - Name: maven-instances
  - Labels: maven-instances
  - Usage: Use this node as much as possible
  - Pod Template YAML: <paste jenkins-maven-agent.yaml content>
```

## Jenkins Job DSL

Create a seed job to automatically configure pipelines:

```groovy
// jobs/seed.groovy
multibranchPipelineJob('currency-converter-main-service') {
    displayName('Currency Converter - Main Service')
    description('Main service for currency conversion')

    branchSources {
        github {
            id('main-service-github')
            repoOwner('your-org')
            repository('Spring-Boot-Currency-Converter')
            credentialsId('github-credentials')

            buildOriginBranch(true)
            buildOriginBranchWithPR(true)
            buildOriginPRMerge(false)
            buildForkPRMerge(false)
        }
    }

    configure { node ->
        node / sources / 'data' / 'jenkins.branch.BranchSource' / source / traits {
            'jenkins.plugins.git.traits.BranchDiscoveryTrait' {}
            'jenkins.plugins.git.traits.OriginPullRequestDiscoveryTrait' {
                strategyId(1)
            }
        }
    }

    factory {
        workflowBranchProjectFactory {
            scriptPath('main-service/Jenkinsfile')
        }
    }

    orphanedItemStrategy {
        discardOldItems {
            daysToKeep(7)
        }
    }

    triggers {
        periodicFolderTrigger {
            interval('1h')
        }
    }
}

multibranchPipelineJob('currency-converter-rate-service') {
    displayName('Currency Converter - Rate Service')
    description('Rate service for exchange rates')

    branchSources {
        github {
            id('rate-service-github')
            repoOwner('your-org')
            repository('Spring-Boot-Currency-Converter')
            credentialsId('github-credentials')

            buildOriginBranch(true)
            buildOriginBranchWithPR(true)
            buildOriginPRMerge(false)
            buildForkPRMerge(false)
        }
    }

    factory {
        workflowBranchProjectFactory {
            scriptPath('rate-service/Jenkinsfile')
        }
    }

    orphanedItemStrategy {
        discardOldItems {
            daysToKeep(7)
        }
    }

    triggers {
        periodicFolderTrigger {
            interval('1h')
        }
    }
}
```

## Environment Variables

Set these environment variables in Jenkins:

```sh
DOCKER_REGISTRY=ghcr.io
SONAR_ORGANIZATION=currency-converter
SONAR_HOST_URL=https://sonarcloud.io
KUBERNETES_NAMESPACE=currency-converter
```

## Security Best Practices

1. **Enable CSRF Protection**: `Manage Jenkins > Security > Prevent Cross Site Request Forgery`
2. **Use Role-Based Access Control**: Install Role-based Authorization Strategy plugin
3. **Enable Audit Trail**: Install Audit Trail plugin
4. **Secure Credentials**: Never hardcode credentials in Jenkinsfile
5. **Restrict Script Approval**: `Manage Jenkins > In-process Script Approval`
6. **Enable Build Timeout**: Prevent runaway builds
7. **Limit Build History**: Configure build retention policies

## Monitoring Jenkins

### Prometheus Metrics

Install Prometheus plugin and configure:

```txt
Manage Jenkins > Configure System > Prometheus
- Enable: Yes
- Path: /prometheus
- Collecting metrics period: 120 seconds
```

### Health Checks

Monitor these endpoints:

- `http://jenkins-url/` - Jenkins home
- `http://jenkins-url/metrics` - Metrics endpoint
- `http://jenkins-url/prometheus` - Prometheus metrics

## Backup Configuration

### Backup Jenkins Home

```bash
#!/bin/bash
# jenkins-backup.sh

JENKINS_HOME=/var/lib/jenkins
BACKUP_DIR=/backup/jenkins
DATE=$(date +%Y%m%d-%H%M%S)

# Create backup
tar -czf "$BACKUP_DIR/jenkins-home-$DATE.tar.gz" \
  --exclude="$JENKINS_HOME/workspace" \
  --exclude="$JENKINS_HOME/builds/*/archive" \
  "$JENKINS_HOME"

# Keep only last 7 backups
find "$BACKUP_DIR" -name "jenkins-home-*.tar.gz" -mtime +7 -delete
```

Schedule via cron:

```cron
0 2 * * * /usr/local/bin/jenkins-backup.sh
```

## Troubleshooting

### Common Issues

#### Build Fails with "Maven not found"

```bash
# Fix: Ensure Maven is installed on agent
docker exec jenkins-agent which mvn
```

#### Pipeline Cannot Access Kubernetes

```bash
# Fix: Verify kubeconfig credential
kubectl get pods -n jenkins --kubeconfig=/path/to/kubeconfig
```

#### SonarQube Analysis Fails

```bash
# Fix: Check SonarQube token and project key
# Verify in Manage Jenkins > Configure System > SonarQube servers
```

### Logs Location

```bash
# Jenkins logs
tail -f /var/log/jenkins/jenkins.log

# Build logs
cat /var/lib/jenkins/jobs/<job-name>/builds/<build-number>/log
```

## Additional Resources

- [Jenkins Documentation](https://www.jenkins.io/doc/)
- [Pipeline Syntax Reference](https://www.jenkins.io/doc/book/pipeline/syntax/)
- [Plugin Index](https://plugins.jenkins.io/)
- [Shared Library Documentation](https://www.jenkins.io/doc/book/pipeline/shared-libraries/)
