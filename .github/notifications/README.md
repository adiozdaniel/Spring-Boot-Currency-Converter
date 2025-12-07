# Notification Templates

This directory contains notification templates for Slack and Microsoft Teams that can be used with CI/CD pipelines.

## Template Variables

The following variables are used in the notification templates and should be replaced with actual values:

- `${SERVICE_NAME}` - Name of the service (main-service or rate-service)
- `${BUILD_STATUS}` - Status of the build (Success, Failed, etc.)
- `${BRANCH_NAME}` - Git branch name
- `${COMMIT_SHA}` - Git commit SHA (short)
- `${COMMIT_AUTHOR}` - Git commit author
- `${BUILD_NUMBER}` - CI/CD build number
- `${COMMIT_MESSAGE}` - Git commit message
- `${TESTS_PASSED}` - Number of tests passed
- `${TESTS_FAILED}` - Number of tests failed
- `${TESTS_SKIPPED}` - Number of tests skipped
- `${COVERAGE_PERCENTAGE}` - Code coverage percentage
- `${BUILD_URL}` - URL to the build
- `${COMMIT_URL}` - URL to the commit
- `${THEME_COLOR}` - Color theme (0076D7 for success, D73A49 for failure)

## Usage with GitHub Actions

### Slack Notifications

1. Add the Slack webhook URL as a GitHub secret: `SLACK_WEBHOOK_URL`

2. Add this step to your workflow:

```yaml
- name: Send Slack notification
  if: always()
  env:
    SLACK_WEBHOOK_URL: ${{ secrets.SLACK_WEBHOOK_URL }}
  run: |
    # Replace variables in template
    sed -e "s/\${SERVICE_NAME}/${{ env.SERVICE_NAME }}/g" \
        -e "s/\${BUILD_STATUS}/${{ job.status }}/g" \
        -e "s/\${BRANCH_NAME}/${{ github.ref_name }}/g" \
        -e "s/\${COMMIT_SHA}/${{ github.sha }}/g" \
        -e "s/\${COMMIT_AUTHOR}/${{ github.actor }}/g" \
        -e "s/\${BUILD_NUMBER}/${{ github.run_number }}/g" \
        -e "s/\${COMMIT_MESSAGE}/${{ github.event.head_commit.message }}/g" \
        -e "s/\${BUILD_URL}/${{ github.server_url }}/${{ github.repository }}/actions/runs/${{ github.run_id }}/g" \
        -e "s/\${COMMIT_URL}/${{ github.server_url }}/${{ github.repository }}/commit/${{ github.sha }}/g" \
        .github/notifications/slack-notification.json | \
    curl -X POST -H 'Content-type: application/json' \
         --data @- $SLACK_WEBHOOK_URL
```

### Microsoft Teams Notifications

1. Add the Teams webhook URL as a GitHub secret: `TEAMS_WEBHOOK_URL`

2. Add this step to your workflow:

```yaml
- name: Send Teams notification
  if: always()
  env:
    TEAMS_WEBHOOK_URL: ${{ secrets.TEAMS_WEBHOOK_URL }}
    THEME_COLOR: ${{ job.status == 'success' && '0076D7' || 'D73A49' }}
  run: |
    # Replace variables in template
    sed -e "s/\${SERVICE_NAME}/${{ env.SERVICE_NAME }}/g" \
        -e "s/\${BUILD_STATUS}/${{ job.status }}/g" \
        -e "s/\${BRANCH_NAME}/${{ github.ref_name }}/g" \
        -e "s/\${COMMIT_SHA}/${{ github.sha }}/g" \
        -e "s/\${COMMIT_AUTHOR}/${{ github.actor }}/g" \
        -e "s/\${BUILD_NUMBER}/${{ github.run_number }}/g" \
        -e "s/\${COMMIT_MESSAGE}/${{ github.event.head_commit.message }}/g" \
        -e "s/\${BUILD_URL}/${{ github.server_url }}/${{ github.repository }}/actions/runs/${{ github.run_id }}/g" \
        -e "s/\${COMMIT_URL}/${{ github.server_url }}/${{ github.repository }}/commit/${{ github.sha }}/g" \
        -e "s/\${THEME_COLOR}/$THEME_COLOR/g" \
        .github/notifications/teams-notification.json | \
    curl -X POST -H 'Content-type: application/json' \
         --data @- $TEAMS_WEBHOOK_URL
```

