package sisyphus_core.sisyphus_core.auth.model.dto;

import java.util.Map;

public class OAuth2UserResponse {

    private final Map<String, Object> properties;
    private final Map<String, Object> response;
    private final Map<String, Object> kakaoAccount;
    private final Map<String, Object> attribute;

    public OAuth2UserResponse(Map<String, Object> attribute) {
        this.properties = (Map<String, Object>) attribute.get("properties");
        this.response = (Map<String, Object>) attribute.get("response");
        this.kakaoAccount = (Map<String, Object>) attribute.get("kakao_account");
        this.attribute = attribute;
    }

    public String getKakaoNickname() {
        return properties.get("nickname").toString();
    }

    public String getKakaoProfileImage() {
        return properties.get("profile_image").toString();
    }

    public String getKakaoEmail(){ return kakaoAccount.get("email").toString();}

    public String getNaverNickname() { return response.get("nickname").toString();}

    public String getNaverProfileImage() {
        return response.get("profile_image").toString();
    }

    public String getNaverEmail() {
        return response.get("email").toString();
    }

    public String getNaverName() {
        return response.get("name").toString();
    }

    public String getNaverPhoneNumber(){
        return response.get("mobile").toString();
    }

    public String getGoogleEmail(){ return attribute.get("email").toString();}

    public String getGoogleProfileImage(){ return attribute.get("picture").toString();}

    public String getGoogleName(){ return attribute.get("name").toString();}
}
