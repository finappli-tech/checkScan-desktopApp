package sn.finappli.cdcscanner.utility;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import lombok.experimental.UtilityClass;

import java.util.TimeZone;

@UtilityClass
public class Utils {

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder()
            .addModule(new ParameterNamesModule())
            .addModule(new JavaTimeModule())
            .defaultTimeZone(TimeZone.getTimeZone("Africa/Dakar"))
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING)
            .build();

    public <T> String classToJson(T object) throws JsonProcessingException {
        return JSON_MAPPER.writeValueAsString(object);
    }
}
