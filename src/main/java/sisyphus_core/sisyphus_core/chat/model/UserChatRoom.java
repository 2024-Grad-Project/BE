package sisyphus_core.sisyphus_core.chat.model;

import jakarta.persistence.*;
import lombok.*;
import sisyphus_core.sisyphus_core.auth.model.User;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_chatRoom_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "chatRoom_id", nullable = false)
    private ChatRoom chatRoom;

    @Setter
    private boolean isFix;

    private int NotificationCount;

    public void upCount(){
        this.NotificationCount++;
    }

    public void resetCount(){
        this.NotificationCount = 0;
    }
}
