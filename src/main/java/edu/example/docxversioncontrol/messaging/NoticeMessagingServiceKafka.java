package edu.example.docxversioncontrol.messaging;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class NoticeMessagingServiceKafka {

    KafkaTemplate<Long, NotificationMessage > kafkaTemplate;

    public void sendFileURL(NotificationMessage message) {
        kafkaTemplate.send("notificationTopic", message);
    }
}
