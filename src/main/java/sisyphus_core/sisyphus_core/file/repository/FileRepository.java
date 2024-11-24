package sisyphus_core.sisyphus_core.file.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sisyphus_core.sisyphus_core.file.model.UploadFile;

import java.util.List;

@Repository
public interface FileRepository extends JpaRepository<UploadFile, Long> {

    List<UploadFile> findByNickname(String nickname);

    void deleteByFileUrl(String fileUrl);

    List<UploadFile> findByChatRoomId(Long chatRoomId);
}
