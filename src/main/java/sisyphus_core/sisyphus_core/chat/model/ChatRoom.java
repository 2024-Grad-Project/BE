package sisyphus_core.sisyphus_core.chat.model;

import jakarta.persistence.*;
import lombok.*;
import sisyphus_core.sisyphus_core.chat.model.dto.ChatRoomType;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatRoom {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chatroom_id")
    private Long chatRoomId;

    @Setter
    private String roomName;

    private int userCount;

    @Enumerated(value = EnumType.STRING)
    private ChatRoomType type;

    @Setter
    private boolean customRoomName;

    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<UserChatRoom> userChatRooms = new ArrayList<>();

    @Builder.Default
    private ZonedDateTime createAt = ZonedDateTime.now(ZoneId.of("Asia/Seoul"));

    public void joinUser(){
        this.userCount++;
    }

    public void leaveUser(){
        this.userCount--;
    }
}
