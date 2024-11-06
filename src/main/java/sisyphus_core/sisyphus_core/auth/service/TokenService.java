package sisyphus_core.sisyphus_core.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import sisyphus_core.sisyphus_core.auth.model.Token;

import java.util.LinkedHashMap;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private static final String TOKEN_PREFIX = "token:";
    private static final String LOGIN_PREFIX = "login:";

    public void saveToken(Token token) {
        String key = TOKEN_PREFIX + token.getAccessToken();
        String loginKey = LOGIN_PREFIX + token.getLoginId();
        redisTemplate.opsForValue().set(key, token);
        redisTemplate.opsForValue().set(loginKey, token);
    }

    public Token findToken(String loginId){
        String key = LOGIN_PREFIX + loginId;
        Object tokenData = redisTemplate.opsForValue().get(key);

        if(tokenData instanceof LinkedHashMap){
            return objectMapper.convertValue(tokenData, Token.class);
        }
        return (Token) tokenData;
    }

    public Token findTokenByToken(String accessToken){
        String key = TOKEN_PREFIX + accessToken;
        Object tokenData = redisTemplate.opsForValue().get(key);

        if(tokenData instanceof LinkedHashMap){
            return objectMapper.convertValue(tokenData, Token.class);
        }
        return (Token) tokenData;
    }

    public void deleteToken(String loginId) {
        String key = LOGIN_PREFIX + loginId;
        redisTemplate.delete(key);
    }

    public void updateAccessToken(String loginId, String newAccessToken) {
        String key = LOGIN_PREFIX + loginId;
        Token token = (Token) redisTemplate.opsForValue().get(key);
        if (token != null) {
            token.setAccessToken(newAccessToken);
            redisTemplate.opsForValue().set(key, token);
        }
    }
}
