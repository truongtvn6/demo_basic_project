package com.demobasic.notification.fcm;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class FcmSender {

    private static final Logger log = LoggerFactory.getLogger(FcmSender.class);

    private final ObjectProvider<FirebaseMessaging> messagingProvider;

    public FcmSender(ObjectProvider<FirebaseMessaging> messagingProvider) {
        this.messagingProvider = messagingProvider;
    }

    public FcmResult send(List<String> tokens, String title, String body, Map<String, String> data) {
        if (tokens == null || tokens.isEmpty()) {
            log.info("No tokens registered; skipping FCM send");
            return new FcmResult(0, 0, "no-tokens");
        }
        FirebaseMessaging messaging = messagingProvider.getIfAvailable();
        if (messaging == null) {
            log.warn("FCM disabled (demobasic.firebase.enabled=false); would send '{}' to {} token(s)",
                    title, tokens.size());
            return new FcmResult(0, 0, "fcm-disabled");
        }

        Map<String, String> safeData = data == null ? Map.of() : new HashMap<>(data);
        MulticastMessage message = MulticastMessage.builder()
                .addAllTokens(tokens)
                .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                .putAllData(safeData)
                .build();
        try {
            BatchResponse response = messaging.sendEachForMulticast(message);
            log.info("FCM sent: success={} failure={}", response.getSuccessCount(), response.getFailureCount());
            return new FcmResult(response.getSuccessCount(), response.getFailureCount(), null);
        } catch (Exception ex) {
            log.error("FCM send failed: {}", ex.getMessage());
            return new FcmResult(0, tokens.size(), ex.getMessage());
        }
    }

    public record FcmResult(int success, int failure, String errorMessage) {
        public boolean ok() {
            return errorMessage == null && failure == 0;
        }
    }
}
