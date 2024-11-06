package sisyphus_core.sisyphus_core.chat.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class ChatRoomRequest {

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class register{
        private String roomName;
        private String[] nicknames;
        private String type;
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class modify{
        private Long chatRoomId;
        private String newRoomName;
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class invite{
        private Long chatRoomId;
        private String[] nicknames;
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class join{
        private Long chatRoomId;
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class leave{
        private Long chatRoomId;
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class notification{
        private Long roomId;
        private String nickname;
    }
}
