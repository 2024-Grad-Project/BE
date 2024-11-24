package sisyphus_core.sisyphus_core.chat.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import sisyphus_core.sisyphus_core.auth.model.dto.UserResponse;

import java.time.ZonedDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoomResponse {

    private Long chatRoomId;
    private ChatRoomType type;
    private String roomName;
    private String customRoomName;  // 사용자 정의 이름 추가
    private List<UserResponse.toChat> userInfo;
    private int userCount;
    private String recentMessage;
    private ZonedDateTime createAt;
    private boolean isFix;
    private int NotificationCount;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OpenChatRoom {
        private Long chatRoomId; // 채팅방 ID 추가
        private String roomName;
        private String chatroomImage;
        private ZonedDateTime createAt;
        private int userCount;
        private String recentMessage;
    }
}
