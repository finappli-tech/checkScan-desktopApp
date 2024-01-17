package sn.finappli.cdcscanner.model.input;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record LoginRequestInput(String uuid, String hashedCode, LocalDateTime expiry, String error) {
}
