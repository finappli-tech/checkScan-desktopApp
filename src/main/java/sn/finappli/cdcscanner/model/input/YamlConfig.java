package sn.finappli.cdcscanner.model.input;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public final class YamlConfig {

    private String baseUrl;

    private String checkAppRegistrationUrl;
    private String registrationUrl;
    private int loginTokenCountdown;

    private String loginUrl;
    private String requestLoginUsersUrl;
    private String requestLoginUrl;

    private String listScannedItemsUrl;

    private int pageItems;
}
