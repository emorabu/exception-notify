package com.nolimit35.springkit.notification.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nolimit35.springkit.config.ExceptionNotifyProperties;
import com.nolimit35.springkit.formatter.NotificationFormatter;
import com.nolimit35.springkit.model.ExceptionInfo;
import com.nolimit35.springkit.notification.AbstractNotificationProvider;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * WeChat Work implementation of NotificationProvider
 */
@Slf4j
@Component
public class WeChatWorkNotificationProvider extends AbstractNotificationProvider {
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final NotificationFormatter formatter;
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    public WeChatWorkNotificationProvider(ExceptionNotifyProperties properties, NotificationFormatter formatter) {
        super(properties);
        this.httpClient = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
        this.formatter = formatter;
    }

    @Override
    protected boolean doSendNotification(ExceptionInfo exceptionInfo) throws Exception {
        String webhook = properties.getWechatwork().getWebhook();
        
        // Format the exception info into a notification
        String content = formatter.format(exceptionInfo);

        // Prepare request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("msgtype", "markdown");
        
        Map<String, String> markdown = new HashMap<>();
        markdown.put("content", content);
        requestBody.put("markdown", markdown);

        String jsonBody = objectMapper.writeValueAsString(requestBody);
        
        Request request = new Request.Builder()
            .url(webhook)
            .post(RequestBody.create(jsonBody, JSON))
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("Failed to send WeChat Work notification: {}", response.code());
                return false;
            }
            
            String responseBody = response.body().string();
            log.debug("WeChat Work response: {}", responseBody);
            return true;
        }
    }

    @Override
    public boolean isEnabled() {
        return properties.isEnabled() && 
               properties.getWechatwork().getWebhook() != null && 
               !properties.getWechatwork().getWebhook().isEmpty();
    }
} 