package sn.finappli.cdcscanner.model.output;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ScanRegistrationOutput(UUID appId, String ip, String cmc, String recipient, LocalDateTime date, BigDecimal amount) {
}
