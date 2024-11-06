package sisyphus_core.sisyphus_core.auth.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String loginId;
    private String email;
    private String nickname;
    private String name;
    private String profileImageUrl;
    private UserState state;
    private UserRole userRole;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class find{
        private String loginId;
        private String password;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class toChat{
        private String nickname;
        private String profileImageUrl;
        private Long id;
    }
}
