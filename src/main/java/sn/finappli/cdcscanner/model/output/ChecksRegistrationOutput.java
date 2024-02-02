package sn.finappli.cdcscanner.model.output;

import lombok.Builder;
import lombok.Getter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import sn.finappli.cdcscanner.utility.SystemUtils;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Builder
@Getter
public final class ChecksRegistrationOutput {
    private final File fileD;
    private final File fileR;
    private final File fileV;

    private final String name;
    private final LocalDateTime scanDate;
    private final String cmc;
    private final LocalDate date;
    private final String recipient;
    private final String amount;
    private final String appId;
    private final String ip;

    @Contract(" -> new")
    public @NotNull CheckBodyOutput getBody() {
        return new CheckBodyOutput(
                name,
                scanDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                cmc,
                date.format(DateTimeFormatter.ISO_LOCAL_DATE),
                recipient,
                amount,
                SystemUtils.getAppIdentifier().toString(),
                SystemUtils.getIPAddress());
    }

    public record CheckBodyOutput(String name, String scanDate, String cmc, String date,
                                  String recipient, String amount, String appId, String ip) {
    }

}
