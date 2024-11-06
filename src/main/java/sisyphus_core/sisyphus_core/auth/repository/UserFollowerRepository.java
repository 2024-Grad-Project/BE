package sisyphus_core.sisyphus_core.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sisyphus_core.sisyphus_core.auth.model.User;
import sisyphus_core.sisyphus_core.auth.model.UserFollower;

import java.util.List;

@Repository
public interface UserFollowerRepository extends JpaRepository<UserFollower, Long> {

    void deleteByUserAndFollowerUser(User user, User followerUser);

    void deleteByFollowerUser(User followerUser);

    List<UserFollower> findByUser(User user);
}
