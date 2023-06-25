package io.credable.reconapi.util.pathextractor;

import com.opencsv.exceptions.CsvException;
import io.credable.reconapi.exception.CustomException;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;

import static java.util.Objects.isNull;

public class ExcelExtractor {
    public static Set<String> extract(MultipartFile file, Integer headerLineNumber) throws IOException, CsvException {
        if (isNull(headerLineNumber))
            throw new CustomException(HttpStatus.BAD_REQUEST, "You must give header line number for excel file.");
        var bytes = file.getBytes();
        var stringCsv = ExcelBinaryToTextConverter.convert(bytes);
        return CsvExtractor.extractCsvHeaders(stringCsv, headerLineNumber);

    }
}
