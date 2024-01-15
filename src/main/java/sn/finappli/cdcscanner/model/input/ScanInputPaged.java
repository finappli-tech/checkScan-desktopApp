package sn.finappli.cdcscanner.model.input;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class ScanInputPaged {
    private int totalPages;
    private long totalElements;
    private List<ScanInputModel> operations;

    public ScanInputPaged() {
        totalPages = 0;
        totalElements = 0;
        operations = new ArrayList<>();
    }
}

