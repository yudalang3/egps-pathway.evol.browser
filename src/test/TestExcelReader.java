package test;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Test program to verify Excel reading with empty cells.
 */
public class TestExcelReader {

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.out.println("Usage: java TestExcelReader <excel-file>");
            return;
        }

        File file = new File(args[0]);
        if (!file.exists()) {
            System.out.println("File not found: " + file.getAbsolutePath());
            return;
        }

        System.out.println("Reading file: " + file.getAbsolutePath());
        System.out.println("=".repeat(80));

        String fileName = file.getName().toLowerCase();
        boolean isXlsx = fileName.endsWith(".xlsx");

        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = isXlsx ? new XSSFWorkbook(fis) : new HSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null || sheet.getPhysicalNumberOfRows() == 0) {
                System.out.println("Empty sheet!");
                return;
            }

            // Determine the maximum number of columns from the header row
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) {
                System.out.println("No header row!");
                return;
            }

            int columnCount = headerRow.getLastCellNum();
            System.out.println("Column count: " + columnCount);
            System.out.println();

            // Read headers
            List<String> headers = new ArrayList<>(columnCount);
            for (int col = 0; col < columnCount; col++) {
                Cell cell = headerRow.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                headers.add(convertCellToString(cell));
            }

            System.out.println("Headers:");
            for (int i = 0; i < headers.size(); i++) {
                System.out.printf("  [%d] %s%n", i, headers.get(i));
            }
            System.out.println();

            // Read data rows
            int firstDataRow = sheet.getFirstRowNum() + 1;
            int lastRow = sheet.getLastRowNum();

            System.out.println("Data rows (" + (lastRow - firstDataRow + 1) + " rows):");
            System.out.println("-".repeat(80));

            for (int rowIdx = firstDataRow; rowIdx <= lastRow && rowIdx < firstDataRow + 10; rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                if (row == null) {
                    System.out.println("Row " + rowIdx + ": <null row>");
                    continue;
                }

                System.out.println("Row " + rowIdx + ":");
                for (int col = 0; col < columnCount; col++) {
                    Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    String value = convertCellToString(cell);
                    String displayValue = value.isEmpty() ? "<EMPTY>" : value;
                    System.out.printf("  [%d] %s = %s%n", col, headers.get(col), displayValue);
                }
                System.out.println();
            }

            if (lastRow - firstDataRow + 1 > 10) {
                System.out.println("... (showing first 10 rows only)");
            }
        }
    }

    private static String convertCellToString(Cell cell) {
        if (cell == null) {
            return "";
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                double value = cell.getNumericCellValue();
                if (value == Math.floor(value) && !Double.isInfinite(value)) {
                    yield new DecimalFormat("0").format(value);
                }
                yield String.valueOf(value);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            case BLANK -> "";
            default -> "";
        };
    }
}
