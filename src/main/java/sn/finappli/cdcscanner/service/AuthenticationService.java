package sn.finappli.cdcscanner.service;

import sn.finappli.cdcscanner.model.input.LoginRequestInput;
import sn.finappli.cdcscanner.model.input.LoginRequestPhoneInput;

import java.util.List;

public interface AuthenticationService {

    List<LoginRequestPhoneInput> findUserForApp();

    LoginRequestInput requestAuthentication(String telephone);

    boolean authenticate(String uuid);
}
