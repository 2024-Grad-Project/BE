package sisyphus_core.sisyphus_core.auth;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.web.multipart.MultipartFile;
import sisyphus_core.sisyphus_core.auth.exception.DuplicateUserLoginIdException;
import sisyphus_core.sisyphus_core.auth.exception.DuplicateUserNicknameException;
import sisyphus_core.sisyphus_core.auth.model.Token;
import sisyphus_core.sisyphus_core.auth.model.User;
import sisyphus_core.sisyphus_core.auth.model.UserFollower;
import sisyphus_core.sisyphus_core.auth.model.UserFollowing;
import sisyphus_core.sisyphus_core.auth.model.dto.TokenResponse;
import sisyphus_core.sisyphus_core.auth.model.dto.UserRequest;
import sisyphus_core.sisyphus_core.auth.model.dto.UserResponse;
import sisyphus_core.sisyphus_core.auth.repository.UserFollowerRepository;
import sisyphus_core.sisyphus_core.auth.repository.UserFollowingRepository;
import sisyphus_core.sisyphus_core.auth.service.TokenService;
import sisyphus_core.sisyphus_core.auth.service.UserService;
import sisyphus_core.sisyphus_core.chat.model.ChatRoom;
import sisyphus_core.sisyphus_core.chat.model.dto.ChatRoomRequest;
import sisyphus_core.sisyphus_core.chat.service.ChatRoomService;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Slf4j
public class AuthTest {

    @Autowired
    private UserService userService;

    @Autowired
    private ChatRoomService chatRoomService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private UserFollowerRepository userFollowerRepository;

    @Autowired
    private UserFollowingRepository userFollowingRepository;

    @BeforeEach
    void before(){
        UserRequest.register register1= UserRequest.register.builder()
                .loginId("테스트1")
                .password("1234")
                .nickname("test1")
                .email("test1@test.com")
                .name("test1")
                .build();

        UserRequest.register register2= UserRequest.register.builder()
                .loginId("테스트2")
                .password("1234")
                .nickname("test2")
                .email("test2@test.com")
                .name("test2")
                .build();

        UserRequest.register register3= UserRequest.register.builder()
                .loginId("테스트3")
                .password("1234")
                .nickname("test3")
                .email("test3@test.com")
                .name("test3")
                .build();

        userService.register(register1);
        userService.register(register2);
        userService.register(register3);
    }

    @AfterEach
    void after(){
        userService.deleteAll();
    }

    @Test
    @DisplayName("유저 회원가입")
    void userRegister(){
        UserRequest.register register4= UserRequest.register.builder()
                .loginId("테스트4")
                .password("1234")
                .nickname("test4")
                .email("test4@test.com")
                .name("test4")
                .build();

        userService.register(register4);

        User test3 = userService.findByNickname("test4");
        List<UserResponse> allUser = userService.findAllUser();
        assertThat(test3.getLoginId()).isEqualTo("테스트4");
        assertThat(allUser.size()).isEqualTo(4);
    }

    @Test
    @DisplayName("중복 로그인 아이디 체크")
    void userRegisterDuplicateLoginId(){
        assertThatThrownBy(() -> userService.checkDuplicateLoginId("테스트1"))
                .isInstanceOf(DuplicateUserLoginIdException.class)
                .hasMessage("이미 존재하는 아이디입니다.");
    }

    @Test
    @DisplayName("중복 닉네임 체크")
    void userRegisterDuplicateNickname(){
        assertThatThrownBy(() -> userService.checkDuplicateNickname("test2"))
                .isInstanceOf(DuplicateUserNicknameException.class)
                .hasMessage("이미 존재하는 닉네임입니다.");
    }

