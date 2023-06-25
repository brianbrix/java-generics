package io.credable.reconapi.util.pathextractor;

import io.credable.reconapi.exception.CustomException;
import lombok.extern.log4j.Log4j2;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;

import java.io.ByteArrayInputStream;


@Log4j2
public class ExcelBinaryToTextConverter {
    public static String convert(byte[] excelBinaryData) {
        // Get the binary data as a byte array
        // Open the Excel binary data as a workbook
        ByteArrayInputStream inputStream = new ByteArrayInputStream(excelBinaryData);
        Workbook workbook;
        try {
            workbook = new XSSFWorkbook(inputStream);

        } catch (Exception exception) {
            log.info("Could not open workbook using XSSF format for Office Open XML . Trying HSSF...\n" + exception);
            try {
                workbook = new HSSFWorkbook(inputStream);
            } catch (Exception e) {
                throw new CustomException(HttpStatus.BAD_REQUEST, "Unable to extract from given excel format. Error is: " + e);
            }
        }

        StringBuilder finalString = new StringBuilder();
        DataFormatter formatter = new DataFormatter();

        // Iterate over each sheet in the workbook
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet sheet = workbook.getSheetAt(i);
            // Iterate over each row in the sheet
            for (Row row : sheet) {
                StringBuilder rowBuilder = new StringBuilder();

                // Iterate over each cell in the row
                for (Cell cell : row) {
                    // Get the cell value as a string and append it to the row

                    if (cell != null) {
                        // Format the cell value as a string and append it to the row
                        String cellValue = formatter.formatCellValue(cell);
                        rowBuilder.append(cellValue.replaceAll("\"", "\"\"")).append(",");
                    } else {
                        rowBuilder.append(",");
                    }
                }
                String rowString = rowBuilder.toString();

                if (rowString.endsWith(",")) {
                    rowString = rowString.substring(0, rowString.length() - 1);
                }
//                if (rowString.startsWith(",,,")) continue;
                finalString.append(rowString);
                finalString.append("\n");

            }
        }

        return finalString.toString();
    }


}
