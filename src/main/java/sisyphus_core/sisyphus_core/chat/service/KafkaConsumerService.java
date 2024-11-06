package sisyphus_core.sisyphus_core.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import sisyphus_core.sisyphus_core.chat.model.Message;

@Service
@Slf4j
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final RedisTemplate<String, Object> redisTemplate;

    @KafkaListener(topics = "ai-chat", groupId = "myGroup")
    public void consume(Message message) throws Exception{
        log.info("kafka receive message : {}" , message);
        saveMessageToRedis(message);
    }

    private void saveMessageToRedis(Message message) {
        String key = "room:" + message.getRoomId();
        redisTemplate.opsForList().leftPush(key, message);
    }
}
