package sisyphus_core.sisyphus_core.auth.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MailRequest {
    private String email;
    private String state;
    private int authNumber;
}
