package org.example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.apache.poi.ss.usermodel.Workbook;
import org.example.service.ExcelService;
import org.example.service.PrometheusService;
import org.example.service.S3Service;
import org.example.service.SlackService;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class PrometheusRequestLatencyReportExporter implements RequestHandler<Map<String, Object>, String> {

    @Override
    public String handleRequest(Map<String, Object> input, Context context) {
        return start();
    }

    public String start() {
        try {
            String[] applications = {
                    "MonitoringCommunicationApplication",
                    "MonitoringPersistenceApplication",
                    "MonitoringUserConsumerApplication"
            };
            String[] statuses = {"2..", "4..", "5.."};

            PrometheusService prometheusService = new PrometheusService();
            ExcelService excelService = new ExcelService();
            S3Service s3Service = new S3Service();
            SlackService slackService = new SlackService();

            Workbook workbook = excelService.createWorkbook();
            String excelFileName = excelService.getExcelFileName();

            for (String application : applications) {
                for (String status : statuses) {
                    String httpRequestLatencyUrl = prometheusService.buildHttpRequestLatencyUrl(application, status);
                    String httpRequestLatencyResponse = prometheusService.getResponse(httpRequestLatencyUrl);
                    if (httpRequestLatencyResponse != null) {
                        String sheetName = application.replace("Monitoring", "").replace("Application", "") + "_" + status;
                        excelService.writeHttpRequestLatencyToExcel(workbook, sheetName, httpRequestLatencyResponse);
                    }
                }
            }

            for (String application : applications) {
                String repositoryRequestLatencyUrl = prometheusService.buildRepositoryRequestLatencyUrl(application);
                String repositoryRequestLatencyResponse = prometheusService.getResponse(repositoryRequestLatencyUrl);
                if (repositoryRequestLatencyResponse != null) {
                    String sheetName = application.replace("Monitoring", "").replace("Application", "") + "_" + "Repositories";
                    excelService.writeRepositoryRequestLatencyToExcel(workbook, sheetName, repositoryRequestLatencyResponse);
                }
            }

            excelService.saveWorkbook(workbook, excelFileName);

            s3Service.uploadFileToS3(excelFileName);

            String fileUrl = s3Service.getFileUrl(excelFileName);
            String currentDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            String slackMessage = String.format(
                    "✅ Done! The report for %s (last 24 hours) has been successfully generated and uploaded to S3.\n🔗 You can download it from the following link:\n%s",
                    currentDate, fileUrl
            );
            slackService.sendMessage(slackMessage);

            return "The Prometheus request latency report has been successfully generated and uploaded to S3. A notification has been sent to Slack.";
        } catch (Exception e) {
            e.printStackTrace();
            return "Error generating or uploading the report or sending the message to Slack.";
        }
    }
}
