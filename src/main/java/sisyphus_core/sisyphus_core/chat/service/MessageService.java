package sisyphus_core.sisyphus_core.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sisyphus_core.sisyphus_core.auth.model.User;
import sisyphus_core.sisyphus_core.auth.repository.UserRepository;
import sisyphus_core.sisyphus_core.chat.exception.ChatRoomNotFoundException;
import sisyphus_core.sisyphus_core.chat.model.ChatRoom;
import sisyphus_core.sisyphus_core.chat.model.Message;
import sisyphus_core.sisyphus_core.chat.model.UserChatRoom;
import sisyphus_core.sisyphus_core.chat.model.dto.ChatRoomRequest;
import sisyphus_core.sisyphus_core.chat.repository.ChatRoomRepository;
import sisyphus_core.sisyphus_core.chat.repository.UserChatRoomRepository;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {

    private final SimpMessagingTemplate template;
    private final KafkaProducerService kafkaProducerService;
    private final RedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final UserChatRoomRepository userChatRoomRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;

    //메세지 보내기
    @Transactional
    public void sendMessage(Message message){
        ChatRoom chatRoom = chatRoomRepository.findById(message.getRoomId()).orElseThrow(() -> new ChatRoomNotFoundException("일치하는 채팅방이 없습니다."));
        List<UserChatRoom> userChatRoomsByChatRoom = userChatRoomRepository.findUserChatRoomsByChatRoom(chatRoom);
        for (UserChatRoom userChatRoom : userChatRoomsByChatRoom) {
            if(!userChatRoom.getUser().getNickname().equals(message.getSenderName())) {
                userChatRoom.upCount();
            }
            template.convertAndSend("/queue/chatroom/" + userChatRoom.getUser().getNickname(), message);
        }
        kafkaProducerService.sendMessage(message);
        template.convertAndSend("/topic/chatroom/" + message.getRoomId(), message);
    }

    //채팅방 나가기
    public void leave(Message message) {
        kafkaProducerService.sendMessage(message);
        template.convertAndSend("/topic/chatroom/" + message.getRoomId(), message);
    }

    //채팅방 참여
    public void join(Message message){
        kafkaProducerService.sendMessage(message);
        template.convertAndSend("/topic/chatroom/" + message.getRoomId(), message);
    }

    //회원 닉네임 수정
    public void modifySenderName(String senderName, String newNickname, Long chatRoomId){
        String key = "room:" + chatRoomId;
        List<Message> allMessages = findMessagesAboutChatRoom(key);

        for (int i = 0; i < allMessages.size(); i++) {
            Message message = allMessages.get(i);

            // senderName이 일치하면 업데이트
            if (message.getSenderName().equals(senderName)) {
                message.setSenderName(newNickname);
                // Redis 리스트 내 특정 인덱스 값 갱신 (생성 시간 유지)
                redisTemplate.opsForList().set(key, i, message);
            }
        }
    }

    //채팅방 메세지 조회
    public List<Message> chatRoomMessages(Long chatRoomId, String loginId){
        String key = "room:" + chatRoomId;
        List<Message> allMessages = findMessagesAboutChatRoom(key);

        String joinKey = "userJoin:" + chatRoomId + ":" + loginId;
        Long userJoinEpoch = getUserJoinEpoch(joinKey);

        // 유저 참여 이후의 메시지만 필터링
        if (userJoinEpoch != null) {
            ZonedDateTime userJoinTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(userJoinEpoch), ZoneId.of("Asia/Seoul"));
            return allMessages.stream()
                    .filter(message -> message.getCreateAt().isAfter(userJoinTime))  // 참여 시간 이후의 메시지만 필터링
                    .collect(Collectors.toList());
        } else {
            // 참여 기록이 없으면 빈 리스트 반환
            return new ArrayList<>();
        }
    }

    public Long getUserJoinEpoch(String joinKey){
        Object userJoinEpochObj = redisTemplate.opsForValue().get(joinKey);

        Long userJoinEpoch = null;

        // Integer인지 Long인지 확인하여 변환
        if (userJoinEpochObj instanceof Integer) {
            userJoinEpoch = ((Integer) userJoinEpochObj).longValue();  // Integer 값을 Long으로 변환
        } else if (userJoinEpochObj instanceof Long) {
            userJoinEpoch = (Long) userJoinEpochObj;  // 이미 Long 타입일 경우
        }

        return userJoinEpoch;
    }

    //가장 최근 메세지 조회
    public Message recentMessage(Long chatRoomId) {
        String key = "room:" + chatRoomId;
        Object result = redisTemplate.opsForList().index(key, 0);

        return objectMapper.convertValue(result, Message.class);
    }

    public List<Message> findMessagesAboutChatRoom(String key){
        List<Object> allMessagesRaw = redisTemplate.opsForList().range(key, 0, -1);
        List<Message> allMessages = new ArrayList<>();

        // LinkedHashMap을 Message로 변환
        for (Object rawMessage : allMessagesRaw) {
            // 강제로 Message 객체로 변환
            Message message = objectMapper.convertValue(rawMessage, Message.class);
            allMessages.add(message);
        }
        return allMessages;
    }

    @Transactional
    public void resetNotification(ChatRoomRequest.notification notification){
        ChatRoom chatRoom = chatRoomRepository.findById(notification.getRoomId()).orElseThrow(() -> new ChatRoomNotFoundException("일치하는 채팅방이 없습니다"));
        User user = userRepository.findByNickname(notification.getNickname()).orElseThrow(() -> new UsernameNotFoundException("일치하는 유저가 없습니다."));
        UserChatRoom userChatRoomByChatRoomAndUser = userChatRoomRepository.findUserChatRoomByChatRoomAndUser(chatRoom, user).orElseThrow(() -> new ChatRoomNotFoundException("유저가 속해있는 채팅방이 아닙니다."));

        userChatRoomByChatRoomAndUser.resetCount();
    }
}
