package sisyphus_core.sisyphus_core.file.service;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import sisyphus_core.sisyphus_core.auth.model.User;
import sisyphus_core.sisyphus_core.auth.repository.UserRepository;
import sisyphus_core.sisyphus_core.chat.exception.ChatRoomNotFoundException;
import sisyphus_core.sisyphus_core.chat.model.ChatRoom;
import sisyphus_core.sisyphus_core.chat.model.Message;
import sisyphus_core.sisyphus_core.chat.model.UserChatRoom;
import sisyphus_core.sisyphus_core.chat.model.dto.MessageType;
import sisyphus_core.sisyphus_core.chat.repository.ChatRoomRepository;
import sisyphus_core.sisyphus_core.chat.repository.UserChatRoomRepository;
import sisyphus_core.sisyphus_core.chat.service.KafkaProducerService;
import sisyphus_core.sisyphus_core.file.model.UploadFile;
import sisyphus_core.sisyphus_core.file.model.dto.UploadFileResponse;
import sisyphus_core.sisyphus_core.file.repository.FileRepository;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService {

    private final AmazonS3Client amazonS3Client;
    private final UserRepository userRepository;
    private final FileRepository fileRepository;
    private final SimpMessagingTemplate template;
    private final KafkaProducerService kafkaProducerService;
    private final ChatRoomRepository chatRoomRepository;
    private final UserChatRoomRepository userChatRoomRepository;

    @Value("${aws.cloud.s3.bucket}")
    private String bucket;

    @Value("${file.storage.path}")
    private String fileStoragePath;

    @Transactional
    public String upload(List<MultipartFile> files, Long roomId, String loginId){
        User user = userRepository.findByLoginId(loginId).orElseThrow(() -> new UsernameNotFoundException("일치하는 유저가 없습니다."));
        ChatRoom chatRoom = chatRoomRepository.findById(roomId).orElseThrow(() -> new ChatRoomNotFoundException("일치하는 채팅방이 없습니다."));
        List<UserChatRoom> userChatRoomsByChatRoom = userChatRoomRepository.findUserChatRoomsByChatRoom(chatRoom);
        String fileUrl = "";
        StringBuilder fileUrls = new StringBuilder();
        for (MultipartFile file : files) {
            fileUrl = uploadFile(file);
            fileUrls.append(fileUrl).append(",");
            UploadFile uploadFile = UploadFile.builder().nickname(user.getNickname()).fileUrl(fileUrl).build();
            fileRepository.save(uploadFile);
        }

        if (fileUrls.length() > 0) {
            fileUrls.setLength(fileUrls.length() - 1);
        }

        Message message = Message.builder().message(fileUrls.toString()).roomId(roomId).type(MessageType.FILE).senderName(user.getNickname()).build();
        kafkaProducerService.sendMessage(message);
        template.convertAndSend("/topic/chatroom/" + message.getRoomId(), message);
        for (UserChatRoom userChatRoom : userChatRoomsByChatRoom) {
            template.convertAndSend("/queue/chatroom/" + userChatRoom.getUser().getNickname(), message);
        }

        return fileUrl;
    }

    @Transactional
    public List<UploadFileResponse> findByUser(String loginId){
        User user = userRepository.findByLoginId(loginId).orElseThrow(() -> new UsernameNotFoundException("일치하는 유저가 없습니다."));
        return toResponse(fileRepository.findByNickname(user.getNickname()));
    }

    public String uploadFile(MultipartFile file){

        UUID uuid = UUID.randomUUID();

        String fileName = uuid + "_" + file.getOriginalFilename();

        File saveFile = new File(fileStoragePath, fileName);

        try {
            file.transferTo(saveFile);
        } catch(IOException e) {
            throw new RuntimeException("파일을 저장하는 동안 오류가 발생했습니다.");
        }

        amazonS3Client.putObject(new PutObjectRequest(bucket, fileName, saveFile).withCannedAcl(CannedAccessControlList.PublicRead));
        String fileUrl = amazonS3Client.getUrl(bucket, fileName).toString();

        saveFile.delete();
        return fileUrl;
    }

    @Transactional
    public void deleteFile(String fileUrl){
        String fileName = fileUrl.substring(fileUrl.lastIndexOf("/")+1);
        amazonS3Client.deleteObject(bucket, fileName);
        fileRepository.deleteByFileUrl(fileUrl);
    }

    public List<UploadFileResponse> toResponse(List<UploadFile> files){
        List<UploadFileResponse> toResponseFile = new ArrayList<>();

        for (UploadFile uploadFile : files) {
            UploadFileResponse uploadFileResponse = UploadFileResponse.builder()
                    .fileUrl(uploadFile.getFileUrl())
                    .createAt(uploadFile.getCreateAt())
                    .nickname(uploadFile.getNickname())
                    .build();

            toResponseFile.add(uploadFileResponse);
        }

        return toResponseFile;
    }
}
