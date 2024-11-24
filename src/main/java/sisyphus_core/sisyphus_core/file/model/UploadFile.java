package sisyphus_core.sisyphus_core.file.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.ZoneId;
import java.time.ZonedDateTime;

@Entity
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UploadFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_id")
    private Long id;

    private String nickname;

    @Lob
    private String fileUrl;

    private Long fileSize;

    private Long chatRoomId;

    private boolean isReported;

    @Builder.Default
    private ZonedDateTime createAt = ZonedDateTime.now(ZoneId.of("Asia/Seoul"));
}
