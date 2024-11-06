package sisyphus_core.sisyphus_core.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sisyphus_core.sisyphus_core.auth.model.dto.MailRequest;
import sisyphus_core.sisyphus_core.auth.service.MailService;

@RestController
@RequestMapping("/api/mail")
@RequiredArgsConstructor
public class MailController {

    private final MailService mailService;

    //메일 인증
    @PostMapping("/send")
    public ResponseEntity<String> sendMail(@RequestBody @Valid MailRequest request){
        mailService.sendMail(request);
        return ResponseEntity.ok("메일 전송에 성공하였습니다.");
    }

    //코드 인증
    @PostMapping("/confirm")
    public ResponseEntity<String> confirmNumber(@RequestBody @Valid MailRequest request){
        mailService.confirmAuthNumber(request);
        return ResponseEntity.ok("인증에 성공하였습니다.");
    }
}
