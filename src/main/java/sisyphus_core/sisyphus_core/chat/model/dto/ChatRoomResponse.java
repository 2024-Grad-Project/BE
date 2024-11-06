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
    private List<UserResponse.toChat> userInfo;
    private int userCount;
    private String recentMessage;
    private ZonedDateTime createAt;
    private boolean isFix;
    private int NotificationCount;
}
