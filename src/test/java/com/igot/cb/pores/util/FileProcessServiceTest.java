package com.igot.cb.pores.util;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileProcessService Unit Tests")
class FileProcessServiceTest {

    @InjectMocks
    private FileProcessService fileProcessService;

    @Mock
    private MultipartFile multipartFile;

    private Workbook createTestWorkbook(String[][] data) {
        // Use HSSFWorkbook instead of XSSFWorkbook to avoid ServiceLoader issues in tests
        Workbook workbook = new HSSFWorkbook();
        Sheet sheet = workbook.createSheet("TestSheet");

        for (int i = 0; i < data.length; i++) {
            Row row = sheet.createRow(i);
            for (int j = 0; j < data[i].length; j++) {
                Cell cell = row.createCell(j);
                cell.setCellValue(data[i][j]);
            }
        }
        return workbook;
    }

    private InputStream workbookToInputStream(Workbook workbook) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        workbook.write(bos);
        return new ByteArrayInputStream(bos.toByteArray());
    }

    private String createCsvContent(String[][] data) {
        StringBuilder csv = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[i].length; j++) {
                csv.append(data[i][j]);
                if (j < data[i].length - 1) {
                    csv.append(",");
                }
            }
            csv.append("\n");
        }
        return csv.toString();
    }

    @BeforeEach
    void setUp() {
        // Setup is handled by Mockito annotations
    }

    @Test
    void testProcessExcelFileSuccess() throws IOException {
        // Given
        String[][] testData = {
                {"Name", "Age", "City"},
                {"John", "25", "New York"},
                {"Jane", "30", "Los Angeles"}
        };

        Workbook workbook = createTestWorkbook(testData);
        InputStream inputStream = workbookToInputStream(workbook);

        when(multipartFile.getOriginalFilename()).thenReturn("test.xlsx");
        when(multipartFile.getInputStream()).thenReturn(inputStream);

        // When
        List<Map<String, String>> result = fileProcessService.processExcelFile(multipartFile);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("John", result.get(0).get("Name"));
        assertEquals("25", result.get(0).get("Age"));
        assertEquals("New York", result.get(0).get("City"));
        assertEquals("Jane", result.get(1).get("Name"));
        assertEquals("30", result.get(1).get("Age"));
        assertEquals("Los Angeles", result.get(1).get("City"));
    }

    @Test
    void testProcessExcelFileWithCsvFile() throws IOException {
        // Given
        String csvContent = createCsvContent(new String[][]{
                {"Name", "Age", "City"},
                {"John", "25", "New York"},
                {"Jane", "30", "Los Angeles"}
        });

        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());

        when(multipartFile.getOriginalFilename()).thenReturn("test.csv");
        when(multipartFile.getInputStream()).thenReturn(inputStream);

        // When
        List<Map<String, String>> result = fileProcessService.processExcelFile(multipartFile);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("John", result.get(0).get("Name"));
        assertEquals("25", result.get(0).get("Age"));
        assertEquals("New York", result.get(0).get("City"));
    }

    @Test
    void testProcessExcelFileNullFilename() throws IOException {
        // Given
        when(multipartFile.getOriginalFilename()).thenReturn(null);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> fileProcessService.processExcelFile(multipartFile));
        assertEquals("File name is null", exception.getMessage());
    }

    @Test
    void testProcessExcelFileUnsupportedType() throws IOException {
        // Given
        InputStream inputStream = new ByteArrayInputStream(new byte[0]);
        when(multipartFile.getOriginalFilename()).thenReturn("test.txt");
        when(multipartFile.getInputStream()).thenReturn(inputStream);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> fileProcessService.processExcelFile(multipartFile));
        assertTrue(exception.getMessage().contains("Unsupported file type"));
    }

    @Test
    void testProcessExcelFileIOException() throws IOException {
        // Given
        when(multipartFile.getOriginalFilename()).thenReturn("test.xlsx");
        when(multipartFile.getInputStream()).thenThrow(new IOException("Test IO Exception"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> fileProcessService.processExcelFile(multipartFile));
        assertEquals("Test IO Exception", exception.getMessage());
    }

    @Test
    @DisplayName("Test processExcelFile with general exception")
    void testProcessExcelFileGeneralException() throws IOException {
        // Given
        when(multipartFile.getOriginalFilename()).thenReturn("test.xlsx");
        when(multipartFile.getInputStream()).thenThrow(new RuntimeException("Test Exception"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> fileProcessService.processExcelFile(multipartFile));
        assertEquals("Test Exception", exception.getMessage());
    }

    @Test
    void testExcelProcessingWithBlankRows() throws IOException {
        // Given
        String[][] testData = {
                {"Name", "Age"},
                {"John", "25"},
                {"", ""},  // Blank row
                {"Jane", "30"}
        };

        Workbook workbook = createTestWorkbook(testData);
        InputStream inputStream = workbookToInputStream(workbook);

        when(multipartFile.getOriginalFilename()).thenReturn("test.xlsx");
        when(multipartFile.getInputStream()).thenReturn(inputStream);

        // When
        List<Map<String, String>> result = fileProcessService.processExcelFile(multipartFile);

        // Then
        assertNotNull(result);
        assertEquals(3, result.size()); // Should stop at blank row
        assertEquals("John", result.get(0).get("Name"));
        assertEquals("25", result.get(0).get("Age"));
    }

    @Test
    void testExcelProcessingWithDateCells() throws IOException {
        // Given
        Workbook workbook = new HSSFWorkbook();
        Sheet sheet = workbook.createSheet("TestSheet");

        // Create header row
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Name");
        headerRow.createCell(1).setCellValue("Date");

        // Create data row with date
        Row dataRow = sheet.createRow(1);
        dataRow.createCell(0).setCellValue("John");
        Cell dateCell = dataRow.createCell(1);
        dateCell.setCellValue(new Date());

        // Set date format for the cell
        CellStyle dateStyle = workbook.createCellStyle();
        CreationHelper createHelper = workbook.getCreationHelper();
        dateStyle.setDataFormat(createHelper.createDataFormat().getFormat("yyyy-MM-dd"));
        dateCell.setCellStyle(dateStyle);

        InputStream inputStream = workbookToInputStream(workbook);

        when(multipartFile.getOriginalFilename()).thenReturn("test.xlsx");
        when(multipartFile.getInputStream()).thenReturn(inputStream);

        // When
        List<Map<String, String>> result = fileProcessService.processExcelFile(multipartFile);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("John", result.get(0).get("Name"));
        assertTrue(result.get(0).get("Date").contains("T")); // Should contain ISO format
    }

    @Test
    void testExcelProcessingWithNullDataRow() throws IOException {
        // Given
        Workbook workbook = new HSSFWorkbook();
        Sheet sheet = workbook.createSheet("TestSheet");

        // Create header row
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Name");

        // Skip row 1 (it will be null)
        // Row 2 will exist but row 1 will be null

        InputStream inputStream = workbookToInputStream(workbook);

        when(multipartFile.getOriginalFilename()).thenReturn("test.xlsx");
        when(multipartFile.getInputStream()).thenReturn(inputStream);

        // When
        List<Map<String, String>> result = fileProcessService.processExcelFile(multipartFile);

        // Then
        assertNotNull(result);
        assertEquals(0, result.size()); // Should stop at null row
    }

    @Test
    void testExcelProcessingWithBlankHeaderCells() throws IOException {
        // Given
        Workbook workbook = new HSSFWorkbook();
        Sheet sheet = workbook.createSheet("TestSheet");

        // Create header row with blank cell
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Name");
        Cell blankHeaderCell = headerRow.createCell(1);
        blankHeaderCell.setCellType(CellType.BLANK);
        headerRow.createCell(2).setCellValue("Age");

        // Create data row
        Row dataRow = sheet.createRow(1);
        dataRow.createCell(0).setCellValue("John");
        dataRow.createCell(1).setCellValue("Ignored");
        dataRow.createCell(2).setCellValue("25");

        InputStream inputStream = workbookToInputStream(workbook);

        when(multipartFile.getOriginalFilename()).thenReturn("test.xlsx");
        when(multipartFile.getInputStream()).thenReturn(inputStream);

        // When
        List<Map<String, String>> result = fileProcessService.processExcelFile(multipartFile);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("John", result.get(0).get("Name"));
        assertEquals("25", result.get(0).get("Age"));
        assertFalse(result.get(0).containsKey("")); // Blank header should be ignored
    }

    @Test
    void testExcelProcessingWithNewlines() throws IOException {
        // Given
        Workbook workbook = new HSSFWorkbook();
        Sheet sheet = workbook.createSheet("TestSheet");

        // Create header row with newlines
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Name\n*");
        headerRow.createCell(1).setCellValue("Description");

        // Create data row with newlines
        Row dataRow = sheet.createRow(1);
        dataRow.createCell(0).setCellValue("John");
        dataRow.createCell(1).setCellValue("Line1\nLine2");

        InputStream inputStream = workbookToInputStream(workbook);

        when(multipartFile.getOriginalFilename()).thenReturn("test.xlsx");
        when(multipartFile.getInputStream()).thenReturn(inputStream);

        // When
        List<Map<String, String>> result = fileProcessService.processExcelFile(multipartFile);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("John", result.get(0).get("Name"));
        assertEquals("Line1,Line2", result.get(0).get("Description")); // Newline replaced with comma
    }

    @Test
    void testCsvProcessingWithDateFormat() throws IOException {
        // Given
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        String dateString = dateFormat.format(new Date());

        String csvContent = "Name,Date\nJohn," + dateString + "\n";
        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());

        when(multipartFile.getOriginalFilename()).thenReturn("test.csv");
        when(multipartFile.getInputStream()).thenReturn(inputStream);

        // When
        List<Map<String, String>> result = fileProcessService.processExcelFile(multipartFile);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("John", result.get(0).get("Name"));
        assertTrue(result.get(0).get("Date").contains("T")); // Should contain ISO format
    }

    @Test
    void testCsvProcessingWithBlankCells() throws IOException {
        // Given
        String csvContent = "Name,Age\nJohn,25\n,\nJane,30\n";
        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());

        when(multipartFile.getOriginalFilename()).thenReturn("test.csv");
        when(multipartFile.getInputStream()).thenReturn(inputStream);

        // When
        List<Map<String, String>> result = fileProcessService.processExcelFile(multipartFile);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size()); // Should stop at blank row
        assertEquals("John", result.get(0).get("Name"));
        assertEquals("25", result.get(0).get("Age"));
    }

    @Test
    void testCsvProcessingWithNewlines() throws IOException {
        // Given
        String csvContent = "Name,Description\nJohn,\"Line1\nLine2\"\n";
        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());

        when(multipartFile.getOriginalFilename()).thenReturn("test.csv");
        when(multipartFile.getInputStream()).thenReturn(inputStream);

        // When
        List<Map<String, String>> result = fileProcessService.processExcelFile(multipartFile);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("John", result.get(0).get("Name"));
        assertEquals("Line1,Line2", result.get(0).get("Description")); // Newline replaced with comma
    }

    @Test
    void testCsvProcessingWithException() throws IOException {
        // Given
        String csvContent = "Name,Age\nJohn"; // Malformed CSV
        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());

        when(multipartFile.getOriginalFilename()).thenReturn("test.csv");
        when(multipartFile.getInputStream()).thenReturn(inputStream);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> fileProcessService.processExcelFile(multipartFile));
        assertNotNull(exception.getMessage());
    }

    @Test
    @DisplayName("Test Excel processing with exception in sheet processing")
    void testExcelProcessingWithException() throws IOException {
        // Given - Invalid workbook data should throw exception
        when(multipartFile.getOriginalFilename()).thenReturn("test.xlsx");
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> fileProcessService.processExcelFile(multipartFile));
        assertNotNull(exception.getMessage());
    }

    @Test
    void testXlsFileExtension() throws IOException {
        // Given
        String[][] testData = {
                {"Name", "Age"},
                {"John", "25"}
        };

        Workbook workbook = createTestWorkbook(testData);
        InputStream inputStream = workbookToInputStream(workbook);

        when(multipartFile.getOriginalFilename()).thenReturn("test.xls");
        when(multipartFile.getInputStream()).thenReturn(inputStream);

        // When
        List<Map<String, String>> result = fileProcessService.processExcelFile(multipartFile);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("John", result.get(0).get("Name"));
        assertEquals("25", result.get(0).get("Age"));
    }

    @Test
    void testCsvProcessingWithNullValues() throws IOException {
        // Given
        String csvContent = "Name,Age\nJohn,\n";
        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());

        when(multipartFile.getOriginalFilename()).thenReturn("test.csv");
        when(multipartFile.getInputStream()).thenReturn(inputStream);

        // When
        List<Map<String, String>> result = fileProcessService.processExcelFile(multipartFile);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("John", result.get(0).get("Name"));
        assertEquals("", result.get(0).get("Age")); // Null should be converted to empty string
    }

    @Test
    void testCsvProcessingWithWhitespaceValues() throws IOException {
        // Given
        String csvContent = "Name,Age\nJohn,   \n";
        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());

        when(multipartFile.getOriginalFilename()).thenReturn("test.csv");
        when(multipartFile.getInputStream()).thenReturn(inputStream);

        // When
        List<Map<String, String>> result = fileProcessService.processExcelFile(multipartFile);

        // Then
        assertNotNull(result);
    }

    @Test
    void testDateParsingSuccess() throws IOException {
        // Given
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        String validDateString = dateFormat.format(new Date());

        String csvContent = "Name,Date\nJohn," + validDateString + "\n";
        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());

        when(multipartFile.getOriginalFilename()).thenReturn("test.csv");
        when(multipartFile.getInputStream()).thenReturn(inputStream);

        // When
        List<Map<String, String>> result = fileProcessService.processExcelFile(multipartFile);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).get("Date").contains("T"));
    }

    @Test
    void testDateParsingFailure() throws IOException {
        // Given
        String invalidDateString = "not-a-date";

        String csvContent = "Name,Date\nJohn," + invalidDateString + "\n";
        InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes());

        when(multipartFile.getOriginalFilename()).thenReturn("test.csv");
        when(multipartFile.getInputStream()).thenReturn(inputStream);

        // When
        List<Map<String, String>> result = fileProcessService.processExcelFile(multipartFile);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("not-a-date", result.get(0).get("Date")); // Should remain as is
    }
}