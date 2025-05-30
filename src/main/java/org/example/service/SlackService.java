package org.example.service;

import com.slack.api.Slack;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import org.example.service.util.ConfigLoader;

import java.io.IOException;

public class SlackService {


    private static final String SLACK_REPORT_CHANNEL_NAME = "requests-latency-report";
    private static final String SLACK_BOT_TOKEN = ConfigLoader.get("SLACK_BOT_TOKEN");
    private final Slack slack = Slack.getInstance();

    public void sendMessage(String message) {
        try {
            ChatPostMessageResponse response = slack.methods(SLACK_BOT_TOKEN).chatPostMessage(req ->
                    req.channel(SLACK_REPORT_CHANNEL_NAME)
                            .text(message)
            );
        } catch (IOException | SlackApiException e) {
            e.printStackTrace();
        }
    }
}