package sn.finappli.cdcscanner.model.input;

import com.dynarithmic.twain.DTwainConstants;
import lombok.Builder;

@Builder
public record TwainResponse(DTwainConstants.ErrorCode code, String error) {
}
