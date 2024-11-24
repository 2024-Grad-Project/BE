package sisyphus_core.sisyphus_core.chat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import sisyphus_core.sisyphus_core.auth.model.User;
import sisyphus_core.sisyphus_core.auth.model.dto.UserResponse;
import sisyphus_core.sisyphus_core.auth.repository.UserRepository;
import sisyphus_core.sisyphus_core.chat.exception.ChatRoomNotFoundException;
import sisyphus_core.sisyphus_core.chat.exception.DuplicateChatRoomNameException;
import sisyphus_core.sisyphus_core.chat.model.ChatRoom;
import sisyphus_core.sisyphus_core.chat.model.Message;
import sisyphus_core.sisyphus_core.chat.model.UserChatRoom;
import sisyphus_core.sisyphus_core.chat.model.dto.ChatRoomRequest;
import sisyphus_core.sisyphus_core.chat.model.dto.ChatRoomResponse;
import sisyphus_core.sisyphus_core.chat.model.dto.ChatRoomType;
import sisyphus_core.sisyphus_core.chat.model.dto.MessageType;
import sisyphus_core.sisyphus_core.chat.repository.ChatRoomRepository;
import sisyphus_core.sisyphus_core.chat.repository.UserChatRoomRepository;
import sisyphus_core.sisyphus_core.file.service.FileService;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final UserChatRoomRepository userChatRoomRepository;
    private final UserRepository userRepository;
    private final MessageService messageService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final SimpMessagingTemplate template;
    private final FileService fileService;
    private final String CHAT_ROOM_FIX = "채팅방을 고정하였습니다.";
    private final String CHAT_ROOM_UNFIX = "채팅방 고정을 해제하였습니다.";

    @Value("${default.open.image.url}")
    private String defaultChatImage;

    //채팅 방 생성
    @Transactional
    public ChatRoom createChatRoom(ChatRoomRequest.register register, MultipartFile file, String loginId){
        String roomName = register.getRoomName();
        String[] nicknames = register.getNicknames();
        String type = register.getType().toLowerCase();
        String chatRoomImage = defaultChatImage;

        if(file != null) chatRoomImage = fileService.uploadAtChatRoom(file);
        boolean customRoomName = true;

        User user = userRepository.findByLoginId(loginId).orElseThrow(() -> new UsernameNotFoundException("일치하는 유저가 없습니다."));
        if(roomName == null || roomName.equals("")) {
            customRoomName = false;
            if (nicknames.length == 1) {
                roomName = user.getNickname() + ", " + nicknames[0];
            } else {
                roomName = user.getNickname() + ", " + String.join(", ", nicknames);
            }
        }

        Optional<ChatRoom> byRoomName = chatRoomRepository.findByRoomName(roomName);
        List<UserChatRoom> chatRoomList = userChatRoomRepository.findUserChatRoomsByUser(user);
        if(byRoomName.isPresent()){
            ChatRoom chatRoom = byRoomName.get();
            if(chatRoom.getType() == ChatRoomType.OPEN){
                throw new DuplicateChatRoomNameException("이미 존재하는 오픈채팅방입니다.");
            }else{
                for (UserChatRoom userChatRoom : chatRoomList) {
                    if(userChatRoom.getChatRoom().equals(chatRoom)) return chatRoom;
                }
            }
        }

        ChatRoomType chatRoomType = type.equals("open") ? ChatRoomType.OPEN : ChatRoomType.GENERAL;
        ChatRoom chatRoom = ChatRoom.builder()
                .roomName(roomName)
                .userCount(nicknames.length + 1)
                .type(chatRoomType)
                .customRoomName(customRoomName)
                .chatRoomImage(chatRoomImage)
                .build();

        chatRoomRepository.save(chatRoom);

        saveUserJoinTime(chatRoom.getChatRoomId(), user.getLoginId());
        for (String nickname : nicknames) {
            User inviteUser = userRepository.findByNickname(nickname).orElseThrow(() -> new UsernameNotFoundException("일치하는 유저가 없습니다."));
            UserChatRoom userChatRoom = UserChatRoom.builder()
                    .chatRoom(chatRoom)
                    .user(inviteUser)
                    .isFix(false)
                    .NotificationCount(0)
                    .build();
            userChatRoomRepository.save(userChatRoom);
            saveUserJoinTime(chatRoom.getChatRoomId(), inviteUser.getLoginId());
        }

        UserChatRoom userChatRoom = UserChatRoom.builder()
                .chatRoom(chatRoom)
                .user(user)
                .isFix(false)
                .NotificationCount(0)
                .build();

        userChatRoomRepository.save(userChatRoom);
        String createMessage = user.getNickname() + "님이 ";

        for(int i=0; i<nicknames.length; i++){
            String nickname = nicknames[i];
            if (i == nicknames.length - 1) {
                createMessage += nickname + "님을 초대하였습니다.";
            } else {
                createMessage += nickname + "님과 ";
            }

            template.convertAndSend("/queue/chatroom/list/" + nickname, toResponseChatRoom(chatRoom));
        }

        if(type.equals("open")) createMessage = user.getNickname() + "님이 오픈채팅방을 생성하셨습니다.";
        Message message = Message.builder().type(MessageType.INVITE).message(createMessage).roomId(chatRoom.getChatRoomId()).senderName(user.getNickname()).build();
        messageService.join(message);
        template.convertAndSend("/queue/chatroom/list/" + user.getNickname(), toResponseChatRoom(chatRoom));
        return chatRoom;
    }

    //방 정보 변경
    @Transactional
    public void modifyChatRoom(ChatRoomRequest.modify modify, String loginId){
        ChatRoom chatRoom = chatRoomRepository.findById(modify.getChatRoomId()).orElseThrow(() -> new ChatRoomNotFoundException("일치하는 채팅방이 없습니다."));
        User user1 = userRepository.findByLoginId(loginId).orElseThrow(() -> new UsernameNotFoundException("일치하는 유저가 없습니다."));
        if(userChatRoomRepository.findUserChatRoomByChatRoomAndUser(chatRoom, user1).isEmpty()) {
            throw new ChatRoomNotFoundException("방 정보 변경의 권한이 없습니다.");
        }
        List<UserChatRoom> userChatRoomsByChatRoom = userChatRoomRepository.findUserChatRoomsByChatRoom(chatRoom);
        if(chatRoom.getType() == ChatRoomType.OPEN){
            if(chatRoomRepository.findByRoomName(modify.getNewRoomName()).isPresent()){
                throw new DuplicateChatRoomNameException("이미 존재하는 오픈채팅방입니다.");
            }
        }

        chatRoom.setRoomName(modify.getNewRoomName());
        if(!chatRoom.isCustomRoomName()) chatRoom.setCustomRoomName(true);
        chatRoomRepository.save(chatRoom);

        for (UserChatRoom userChatRoom : userChatRoomsByChatRoom) {
            User user = userChatRoom.getUser();
            template.convertAndSend("/queue/chatroom/list/" + user.getNickname(), toResponseChatRoom(chatRoom));
        }
    }

    //유저 채팅방 조회
    @Transactional
    public List<ChatRoomResponse> userChatRoomList(String loginId){
        User user = userRepository.findByLoginId(loginId).orElseThrow(() -> new UsernameNotFoundException("일치하는 유저가 없습니다."));
        List<UserChatRoom> userChatRoomsByUser = userChatRoomRepository.findUserChatRoomsByUser(user);

        return toResponseChatRoom(userChatRoomsByUser);
    }

    //채팅방 입장 (오픈채팅)
    @Transactional
    public void joinChatRoom(String loginId, Long chatRoomId){
        User user = userRepository.findByLoginId(loginId).orElseThrow(() -> new UsernameNotFoundException("일치하는 유저가 없습니다."));
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId).orElseThrow(() -> new ChatRoomNotFoundException("일치하는 채팅방이 없습니다."));
        saveUserJoinTime(chatRoom.getChatRoomId(), loginId);
        chatRoom.joinUser();

        UserChatRoom userChatRoom = UserChatRoom.builder()
                .chatRoom(chatRoom)
                .user(user)
                .isFix(false)
                .NotificationCount(0)
                .build();

        userChatRoomRepository.save(userChatRoom);
        Message message = Message.builder().roomId(chatRoomId).message(user.getNickname() + "님이 입장하셨습니다.").type(MessageType.JOIN).build();
        messageService.join(message);
    }

    //채팅방 초대 (일반채팅)
    @Transactional
    public void inviteChatRoom(ChatRoomRequest.invite invite,String loginId){
        ChatRoom chatRoom = chatRoomRepository.findById(invite.getChatRoomId()).orElseThrow(() -> new ChatRoomNotFoundException("일치하는 채팅방이 없습니다."));
        User user1 = userRepository.findByLoginId(loginId).orElseThrow(() -> new UsernameNotFoundException("일치하는 유저가 없습니다."));
        String inviteMessage = user1.getNickname() + "님이 ";
        String[] nicknames = invite.getNicknames();
        for (int i = 0; i < nicknames.length; i++) {
            String nickname = nicknames[i];

            User user = userRepository.findByNickname(nickname)
                    .orElseThrow(() -> new UsernameNotFoundException("일치하는 유저가 없습니다."));

            saveUserJoinTime(chatRoom.getChatRoomId(), user.getLoginId());
            chatRoom.joinUser();

            if (!chatRoom.isCustomRoomName()) {
                chatRoom.setRoomName(chatRoom.getRoomName() + ", " + user.getNickname());
            }

            UserChatRoom userChatRoom = UserChatRoom.builder()
                    .chatRoom(chatRoom)
                    .user(user)
                    .isFix(false)
                    .NotificationCount(0)
                    .build();

            userChatRoomRepository.save(userChatRoom);

            if (i == nicknames.length - 1) {
                inviteMessage += nickname + "님을 초대하였습니다.";
            } else {
                inviteMessage += nickname + "님과 ";
            }
        }

        Message message = Message.builder().roomId(chatRoom.getChatRoomId()).message(inviteMessage).type(MessageType.INVITE).build();
        messageService.join(message);
    }

    //채팅방 초대, 참여 시 유저 참여 시간 저장
    public void saveUserJoinTime(Long chatRoomId, String loginId) {
        String joinKey = "userJoin:" + chatRoomId + ":" + loginId;
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Seoul"));
        redisTemplate.opsForValue().set(joinKey, now.toEpochSecond());
    }

    //채팅방 나가기
    @Transactional
    public void leaveChatRoom(ChatRoomRequest.leave leave,String loginId){
        User user = userRepository.findByLoginId(loginId).orElseThrow(() -> new UsernameNotFoundException("일치하는 유저가 없습니다."));
        ChatRoom chatRoom = chatRoomRepository.findById(leave.getChatRoomId()).orElseThrow(() -> new ChatRoomNotFoundException("일치하는 채팅방이 없습니다."));

        chatRoom.leaveUser();
        if(!chatRoom.isCustomRoomName()) {
            String renewalChatRoomName = chatRoom.getRoomName().replace(user.getNickname(), "").trim();

            renewalChatRoomName = renewalChatRoomName.replaceAll(",\\s*,", ", ")
                    .replaceAll(",\\s*$", "")
                    .replaceAll("^\\s*,", "")
                    .trim();

            chatRoom.setRoomName(renewalChatRoomName);
        }

        List<UserChatRoom> userChatRoomsByUser = userChatRoomRepository.findUserChatRoomsByUser(user);
        for (UserChatRoom userChatRoom : userChatRoomsByUser) {
            if (userChatRoom.getChatRoom().getChatRoomId().equals(leave.getChatRoomId())) {
                userChatRoomRepository.delete(userChatRoom);
                break;
            }
        }

        Message message = Message.builder().roomId(chatRoom.getChatRoomId()).senderName(user.getNickname()).message(user.getNickname() + "님이 퇴장하셨습니다.").
                type(MessageType.LEAVE).build();
        messageService.leave(message);
        deleteUserJoinTime(chatRoom.getChatRoomId(), user.getLoginId());

        template.convertAndSend("/queue/chatroom/list/" + user.getNickname(), toResponseChatRoom(chatRoom));
        if(chatRoom.getUserCount() == 0) chatRoomRepository.deleteById(leave.getChatRoomId());
        else chatRoomRepository.save(chatRoom);
    }

    //채팅방을 나갈때 유저 참여 시간 제거
    public void deleteUserJoinTime(Long chatRoomId, String loginId) {
        String joinKey = "userJoin:" + chatRoomId + ":" + loginId;
        redisTemplate.delete(joinKey);
    }

    //회원탈퇴 시 채팅방 정보 변경
    @Transactional
    public void withdrawalUser(List<UserChatRoom> userChatRoomList, String nickname){
        for (UserChatRoom userChatRoom : userChatRoomList) {
            ChatRoom chatRoom = userChatRoom.getChatRoom();
            chatRoom.leaveUser();
            if(!chatRoom.isCustomRoomName()) {
                String renewalChatRoomName = chatRoom.getRoomName().replace(nickname, "").trim();

                renewalChatRoomName = renewalChatRoomName.replaceAll(",\\s*,", ", ")
                        .replaceAll(",\\s*$", "")
                        .replaceAll("^\\s*,", "")
                        .trim();

                chatRoom.setRoomName(renewalChatRoomName);
            }

            if(chatRoom.getUserCount() == 0) chatRoomRepository.deleteById(chatRoom.getChatRoomId());
            else chatRoomRepository.save(chatRoom);
        }
    }

    //채팅방 고정
    @Transactional
    public String toggleChatRoom(Long chatRoomId, String loginId){
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId).orElseThrow(() -> new ChatRoomNotFoundException("일치하는 채팅방이 없습니다."));
        User user = userRepository.findByLoginId(loginId).orElseThrow(() -> new UsernameNotFoundException("일치하는 유저가 없습니다."));
        UserChatRoom userChatRoom = userChatRoomRepository.findUserChatRoomByChatRoomAndUser(chatRoom, user).get();
        if(!userChatRoom.isFix()){
            userChatRoom.setFix(true);
            return CHAT_ROOM_FIX;
        }else{
            userChatRoom.setFix(false);
            return CHAT_ROOM_UNFIX;
        }
    }

    //오픈채팅방 리스트
    @Transactional
    public List<ChatRoomResponse.OpenChatRoom> getOpenChatRooms(){
        List<ChatRoom> openChatRooms = chatRoomRepository.findByType(ChatRoomType.OPEN);

        return toOpenChatRoom(openChatRooms);
    }

    protected List<ChatRoomResponse.OpenChatRoom> toOpenChatRoom(List<ChatRoom> openChatRooms){
        List<ChatRoomResponse.OpenChatRoom> openChatRoomList = new ArrayList<>();
        for (ChatRoom openChatRoom : openChatRooms) {
            Message message = messageService.recentMessage(openChatRoom.getChatRoomId());
            ChatRoomResponse.OpenChatRoom openChatRoom1 = ChatRoomResponse.OpenChatRoom.builder()
                    .chatRoomId(openChatRoom.getChatRoomId()) // chatRoomId 추가
                    .roomName(openChatRoom.getRoomName())
                    .chatroomImage(openChatRoom.getChatRoomImage())
                    .recentMessage(message.getMessage())
                    .createAt(message.getCreateAt())
                    .userCount(openChatRoom.getUserCount())
                    .build();
            openChatRoomList.add(openChatRoom1);
        }
        return openChatRoomList;
    }

    //ResponseChatRoom으로 변환 -> 채팅방리스트 DTO
    @Transactional
    protected List<ChatRoomResponse> toResponseChatRoom(List<UserChatRoom> userChatRoomsByUser) {
        List<ChatRoomResponse> roomResponses = new ArrayList<>();
        for (UserChatRoom userChatRoom : userChatRoomsByUser) {
            List<UserResponse.toChat> userInfo = new ArrayList<>();
            ChatRoom chatRoom = userChatRoom.getChatRoom();
            List<UserChatRoom> userChatRoomsByChatRoom = userChatRoomRepository.findUserChatRoomsByChatRoom(chatRoom);

            Message message = messageService.recentMessage(chatRoom.getChatRoomId());
            String recentMessage = "";
            ZonedDateTime createAt = null;
            if(message != null){
                if(message.getType().equals(MessageType.FILE)){
                    recentMessage = message.getSenderName() + "님이 사진을 보냈습니다.";
                }else{
                    recentMessage = message.getMessage();
                }
                createAt = message.getCreateAt();
            }

            for (UserChatRoom room : userChatRoomsByChatRoom) {
                User user = room.getUser();
                UserResponse.toChat infoToChat = UserResponse.toChat.builder().id(user.getId())
                        .profileImageUrl(user.getProfileImageUrl()).nickname(user.getNickname()).build();
                userInfo.add(infoToChat);
            }

            ChatRoomResponse chatRoomResponse = ChatRoomResponse.builder()
                    .chatRoomId(chatRoom.getChatRoomId())
                    .type(chatRoom.getType())
                    .roomName(chatRoom.isCustomRoomName() ? chatRoom.getRoomName() : null) // customRoomName이 true일 때만 roomName 사용
                    .customRoomName(chatRoom.isCustomRoomName() ? chatRoom.getRoomName() : null) // 사용자 정의 이름으로 설정
                    .userInfo(userInfo)
                    .userCount(chatRoom.getUserCount())
                    .recentMessage(recentMessage)
                    .createAt(createAt)
                    .NotificationCount(userChatRoom.getNotificationCount())
                    .isFix(userChatRoom.isFix())
                    .build();

            roomResponses.add(chatRoomResponse);
        }

        return roomResponses;
    }

    //실시간 채팅방 생성시 필요한 DTO
    @Transactional
    protected ChatRoomResponse toResponseChatRoom(ChatRoom chatRoom) {
        List<UserChatRoom> userChatRoomsByChatRoom = userChatRoomRepository.findUserChatRoomsByChatRoom(chatRoom);
        List<UserResponse.toChat> userInfo = new ArrayList<>();

        Message message = messageService.recentMessage(chatRoom.getChatRoomId());
        String recentMessage = "";
        ZonedDateTime createAt = null;
        if(message != null){
            recentMessage = message.getMessage();
            createAt = message.getCreateAt();
        }

        for (UserChatRoom userChatRoom : userChatRoomsByChatRoom) {
            User user = userChatRoom.getUser();
            UserResponse.toChat infoToChat = UserResponse.toChat.builder().id(user.getId())
                    .profileImageUrl(user.getProfileImageUrl()).nickname(user.getNickname()).build();
            userInfo.add(infoToChat);
        }

        return ChatRoomResponse.builder()
                .roomName(chatRoom.getRoomName())
                .chatRoomId(chatRoom.getChatRoomId())
                .type(chatRoom.getType())
                .NotificationCount(0)
                .isFix(false)
                .userInfo(userInfo)
                .recentMessage(recentMessage)
                .createAt(createAt)
                .userCount(chatRoom.getUserCount())
                .build();
    }

    //테스트 코드용 데이터 전체 삭제
    @Transactional
    public void deleteAll(){
        userChatRoomRepository.deleteAll();
        chatRoomRepository.deleteAll();
    }

    @Transactional
    public ChatRoom findByRoomName(String roomName){
        return chatRoomRepository.findByRoomName(roomName).orElseThrow(() -> new ChatRoomNotFoundException("일치하는 채팅방이 없습니다."));
    }
}
