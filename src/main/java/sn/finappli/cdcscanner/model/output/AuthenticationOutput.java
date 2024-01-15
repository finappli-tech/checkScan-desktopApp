package sn.finappli.cdcscanner.model.output;

import lombok.Builder;

import java.util.UUID;

@Builder
public record AuthenticationOutput(UUID appId, String code) {
}
