package sisyphus_core.sisyphus_core.auth.model;

import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RedisHash(value = "email", timeToLive = 300)
public class Mail {

    @Id
    private String email;

    @Indexed
    private int authNumber;
}
