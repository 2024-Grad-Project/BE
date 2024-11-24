package sisyphus_core.sisyphus_core.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import sisyphus_core.sisyphus_core.auth.model.CustomUserDetails;
import sisyphus_core.sisyphus_core.auth.model.User;
import sisyphus_core.sisyphus_core.auth.model.dto.OAuth2UserResponse;
import sisyphus_core.sisyphus_core.auth.model.dto.UserRequest;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OAuth2UserService extends DefaultOAuth2UserService {

    private final UserService userService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        log.info("oAuth2user : {}", oAuth2User);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2UserResponse oAuth2UserResponse = new OAuth2UserResponse(oAuth2User.getAttributes());

        // 사용자 정보 추출
        UserRequest.register userInfo = extractUserInfo(registrationId, oAuth2UserResponse);

        // 사용자 등록 또는 검색
        return registerOrFindUser(userInfo);
    }

    private UserRequest.register extractUserInfo(String registrationId, OAuth2UserResponse oAuth2UserResponse) {
        String nickname = "";
        String loginId = "";
        String profileImage = "";
        String name = "";
        String phoneNumber = "";

        switch (registrationId) {
            case "naver":
                nickname = oAuth2UserResponse.getNaverNickname();
                loginId = oAuth2UserResponse.getNaverEmail();
                profileImage = oAuth2UserResponse.getNaverProfileImage();
                name = oAuth2UserResponse.getNaverName();
                phoneNumber = oAuth2UserResponse.getNaverPhoneNumber();
                break;
            case "kakao":
                nickname = oAuth2UserResponse.getKakaoNickname();
                loginId = oAuth2UserResponse.getKakaoEmail();
                profileImage = oAuth2UserResponse.getKakaoProfileImage();
                name = nickname;
                break;
            case "google":
                nickname = oAuth2UserResponse.getGoogleName();
                loginId = oAuth2UserResponse.getGoogleEmail();
                profileImage = oAuth2UserResponse.getGoogleProfileImage();
                name = nickname;
                break;
            default:
                throw new IllegalArgumentException("Unsupported registrationId: " + registrationId);
        }

        return UserRequest.register.builder()
                .loginId(loginId)
                .nickname(nickname)
                .email(loginId)
                .name(name)
                .profileImageUrl(profileImage)
                .phoneNumber(phoneNumber)
                .password(UUID.randomUUID().toString()) // 비밀번호는 UUID로 설정
                .build();
    }

    private OAuth2User registerOrFindUser(UserRequest.register userInfo) {
        User existUser = userService.findByNickname(userInfo.getNickname());

        if (existUser == null) {
            userService.register(userInfo);
            User newUser = userService.findByNickname(userInfo.getNickname());
            return new CustomUserDetails(newUser);
        }

        return new CustomUserDetails(existUser);
    }
}
