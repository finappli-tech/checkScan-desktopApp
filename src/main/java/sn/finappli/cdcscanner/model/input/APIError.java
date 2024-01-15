package sn.finappli.cdcscanner.model.input;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@JsonIgnoreProperties(allowGetters = true, value = {"status"}, ignoreUnknown = true)
public class APIError {

    private int status;
    private String error;
    private String message;
    private String messageDetails;
    private Object errors;

    public APIError(int status, String defaultErrorMessage) {
        this.status = status;
        message = defaultErrorMessage;
    }
}
