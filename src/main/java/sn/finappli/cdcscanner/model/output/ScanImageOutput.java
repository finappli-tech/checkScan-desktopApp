package sn.finappli.cdcscanner.model.output;

import lombok.Builder;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;
import java.util.Objects;

@Builder
public final class ScanImageOutput {
    private final int scanId;
    private final List<File> files;

    public int scanId() {
        return scanId;
    }

    public List<File> files() {
        return files;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ScanImageOutput) obj;
        return this.scanId == that.scanId &&
                Objects.equals(this.files, that.files);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scanId, files);
    }

    @Contract(pure = true)
    @Override
    public @NotNull String toString() {
        return "ScanImageOutput[scanId=%d, files=%s]".formatted(scanId, files.toString());
    }

}