## Usage with Jenkins

### Jenkins Slack Notifications

Add this to your Jenkinsfile (requires Slack Notification plugin):

```groovy
post {
    always {
        script {
            def payload = readFile('.github/notifications/slack-notification.json')
            payload = payload
                .replace('${SERVICE_NAME}', env.SERVICE_NAME)
                .replace('${BUILD_STATUS}', currentBuild.result)
                .replace('${BRANCH_NAME}', env.BRANCH_NAME)
                .replace('${COMMIT_SHA}', env.GIT_COMMIT.take(7))
                .replace('${COMMIT_AUTHOR}', env.GIT_AUTHOR_NAME)
                .replace('${BUILD_NUMBER}', env.BUILD_NUMBER)
                .replace('${COMMIT_MESSAGE}', env.GIT_COMMIT_MSG)
                .replace('${BUILD_URL}', env.BUILD_URL)
                .replace('${COMMIT_URL}', env.GIT_URL + '/commit/' + env.GIT_COMMIT)

            slackSend(
                channel: '#ci-cd-notifications',
                attachments: payload
            )
        }
    }
}
```

### Jenkins Microsoft Teams Notifications

Add this to your Jenkinsfile (requires Office 365 Connector plugin):

```groovy
post {
    always {
        script {
            def themeColor = currentBuild.result == 'SUCCESS' ? '0076D7' : 'D73A49'
            def payload = readFile('.github/notifications/teams-notification.json')
            payload = payload
                .replace('${SERVICE_NAME}', env.SERVICE_NAME)
                .replace('${BUILD_STATUS}', currentBuild.result)
                .replace('${BRANCH_NAME}', env.BRANCH_NAME)
                .replace('${COMMIT_SHA}', env.GIT_COMMIT.take(7))
                .replace('${COMMIT_AUTHOR}', env.GIT_AUTHOR_NAME)
                .replace('${BUILD_NUMBER}', env.BUILD_NUMBER)
                .replace('${COMMIT_MESSAGE}', env.GIT_COMMIT_MSG)
                .replace('${BUILD_URL}', env.BUILD_URL)
                .replace('${COMMIT_URL}', env.GIT_URL + '/commit/' + env.GIT_COMMIT)
                .replace('${THEME_COLOR}', themeColor)

            office365ConnectorSend(
                webhookUrl: env.TEAMS_WEBHOOK_URL,
                message: payload
            )
        }
    }
}
```

## Setup Instructions

### For Slack

1. Create an incoming webhook in your Slack workspace:
   - Go to <https://api.slack.com/apps>
   - Create a new app or select existing
   - Enable "Incoming Webhooks"
   - Add new webhook to workspace
   - Copy the webhook URL

2. Add the webhook URL to your CI/CD system:
   - **GitHub**: Add as repository secret named `SLACK_WEBHOOK_URL`
   - **Jenkins**: Add as credential and reference in pipeline

### For Microsoft Teams

1. Create an incoming webhook in your Teams channel:
   - Go to the Teams channel
   - Click "..." → "Connectors"
   - Search for "Incoming Webhook"
   - Configure and copy the webhook URL

2. Add the webhook URL to your CI/CD system:
   - **GitHub**: Add as repository secret named `TEAMS_WEBHOOK_URL`
   - **Jenkins**: Add as credential and reference in pipeline

## Customization

You can customize the notification templates by editing the JSON files:

- `slack-notification.json` - Slack notification format
- `teams-notification.json` - Microsoft Teams notification format

The templates use Slack Block Kit and Teams MessageCard formats respectively.
