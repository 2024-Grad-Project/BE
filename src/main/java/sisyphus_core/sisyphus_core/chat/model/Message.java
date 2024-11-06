package sisyphus_core.sisyphus_core.chat.model;

import jakarta.persistence.Id;
import lombok.*;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;
import sisyphus_core.sisyphus_core.chat.model.dto.MessageType;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@RedisHash(value = "message" , timeToLive = 86400)
public class Message {

    @Setter
    private MessageType type;

    @Id
    @Builder.Default
    private String id = UUID.randomUUID().toString();

    @Setter
    private String message;

    @Indexed
    @Setter
    private String senderName;

    @Indexed
    private Long roomId;

    @Builder.Default
    private ZonedDateTime createAt = ZonedDateTime.now(ZoneId.of("Asia/Seoul"));
}
