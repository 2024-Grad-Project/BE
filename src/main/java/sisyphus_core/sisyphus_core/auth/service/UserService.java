package sisyphus_core.sisyphus_core.auth.service;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sisyphus_core.sisyphus_core.auth.exception.DuplicateUserException;
import sisyphus_core.sisyphus_core.auth.exception.DuplicateUserLoginIdException;
import sisyphus_core.sisyphus_core.auth.exception.DuplicateUserNicknameException;
import sisyphus_core.sisyphus_core.auth.exception.UserPasswordNotMatchException;
import sisyphus_core.sisyphus_core.auth.model.*;
import sisyphus_core.sisyphus_core.auth.model.dto.*;
import sisyphus_core.sisyphus_core.auth.model.jwt.JwtUtil;
import sisyphus_core.sisyphus_core.auth.repository.UserFollowerRepository;
import sisyphus_core.sisyphus_core.auth.repository.UserFollowingRepository;
import sisyphus_core.sisyphus_core.auth.repository.UserRepository;
import sisyphus_core.sisyphus_core.chat.model.ChatRoom;
import sisyphus_core.sisyphus_core.chat.model.UserChatRoom;
import sisyphus_core.sisyphus_core.chat.repository.ChatRoomRepository;
import sisyphus_core.sisyphus_core.chat.repository.UserChatRoomRepository;
import sisyphus_core.sisyphus_core.chat.service.ChatRoomService;
import sisyphus_core.sisyphus_core.chat.service.MessageService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final ChatRoomService chatRoomService;
    private final UserChatRoomRepository userChatRoomRepository;
    private final CustomUserDetailService customUserDetailService;
    private final TokenService tokenService;
    private final JwtUtil jwtUtil;
    private final ChatRoomRepository chatRoomRepository;
    private final MessageService messageService;
    private final SimpMessagingTemplate template;
    private final UserFollowingRepository userFollowingRepository;
    private final UserFollowerRepository userFollowerRepository;

    @Value("${default.profile.image.url}")
    private String defProfileImage;

    //유저 회원가입
    @Transactional
    public void register(UserRequest.register register) {
        if (userRepository.findByNickname(register.getNickname()).isPresent()) {
            throw new DuplicateUserException("이미 존재하는 아이디입니다.");
        }
        String profileImage = register.getProfileImageUrl();
        if(register.getProfileImageUrl() == null){
            profileImage = defProfileImage;
        }

        User user = User.builder()
                .loginId(register.getLoginId())
                .password(register.getPassword())
                .nickname(register.getNickname())
                .email(register.getEmail())
                .name(register.getName())
                .describe(" ")
                .phoneNumber(register.getPhoneNumber())
                .profileImageUrl(profileImage)
                .state(UserState.ACTIVE)
                .userRole(UserRole.GENERAL)
                .build();

        userRepository.save(user);
    }

    //로그인
    @Transactional
    public TokenResponse authenticate(UserRequest.login login, HttpServletResponse response) {
        CustomUserDetails userDetails = (CustomUserDetails) customUserDetailService.loadUserByUsername(login.getLoginId());
        User user = userRepository.findByLoginId(login.getLoginId()).orElseThrow(() -> new UsernameNotFoundException("일치하는 유저가 없습니다."));

        //로그인 비밀번호와 기존 비밀번호 검증
        if (!userDetails.getPassword().equals(login.getPassword())) {
            throw new UserPasswordNotMatchException("아이디와 비밀번호가 일치하지않습니다.");
        }

        String accessToken = jwtUtil.getAccessToken(userDetails);
        Token existingToken = tokenService.findToken(login.getLoginId());
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        response.addHeader("Authorization", "Bearer " + accessToken);
        if (existingToken == null) {
            String refreshToken = jwtUtil.getRefreshToken(userDetails);
            Token token = Token.builder().accessToken(accessToken).refreshToken(refreshToken).loginId(login.getLoginId()).build();
            tokenService.saveToken(token);
        } else {
            tokenService.updateAccessToken(existingToken.getRefreshToken(), accessToken);
        }
        user.setState(UserState.ACTIVE);

        List<UserFollower> userFollowerList = userFollowerRepository.findByUser(user);
        for (UserFollower userFollower : userFollowerList) {
            User followerUser = userFollower.getFollowerUser();
            template.convertAndSend("/queue/state/" + followerUser.getNickname(), user.getNickname());
        }
        return new TokenResponse(accessToken, user.getNickname());
    }

    //로그아웃
    @Transactional
    public void unAuthenticate(String loginId, HttpServletResponse response) {
        User user = userRepository.findByLoginId(loginId).orElseThrow(() -> new UsernameNotFoundException("일치하는 유저가 없습니다."));
        SecurityContextHolder.clearContext();
        tokenService.deleteToken(loginId);
        response.setHeader("Authorization", null);
        user.setState(UserState.NONACTIVE);
        List<UserFollower> userFollowerList = userFollowerRepository.findByUser(user);
        for (UserFollower userFollower : userFollowerList) {
            User followerUser = userFollower.getFollowerUser();
            template.convertAndSend("/queue/state/" + followerUser.getNickname());
        }
    }

    //회원 정보 변경
    @Transactional
    public void modify(UserRequest.modify modify, String loginId) {
        User user = userRepository.findByLoginId(loginId).orElseThrow(() -> new UsernameNotFoundException("일치하는 회원이 없습니다."));
        List<UserChatRoom> userChatRoomsByUser = userChatRoomRepository.findUserChatRoomsByUser(user);
        for (UserChatRoom userChatRoom : userChatRoomsByUser) {
            ChatRoom chatRoom = userChatRoom.getChatRoom();
            if (!chatRoom.isCustomRoomName()) {
                String replaceRoomName = chatRoom.getRoomName().replace(user.getNickname(), modify.getNickname());
                chatRoom.setRoomName(replaceRoomName);
                chatRoomRepository.save(chatRoom);
            }
            messageService.modifySenderName(user.getNickname(), modify.getNickname(), chatRoom.getChatRoomId());
        }

        user.modify(modify);
        userRepository.save(user);
    }

    //회원탈퇴
    @Transactional
    public void withdrawal(String loginId) {
        User user = userRepository.findByLoginId(loginId).orElseThrow(() -> new UsernameNotFoundException("일치하는 회원이 없습니다."));
        List<UserChatRoom> userChatRoomsByUser = userChatRoomRepository.findUserChatRoomsByUser(user);
        tokenService.deleteToken(loginId);
        userChatRoomRepository.deleteAllByUser(user);
        chatRoomService.withdrawalUser(userChatRoomsByUser, user.getNickname());
        userFollowingRepository.deleteByUser(user);
        userFollowerRepository.deleteByFollowerUser(user);
        userRepository.deleteByLoginId(loginId);
    }

    //아이디찾기
    @Transactional
    public UserResponse.find findLoginId(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("일치하는 유저가 없습니다."));
        return new UserResponse.find(user.getLoginId(), "");
    }

    //비밀번호찾기
    @Transactional
    public UserResponse.find findPassword(String loginId) {
        User user = userRepository.findByLoginId(loginId).orElseThrow(() -> new UsernameNotFoundException("일치하는 유저가 없습니다."));
        return new UserResponse.find("", user.getPassword());
    }

    //유저 로그인 아이디 중복 검증
    @Transactional
    public void checkDuplicateLoginId(String loginId) {
        if (userRepository.findByLoginId(loginId).isPresent()) {
            throw new DuplicateUserLoginIdException("이미 존재하는 아이디입니다.");
        }
    }

    //유저 닉네임 중복 검증
    @Transactional
    public void checkDuplicateNickname(String nickname) {
        if (userRepository.findByNickname(nickname).isPresent()) {
            throw new DuplicateUserNicknameException("이미 존재하는 닉네임입니다.");
        }
    }

    //팔로우
    @Transactional
    public void follow(String loginId, String nickname) {
        User user = userRepository.findByLoginId(loginId).orElseThrow(() -> new UsernameNotFoundException("일치하는 유저가없습니다."));
        User followUser = userRepository.findByNickname(nickname).orElseThrow(() -> new UsernameNotFoundException("일치하는 유저가 없습니다."));

        UserFollowing userFollowing = UserFollowing.builder().user(user).followingUser(followUser).build();
        UserFollower userFollower = UserFollower.builder().user(followUser).followerUser(user).build();
        userFollowingRepository.save(userFollowing);
        userFollowerRepository.save(userFollower);
    }

    //언팔로우
    @Transactional
    public void unfollow(String loginId, String nickname) {
        User user = userRepository.findByLoginId(loginId).orElseThrow(() -> new UsernameNotFoundException("일치하는 유저가없습니다."));
        User followUser = userRepository.findByNickname(nickname).orElseThrow(() -> new UsernameNotFoundException("일치하는 유저가 없습니다."));

        userFollowingRepository.deleteByUserAndFollowingUser(user, followUser);
        userFollowerRepository.deleteByUserAndFollowerUser(followUser, user);
    }

    //팔로워유저 조회
    @Transactional
    public List<UserResponse> findFollowerList(String loginId) {
        User user = userRepository.findByLoginId(loginId).orElseThrow(() -> new UsernameNotFoundException("일치하는 유저가 없습니다."));
        List<UserFollower> userFollowerList = userFollowerRepository.findByUser(user);
        List<User> followerList = userFollowerList.stream().map(UserFollower::getFollowerUser).collect(Collectors.toList());
        return toUserResponse(followerList);
    }

    //팔로잉유저 조회
    @Transactional
    public List<UserResponse> findFollowingList(String loginId) {
        User user = userRepository.findByLoginId(loginId).orElseThrow(() -> new UsernameNotFoundException("일치하는 유저가 없습니다."));
        List<UserFollowing> userFollowingList = userFollowingRepository.findByUser(user);
        List<User> followingList = userFollowingList.stream().map(UserFollowing::getFollowingUser).collect(Collectors.toList());
        return toUserResponse(followingList);
    }

    //유저 조회
    @Transactional
    public UserResponse userInfo(String loginId,String nickname){
        User user;
        if(nickname == null){
            user = userRepository.findByLoginId(loginId).orElseThrow(() -> new UsernameNotFoundException("일치하는 유저가 없습니다."));
        } else {
            user = userRepository.findByNickname(nickname).orElseThrow(() -> new UsernameNotFoundException("일치하는 유저가 없습니다."));
        }
        return toUserResponseOnce(user);
    }

    //모든 유저 조회
    @Transactional
    public List<UserResponse> findAllUser() {
        List<User> allUser = userRepository.findAll();
        return toUserResponse(allUser);
    }

    //테스트 코드용 데이터 전체 삭제
    @Transactional
    public void deleteAll() {
        userFollowerRepository.deleteAll();
        userFollowingRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Transactional
    public User findByNickname(String nickname) {
        Optional<User> optionalUser = userRepository.findByNickname(nickname);
        if(optionalUser.isEmpty()){
            return null;
        }
        return optionalUser.get();
    }

    protected List<UserResponse> toUserResponse(List<User> users) {
        List<UserResponse> userResponseList = new ArrayList<>();
        for (User user : users) {
            UserResponse userResponse = UserResponse.builder()
                    .email(user.getEmail())
                    .nickname(user.getNickname())
                    .name(user.getName())
                    .profileImageUrl(user.getProfileImageUrl())
                    .state(user.getState())
                    .build();

            userResponseList.add(userResponse);
        }

        return userResponseList;
    }

    protected UserResponse toUserResponseOnce(User user){
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .loginId(user.getLoginId())
                .name(user.getName())
                .nickname(user.getNickname())
                .userRole(user.getUserRole())
                .phoneNumber(user.getPhoneNumber())
                .profileImageUrl(user.getProfileImageUrl())
                .state(user.getState())
                .build();
    }
}