    @Test
    @WithMockUser(username = "테스트1", roles = "GENERAL")
    @DisplayName("유저 로그인")
    void userLogin(){
        UserRequest.login login = UserRequest.login.builder().loginId("테스트1").password("1234").build();

        HttpHeaders headers = new HttpHeaders();
        HttpEntity<UserRequest.login> request = new HttpEntity<>(login, headers);

        ResponseEntity<TokenResponse> response = testRestTemplate.postForEntity("/api/user/login", request, TokenResponse.class);
        String accessToken = response.getBody().getAccessToken();
        Token token = tokenService.findToken(login.getLoginId());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        assertThat(token.getAccessToken()).isEqualTo(accessToken);
        assertThat(authentication).isNotNull();
        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getName()).isEqualTo(login.getLoginId());
    }

    @Test
    @DisplayName("유저 로그아웃")
    void userLogout() {
        UserRequest.login login = UserRequest.login.builder().loginId("테스트1").password("1234").build();

        HttpHeaders headers = new HttpHeaders();
        HttpEntity<UserRequest.login> request = new HttpEntity<>(login, headers);

        ResponseEntity<TokenResponse> response = testRestTemplate.postForEntity("/api/user/login", request, TokenResponse.class);
        String accessToken = response.getBody().getAccessToken();
        HttpHeaders headers1 = new HttpHeaders();
        headers1.setBearerAuth(accessToken);

        HttpEntity<Object> request1 = new HttpEntity<>(null, headers1);
        ResponseEntity<String> response1 = testRestTemplate.exchange("/api/user/logout", HttpMethod.GET, request1, String.class);
        Token token = tokenService.findToken(login.getLoginId());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        assertThat(response1.getBody().toString()).isEqualTo("로그아웃에 성공하였습니다.");
        assertThat(token).isNull();
        assertThat(authentication).isNull();
    }

    @Test
    @DisplayName("회원 정보 변경")
    void userModify(){
        UserRequest.modify modify = UserRequest.modify.builder()
                .password("12345")
                .profileImageUrl("https://search.pstatic.net/common/?src=http%3A%2F%2Fblogfiles.naver.net%2FMjAyNDA5MDVfMTYw%2FMDAxNzI1NDcxNTA3MDEw.7GrjsYaNU2A9K2E-Wc4tDnK5x1AEotXYmtMgblobJyAg.OWcwAGjb2UML-AfJFz74QZWK1hOgl_nsgz90APbzbNMg.PNG%2F00-01-.png&type=a340")
                .email("test10@test.com")
                .describe("describe")
                .nickname("테스트10").build();
        userService.modify(modify, "테스트1");

        User test10 = userService.findByNickname("테스트10");
        assertThat(test10).isNotNull();
        assertThat(test10.getEmail()).isEqualTo("test10@test.com");
        assertThat(test10.getDescribe()).isEqualTo("describe");
        assertThat(test10.getPassword()).isEqualTo("12345");
        assertThat(test10.getProfileImageUrl()).isEqualTo("https://search.pstatic.net/common/?src=http%3A%2F%2Fblogfiles.naver.net%2FMjAyNDA5MDVfMTYw%2FMDAxNzI1NDcxNTA3MDEw.7GrjsYaNU2A9K2E-Wc4tDnK5x1AEotXYmtMgblobJyAg.OWcwAGjb2UML-AfJFz74QZWK1hOgl_nsgz90APbzbNMg.PNG%2F00-01-.png&type=a340");
    }

    @Test
    @DisplayName("회원 탈퇴")
    void userWithdrawal(){
        userService.follow("테스트1","test2");
        userService.follow("테스트1", "test3");

        User test1 = userService.findByNickname("test1");
        User test2 = userService.findByNickname("test2");
        User test3 = userService.findByNickname("test3");

        userService.withdrawal("테스트1");
        List<UserFollowing> test1FollowingUser = userFollowingRepository.findByUser(test1);
        List<UserFollower> test2FollowerUser = userFollowerRepository.findByUser(test2);
        List<UserFollower> test3FollowerUser = userFollowerRepository.findByUser(test3);

        List<UserResponse> allUser = userService.findAllUser();
        assertThat(allUser.size()).isEqualTo(2);
        assertThat(test1FollowingUser.size()).isEqualTo(0);
        assertThat(test2FollowerUser.size()).isEqualTo(0);
        assertThat(test3FollowerUser.size()).isEqualTo(0);
    }

    @Test
    @DisplayName("회원 탈퇴 채팅방 정보 번경")
    void updateChatRoomByUserWithdrawal(){
        String[] nicknames = new String[]{"test2"};
        ChatRoomRequest.register chatRegister = ChatRoomRequest.register.builder()
                .nicknames(nicknames)
                .type("open")
                .build();
        MultipartFile file =null;
        chatRoomService.createChatRoom(chatRegister, file,"테스트1");


        userService.withdrawal("테스트1");
        ChatRoom room = chatRoomService.findByRoomName("test2");
        assertThat(room.getUserCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("회원 아이디 찾기")
    void findUserLoginId(){
        UserResponse.find find = userService.findLoginId("test1@test.com");

        assertThat(find.getLoginId()).isEqualTo("테스트1");
    }

    @Test
    @DisplayName("회원 비밀번호 찾기")
    void findUserPassword(){
        UserResponse.find find = userService.findPassword("테스트2");
        assertThat(find.getPassword()).isEqualTo("1234");
    }

    @Test
    @DisplayName("유저 팔로우/언팔로우")
    void followUser(){
        userService.follow("테스트1", "test2");
        userService.follow("테스트1", "test3");

        User test1 = userService.findByNickname("test1");
        User test2 = userService.findByNickname("test2");
        User test3 = userService.findByNickname("test3");
        List<UserFollowing> test1FollowingUser = userFollowingRepository.findByUser(test1);
        List<UserFollower> test2FollowerUser = userFollowerRepository.findByUser(test2);
        List<UserFollower> test3FollowerUser = userFollowerRepository.findByUser(test3);

        assertThat(test1FollowingUser.size()).isEqualTo(2);
        assertThat(test2FollowerUser.size()).isEqualTo(1);
        assertThat(test3FollowerUser.size()).isEqualTo(1);

        userService.unfollow("테스트1", "test2");
        List<UserFollowing> test1FollowingUser2 = userFollowingRepository.findByUser(test1);
        List<UserFollower> test2FollowerUser2 = userFollowerRepository.findByUser(test2);
        List<UserFollower> test3FollowerUser2 = userFollowerRepository.findByUser(test3);

        assertThat(test1FollowingUser2.size()).isEqualTo(1);
        assertThat(test2FollowerUser2.size()).isEqualTo(0);
        assertThat(test3FollowerUser2.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("유저 팔로워/팔로잉 목록 조회")
    void followListUser(){
        userService.follow("테스트1", "test2");
        userService.follow("테스트1", "test3");
        userService.follow("테스트2", "test3");

        List<UserResponse> test1Followers = userService.findFollowerList("테스트1");
        List<UserResponse> test1Followings = userService.findFollowingList("테스트1");
        List<UserResponse> test2Followers = userService.findFollowerList("테스트2");
        List<UserResponse> test2Followings = userService.findFollowingList("테스트2");
        List<UserResponse> test3Followers = userService.findFollowerList("테스트3");
        List<UserResponse> test3Followings = userService.findFollowingList("테스트3");

        assertThat(test1Followers.size()).isEqualTo(0);
        assertThat(test1Followings.size()).isEqualTo(2);
        assertThat(test2Followers.size()).isEqualTo(1);
        assertThat(test2Followings.size()).isEqualTo(1);
        assertThat(test3Followers.size()).isEqualTo(2);
        assertThat(test3Followings.size()).isEqualTo(0);
    }
}
