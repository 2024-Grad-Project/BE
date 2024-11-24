package sisyphus_core.sisyphus_core.auth.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import sisyphus_core.sisyphus_core.auth.model.CustomUserDetails;
import sisyphus_core.sisyphus_core.auth.model.Token;
import sisyphus_core.sisyphus_core.auth.model.jwt.JwtUtil;
import sisyphus_core.sisyphus_core.auth.service.TokenService;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final TokenService tokenService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        CustomUserDetails userDetail =(CustomUserDetails) authentication.getPrincipal();

        String loginId = userDetail.getName();
        String nickname = userDetail.getNickname();

        String accessToken = jwtUtil.getAccessToken(userDetail);
        String refreshToken = jwtUtil.getRefreshToken(userDetail);
        Token token = Token.builder().accessToken(accessToken).refreshToken(refreshToken).loginId(loginId).build();
        tokenService.saveToken(token);

        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetail, null, userDetail.getAuthorities());
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);

        response.sendRedirect("http://localhost:5173/oauth2/redirect?token=" + accessToken + "&nickname=" + nickname);
    }

}