# Prometheus Request Latency Report Exporter

This Java Lambda application queries Prometheus for request latency metrics, generates an Excel report, uploads it to AWS S3, and sends a Slack notification with the report link.

## Features

- Collects metrics from Prometheus for multiple applications and HTTP statuses.
- Generates Excel reports with sorted latency data.
- Uploads the report to AWS S3.
- Sends a Slack message with the report link.

## Project Structure

- `PrometheusRequestLatencyReportExporter`: Entry point for the Lambda handler.
- `ExcelService`: Creates Excel reports from metrics.
- `PrometheusService`: Builds and executes Prometheus queries.
- `S3Service`: Uploads files to S3.
- `SlackService`: Sends messages to Slack.


## Configuration

Create a `config.properties` file based on the template:

```properties
prometheus.baseUrl=https://your-prometheus-url/api/v1/query_range
AWS_ACCESS_KEY=your-access-key
AWS_SECRET_KEY=your-secret-key
SLACK_BOT_TOKEN=xoxb-your-slack-bot-token
