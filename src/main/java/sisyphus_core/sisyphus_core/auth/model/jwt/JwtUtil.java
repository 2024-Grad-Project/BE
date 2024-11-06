package sisyphus_core.sisyphus_core.auth.model.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import sisyphus_core.sisyphus_core.auth.model.CustomUserDetails;
import sisyphus_core.sisyphus_core.auth.service.CustomUserDetailService;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {

    private final CustomUserDetailService userDetailService;
    private final SecretKey secretKey;
    private final CustomUserDetailService customUserDetailService;

    @Value("${spring.jwt.access.token}")
    private long ACCESS_TOKEN_EXPIRE_TIME;

    @Value("${spring.jwt.refresh.token}")
    private long REFRESH_TOKEN_EXPIRE_TIME;

    public JwtUtil(@Value("${spring.jwt.secretKey}") String secretKey, CustomUserDetailService userDetailService, CustomUserDetailService customUserDetailService) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64URL.decode(secretKey));
        this.userDetailService = userDetailService;
        this.customUserDetailService = customUserDetailService;
    }

    //token생성
    public String createToken(CustomUserDetails userDetails, long expireTime){
        return Jwts.builder()
                .setHeaderParam("typ", "JWT")
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expireTime))
                .signWith(secretKey)
                .compact();
    }

    //accessToken생성
    public String getAccessToken(CustomUserDetails userDetails){
        return createToken(userDetails, ACCESS_TOKEN_EXPIRE_TIME);
    }

    //refreshToken생성
    public String getRefreshToken(CustomUserDetails userDetails) {
        return createToken(userDetails, REFRESH_TOKEN_EXPIRE_TIME);
    }

    //token으로 유저 loginId조회
    public String getLoginId(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
    }

    //인증 객체 생성
    public Authentication getAuthentication(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();

        CustomUserDetails userDetails = (CustomUserDetails) customUserDetailService.loadUserByUsername(claims.getSubject());

        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    //유효한 토큰인지 확인
    public Boolean isExpired(String token) {
        try{
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            return claims.getExpiration().before(new Date());
        } catch (Exception e){
            return true;
        }
    }
}
