package sn.finappli.cdcscanner.model.input;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import static sn.finappli.cdcscanner.utility.Utils.bigDecimalToFormattedFRString;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ScanInputModel {
    private String cmc;
    private String recipient;
    private String amount;
    private String date;
    private String createdAt;
    private boolean validated;
    private boolean rejected;

    private String status;

    public void setCreatedAt(long stamp) {
        createdAt = LocalDateTime.ofInstant(new Date(stamp).toInstant(), ZoneId.of("UTC"))
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    public void setDate(long stamp) {
        date = LocalDateTime.ofInstant(new Date(stamp).toInstant(), ZoneId.of("UTC"))
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    public void setAmount(BigDecimal number) {
        amount = bigDecimalToFormattedFRString(number);
    }

    public String getStatus() {
        if (validated) return "Validé";
        else if (rejected) return "Rejeté";
        else return "En cours";
    }

}
