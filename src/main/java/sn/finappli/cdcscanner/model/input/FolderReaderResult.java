package sn.finappli.cdcscanner.model.input;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.nio.file.Path;
import java.time.LocalDateTime;

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

}
