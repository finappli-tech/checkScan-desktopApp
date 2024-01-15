package sn.finappli.cdcscanner.model.input;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public final class YamlConfig {

    private String baseUrl;

    private int pageItems;
}
