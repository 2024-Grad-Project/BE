package sisyphus_core.sisyphus_core.auth.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import sisyphus_core.sisyphus_core.auth.model.dto.TokenResponse;
import sisyphus_core.sisyphus_core.auth.model.dto.UserRequest;
import sisyphus_core.sisyphus_core.auth.model.dto.UserResponse;
import sisyphus_core.sisyphus_core.auth.service.UserService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "유저 API", description = "컨트롤러에 대한 설명입니다.")
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    //회원가입
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody @Valid UserRequest.register register){
        userService.register(register);
        return ResponseEntity.ok("회원가입에 성공하였습니다.");
    }

    //로그인
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody @Valid UserRequest.login login, HttpServletResponse response) {
        return ResponseEntity.ok().body(userService.authenticate(login, response));
    }

    //로그아웃
    @GetMapping("/logout")
    public ResponseEntity<String> logout(Authentication auth, HttpServletResponse response){
        userService.unAuthenticate(auth.getName(), response);
        return ResponseEntity.ok("로그아웃에 성공하였습니다.");
    }

    @GetMapping("/info")
    public ResponseEntity<UserResponse> info(Authentication auth){
        return ResponseEntity.ok().body(userService.userInfo(auth.getName()));
    }

    //정보변경
    @PutMapping("/modify")
    public ResponseEntity<String> modify(@RequestBody @Valid UserRequest.modify modify, Authentication auth){
        userService.modify(modify, auth.getName());
        return ResponseEntity.ok("유저의 정보가 변경되었습니다.");
    }

    //회원탈퇴
    @DeleteMapping("/withdrawal")
    public ResponseEntity<String> withdrawal(Authentication auth){
        userService.withdrawal(auth.getName());
        return ResponseEntity.ok("회원탈퇴에 성공하였습니다.");
    }

    //아이디찾기
    @PostMapping("/find/loginId")
    public ResponseEntity<UserResponse.find> findLoginId(@RequestBody @Valid UserRequest.find find){
        return ResponseEntity.ok().body(userService.findLoginId(find.getEmail()));
    }

    //비밀번호찾기
    @PostMapping("/find/password")
    public ResponseEntity<UserResponse.find> findPassword(@RequestBody @Valid UserRequest.find find){
        return ResponseEntity.ok().body(userService.findPassword(find.getLoginId()));
    }

    //로그인아이디 중복체크
    @PostMapping("/check/loginId")
    public ResponseEntity<String> checkLoginId(@RequestBody @Valid UserRequest.check check){
        userService.checkDuplicateLoginId(check.getLoginId());
        return ResponseEntity.ok("사용 가능한 아이디입니다.");
    }

    //닉네임 중복체크
    @PostMapping("/check/nickname")
    public ResponseEntity<String> checkNickname(@RequestBody @Valid UserRequest.check check){
        userService.checkDuplicateNickname(check.getNickname());
        return ResponseEntity.ok("사용 가능한 닉네임입니다.");
    }

    //유저 팔로우
    @PutMapping("/follow")
    public ResponseEntity<String> follow(@RequestParam String nickname,Authentication auth){
        userService.follow(auth.getName(), nickname);
        return ResponseEntity.ok("팔로우 하였습니다.");
    }

    //유저 언팔로우
    @PutMapping("/unfollow")
    public ResponseEntity<String> unfollow(@RequestParam String nickname, Authentication auth){
        userService.unfollow(auth.getName(), nickname);
        return ResponseEntity.ok("언팔로우 하였습니다.");
    }

    @GetMapping("/follower/list")
    public ResponseEntity<List<UserResponse>> findFollowerList(Authentication auth){
        return ResponseEntity.ok().body(userService.findFollowerList(auth.getName()));
    }

    @GetMapping("/following/list")
    public ResponseEntity<List<UserResponse>> findFollowingList(Authentication auth){
        return ResponseEntity.ok().body(userService.findFollowingList(auth.getName()));
    }
}
