package sisyphus_core.sisyphus_core.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import sisyphus_core.sisyphus_core.auth.handler.OAuth2SuccessHandler;
import sisyphus_core.sisyphus_core.auth.model.jwt.JwtFilter;
import sisyphus_core.sisyphus_core.auth.model.jwt.JwtUtil;
import sisyphus_core.sisyphus_core.auth.service.OAuth2UserService;
import sisyphus_core.sisyphus_core.auth.service.TokenService;

import java.util.Arrays;
import java.util.Collections;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtUtil jwtUtil;
    private final TokenService tokenService;
    private final OAuth2UserService oAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception{
        //web security setting
        http
                .httpBasic(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests ->
                        requests
                                .requestMatchers("/", "api/user/login", "/api/user/register" , "/api/user/check/loginId", "/api/user/check/nickname", "/api/user/find/loginId", "/api/user/find/password").permitAll()
                                .requestMatchers("/api/mail/send", "/api/mail/confirm","/api/chat/**", "/api/file/**").permitAll()
                                .requestMatchers("/chat/**").permitAll()
                                .requestMatchers("/api/user/login/naver", "/login/oauth2/code/naver", "/api/user/login/kakao", "/login/oauth2/code/kakao").permitAll()
                                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                                .anyRequest().authenticated())
                .oauth2Login(login -> login
                        .authorizationEndpoint(endpoint -> endpoint.baseUri("/api/user/login"))
                        .userInfoEndpoint(userInfoEndpointConfig -> userInfoEndpointConfig.userService(oAuth2UserService))
                        .successHandler(oAuth2SuccessHandler))
                .addFilterBefore(new JwtFilter(jwtUtil, tokenService), UsernamePasswordAuthenticationFilter.class);

        //Cors setting
        http
                .cors(cors ->
                        cors.configurationSource(request -> {
                            CorsConfiguration corsConfiguration = new CorsConfiguration();
                            corsConfiguration.setAllowedOrigins(Arrays.asList("http://localhost:5173"));
                            corsConfiguration.setAllowedMethods(Collections.singletonList("*"));
                            corsConfiguration.setAllowedHeaders(Collections.singletonList("*"));
                            corsConfiguration.setAllowCredentials(true);
                            corsConfiguration.setMaxAge(3000L);

                            return corsConfiguration;
                        }));

        return http.build();
    }
}
