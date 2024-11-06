package sisyphus_core.sisyphus_core.auth;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import sisyphus_core.sisyphus_core.auth.model.dto.MailRequest;
import sisyphus_core.sisyphus_core.auth.model.dto.UserRequest;
import sisyphus_core.sisyphus_core.auth.service.MailService;
import sisyphus_core.sisyphus_core.auth.service.UserService;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
public class MailTest {

    @Autowired
    private MailService mailService;

    @Autowired
    private UserService userService;

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

        userService.register(register1);
        userService.register(register2);
    }

    @AfterEach
    void after(){
        userService.deleteAll();
    }

    @Test
    @DisplayName("아이디찾기 인증 메일 전송")
    void sendMailToUser(){
        MailRequest mailRequest = MailRequest.builder()
                .email("test2@test.com")
                .state("아이디찾기")
                .build();

        mailService.sendMail(mailRequest);
        int authNumber = mailService.findAuthNumber("test2@test.com");
        assertThat(authNumber).isNotNull().isGreaterThan(0);
    }

    @Test
    @DisplayName("인증 코드 검증")
    void confirmAuthNumber(){
        MailRequest mailRequest = MailRequest.builder()
                .email("test2@test.com")
                .state("아이디찾기")
                .build();

        mailService.sendMail(mailRequest);
        int authNumber = mailService.findAuthNumber("test2@test.com");
        MailRequest mailRequest1 = MailRequest.builder()
                .authNumber(authNumber)
                .email("test2@test.com")
                .build();
        boolean check = mailService.confirmAuthNumber(mailRequest1);
        assertThat(check).isTrue();
    }
}
