package sn.finappli.cdcscanner.model.input;

import lombok.Builder;

@Builder
public record ServerResponse(boolean hasError, String error, String message) {
}
