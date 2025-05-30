package org.example.service;

import org.example.service.util.ConfigLoader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

public class PrometheusService {


    private static final String HTTP_REQUEST_LATENCY_QUERY = "irate(http_server_requests_seconds_sum{application=\"%s\", uri!~\".*actuator.*\", status=~\"%s\"}[1m]) / irate(http_server_requests_seconds_count{application=\"%s\", uri!~\".*actuator.*\", status=~\"%s\"}[1m])%s";
    private static final String REPOSITORY_REQUEST_LATENCY_QUERY = "irate(spring_data_repository_invocations_seconds_sum{application=\"%s\", uri!~\".*actuator.*\"}[1m]) / irate(spring_data_repository_invocations_seconds_count{application=\"%s\", uri!~\".*actuator.*\"}[1m]) > 0.2";
    private static final int PROMETHEUS_STEP = 60;
    private static final String PROMETHEUS_BASE_URL = ConfigLoader.get("prometheus.baseUrl");
    private final long startTime;
    private final long endTime;

    public PrometheusService() {
        this.startTime = Instant.now().getEpochSecond() - 86400;
        this.endTime = Instant.now().getEpochSecond();
    }

    public String getResponse(String url) throws IOException {
        try {
            URI uri = new URI(url);
            HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setRequestMethod("GET");
            return connection.getResponseCode() == HttpURLConnection.HTTP_OK ? extractResponseContent(connection) : null;
        } catch (URISyntaxException e) {
            throw new IOException("Invalid URL syntax: " + url, e);
        }
    }

    public String buildHttpRequestLatencyUrl(String application, String status) {
        String condition = "2..".equals(status) ? " > 0.5" : "";
        String query = String.format(HTTP_REQUEST_LATENCY_QUERY, application, status, application, status, condition);
        return String.format("%s?query=%s&start=%d&end=%d&step=%d",
                PROMETHEUS_BASE_URL,
                URLEncoder.encode(query, StandardCharsets.UTF_8),
                startTime,
                endTime,
                PROMETHEUS_STEP);
    }

    public String buildRepositoryRequestLatencyUrl(String application) {
        String query = String.format(REPOSITORY_REQUEST_LATENCY_QUERY, application, application);
        return String.format("%s?query=%s&start=%d&end=%d&step=%d",
                PROMETHEUS_BASE_URL,
                URLEncoder.encode(query, StandardCharsets.UTF_8),
                startTime,
                endTime,
                PROMETHEUS_STEP);
    }

    private String extractResponseContent(HttpURLConnection connection) throws IOException {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) response.append(inputLine);
            return response.toString();
        }
    }
}
