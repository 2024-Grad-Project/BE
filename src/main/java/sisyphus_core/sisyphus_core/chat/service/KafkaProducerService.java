package sisyphus_core.sisyphus_core.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import sisyphus_core.sisyphus_core.chat.model.Message;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {

    @Value("${kafka.topic.name}")
    private String TOPIC_NAME;

    private final KafkaTemplate<String, Message> kafkaTemplate;

    public void sendMessage(Message message) {
        log.info("kafka send message : {}", message);
        kafkaTemplate.send(TOPIC_NAME, message);
    }
}
