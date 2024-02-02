package sn.finappli.cdcscanner.model.input;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;

@Builder
@AllArgsConstructor
@Getter
public final class FolderReaderResult {
    private final String name;
    private final Path fileD;
    private final Path fileR;
    private final Path fileV;

    @Setter
    private LocalDateTime scanDate;
    @Setter
    private String cmc;
    @Setter
    private LocalDate date;
    @Setter
    private String recipient;
    @Setter
    private String amount;

    /**
     * Validates the mutable fields of the object.
     *
     * @return true if the fields are valid, false otherwise.
     */
    public boolean isValid() {
        return date != null && isNotBlank(trimToEmpty(recipient)) && isValidAmount();
    }

    /**
     * Validates the amount field.
     *
     * @return true if the amount is a valid non-zero number, false otherwise.
     */
    private boolean isValidAmount() {
        try {
            return new BigDecimal(amount).compareTo(BigDecimal.ZERO) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

}
