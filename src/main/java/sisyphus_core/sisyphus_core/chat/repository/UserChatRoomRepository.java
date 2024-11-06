package sisyphus_core.sisyphus_core.chat.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sisyphus_core.sisyphus_core.auth.model.User;
import sisyphus_core.sisyphus_core.chat.model.ChatRoom;
import sisyphus_core.sisyphus_core.chat.model.UserChatRoom;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserChatRoomRepository extends JpaRepository<UserChatRoom, Long> {

    List<UserChatRoom> findUserChatRoomsByUser(User user);

    List<UserChatRoom> findUserChatRoomsByChatRoom(ChatRoom chatRoom);

    Optional<UserChatRoom> findUserChatRoomByChatRoomAndUser(ChatRoom chatRoom, User user);

    void deleteByChatRoom(ChatRoom chatRoom);

    void deleteAllByUser(User user);
}
