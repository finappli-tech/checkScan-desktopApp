package sn.finappli.cdcscanner.security;

import java.time.LocalDateTime;

public record SecurityContext(String token, String key, String enc, LocalDateTime expiry) {
}
