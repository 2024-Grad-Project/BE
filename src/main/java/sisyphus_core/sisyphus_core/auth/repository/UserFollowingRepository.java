package sisyphus_core.sisyphus_core.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sisyphus_core.sisyphus_core.auth.model.User;
import sisyphus_core.sisyphus_core.auth.model.UserFollowing;

import java.util.List;

@Repository
public interface UserFollowingRepository extends JpaRepository<UserFollowing, Long> {

    void deleteByUserAndFollowingUser(User user, User followingUser);

    void deleteByUser(User user);

    List<UserFollowing> findByUser(User user);
}
