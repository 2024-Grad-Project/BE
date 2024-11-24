package sisyphus_core.sisyphus_core;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.multipart.MultipartFile;
import sisyphus_core.sisyphus_core.auth.model.dto.UserRequest;
import sisyphus_core.sisyphus_core.auth.service.UserService;
import sisyphus_core.sisyphus_core.chat.model.dto.ChatRoomRequest;
import sisyphus_core.sisyphus_core.chat.service.ChatRoomService;

@SpringBootApplication
@RequiredArgsConstructor
public class SisyphusCoreApplication {

	private final UserService userService;
	private final ChatRoomService chatRoomService;

	public static void main(String[] args) {
		SpringApplication.run(SisyphusCoreApplication.class, args);
	}

	@PostConstruct
	public void postData(){
		UserRequest.register register1= UserRequest.register.builder()
				.loginId("test1")
				.password("1234")
				.nickname("test1")
				.email("test1@test.com")
				.name("test1")
				.phoneNumber("010-1111-1111")
				.build();

		UserRequest.register register2= UserRequest.register.builder()
				.loginId("test2")
				.password("1234")
				.nickname("test2")
				.email("test2@test.com")
				.name("test2")
				.phoneNumber("010-2222-2222")
				.build();

		UserRequest.register register3= UserRequest.register.builder()
				.loginId("test3")
				.password("1234")
				.nickname("test3")
				.email("test3@test.com")
				.name("test3")
				.phoneNumber("010-3333-3333")
				.build();

		UserRequest.register register4= UserRequest.register.builder()
				.loginId("test4")
				.password("1234")
				.nickname("test4")
				.email("test4@test.com")
				.name("test4")
				.phoneNumber("010-4444-4444")
				.build();

		String[] nicknames = new String[]{"test2"};
		ChatRoomRequest.register chatRegister = ChatRoomRequest.register.builder()
						.roomName("채팅방1번")
						.chatRoomImage("")
						.nicknames(nicknames)
						.type("general")
						.build();

		String[] nicknames2 = new String[]{"test2", "test3"};
		ChatRoomRequest.register chatRegister2 = ChatRoomRequest.register.builder()
				.roomName("채팅방2번")
				.chatRoomImage("https://search.pstatic.net/sunny/?src=https%3A%2F%2Fi.pinimg.com%2F736x%2F6e%2F9d%2F3c%2F6e9d3cb56fcc872ca8bbb7b62293a9af.jpg&type=sc960_832")
				.nicknames(nicknames2)
				.type("open")
				.build();

		userService.register(register1);
		userService.register(register2);
		userService.register(register3);
		userService.register(register4);
		chatRoomService.createChatRoom(chatRegister, null,"test1");
		chatRoomService.createChatRoom(chatRegister2, null,"test1");
	}

}
