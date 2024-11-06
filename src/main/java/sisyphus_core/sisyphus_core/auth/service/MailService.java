package sisyphus_core.sisyphus_core.auth.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import sisyphus_core.sisyphus_core.auth.exception.InvalidAuthCodeException;
import sisyphus_core.sisyphus_core.auth.model.Mail;
import sisyphus_core.sisyphus_core.auth.model.dto.MailRequest;

import java.util.Random;

@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender javaMailSender;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${spring.mail.username}")
    private String username;

    //메일 보내기
    public void sendMail(MailRequest request){
        int authNumber = randomNumber();
        String setFrom = username;
        String toMail = request.getEmail();
        String title = "[ai-block-messenger] " + request.getState() + " 인증 메일";

        String content =
                "<h1 style='text-align: center;'>[ai-block-messenger] "+ request.getState() + " 인증 메일</h1>" +
                        "<h1 style='text-align: center;'><br>인증 코드<br><strong style='font-size: 32px; letter-spacing: 8px'>[" + authNumber + "]</strong><br>" +
                        "<h1 style='text-align: center;'>입니다.</h1>";

        SendToUserEmail(setFrom, toMail, title, content);
        String key = "authNumber:" + request.getEmail();
        redisTemplate.opsForValue().set(key, authNumber);
    }

    private void SendToUserEmail(String setFrom, String toMail, String title, String content){
        MimeMessage message = javaMailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "utf-8");
            helper.setFrom(setFrom);
            helper.setTo(toMail);
            helper.setSubject(title);
            helper.setText(content, true);
            javaMailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("이메일 메시지 생성에 실패하였습니다.", e);
        } catch (MailException e) {
            throw new RuntimeException("이메일 전송에 실패하였습니다.", e);
        }
    }

    public boolean confirmAuthNumber(MailRequest request){
        String key = "authNumber:" + request.getEmail();
        Integer authCode = (Integer) redisTemplate.opsForValue().get(key);
        if (authCode == null || authCode != request.getAuthNumber()) {
            throw new InvalidAuthCodeException("코드가 일치하지 않습니다.");
        }
        return true;
    }

    private int randomNumber() {
        Random random = new Random();
        String randomNumber = "";

        for (int i = 0; i < 6; i++) {
            randomNumber += Integer.toString(random.nextInt(10));
        }

        return Integer.parseInt(randomNumber);
    }

    //테스트용
    public int findAuthNumber(String email){
        String key = "authNumber:" + email;
        return (Integer) redisTemplate.opsForValue().get(key);
    }
}
