package sisyphus_core.sisyphus_core.chat.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import sisyphus_core.sisyphus_core.auth.model.User;
import sisyphus_core.sisyphus_core.auth.repository.UserRepository;
import sisyphus_core.sisyphus_core.chat.exception.ChatRoomNotFoundException;
import sisyphus_core.sisyphus_core.chat.model.ChatRoom;
import sisyphus_core.sisyphus_core.chat.model.Message;
import sisyphus_core.sisyphus_core.chat.model.dto.ChatRoomResponse;
import sisyphus_core.sisyphus_core.chat.repository.ChatRoomRepository;
import sisyphus_core.sisyphus_core.chat.service.ChatRoomService;
import sisyphus_core.sisyphus_core.chat.service.MessageService;

import java.util.Collections;
import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class RoomController {

    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final ChatRoomService chatRoomService;
    private final MessageService messageService;

    @GetMapping("/chat/{chatRoomId}/{nickname}")
    public String chatP(@PathVariable("chatRoomId") Long chatRoomId,
                        @PathVariable("nickname") String nickname,
                        Model model){
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId).orElseThrow(() -> new ChatRoomNotFoundException("일치하는 채팅방이 없습니다."));
        User user = userRepository.findByNickname(nickname).orElseThrow(() -> new UsernameNotFoundException("일치하는 유저가 없습니다."));
        List<ChatRoomResponse> roomResponses = chatRoomService.userChatRoomList(user.getLoginId());
        List<Message> messages = messageService.chatRoomMessages(chatRoomId,user.getLoginId());
        Collections.reverse(messages);

        model.addAttribute("room", chatRoom);
        model.addAttribute("user", user);
        model.addAttribute("roomList", roomResponses);
        model.addAttribute("messages", messages);

        return "chatRoom";
    }

    @GetMapping("/chat/list")
    public String chatListP(){
        return "chatRooms";
    }

}
