package io.credable.reconapi.util.pathextractor;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import io.credable.reconapi.exception.CustomException;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static java.util.Objects.isNull;

@Log4j2
public class CsvExtractor {

    protected static Set<String> extractCsvHeaders(String csv, int headerLineNumber) throws IOException, FileNotFoundException, CsvException {
        CSVReader reader = new CSVReaderBuilder(new StringReader(csv))
                .build();
        log.info("Header line number: " + headerLineNumber);
        Set<String> headerSet = new HashSet<>();
        for (int i = 1; i <= headerLineNumber; i++) {
            if (i == headerLineNumber) {
                String[] headerRow = reader.readNext();
                headerSet.addAll(Arrays.asList(headerRow));
            } else
                reader.readNext();
        }


        log.info("Headers: {}", headerSet);

        return headerSet;
    }

    public static Set<String> extract(MultipartFile file, Integer headerLineNumber) {
        if (isNull(headerLineNumber))
            throw new CustomException(HttpStatus.BAD_REQUEST, "You must give header line number for csv file.");

        try (InputStream inputStream = file.getInputStream()) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append("\n");
            }
            String fileContentAsString = stringBuilder.toString();
            return extractCsvHeaders(fileContentAsString, headerLineNumber);

        } catch (CsvException | IOException e) {
            log.error(e.getMessage(), e);
            throw new CustomException(HttpStatus.BAD_REQUEST, "Unable to extract columns.");
        }
    }

}
