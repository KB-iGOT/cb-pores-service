package com.igot.cb.designation.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.igot.cb.designation.entity.DesignationEntity;
import com.igot.cb.designation.repository.DesignationRepository;
import com.igot.cb.designation.service.DesignationService;
import com.igot.cb.pores.cache.CacheService;
import com.igot.cb.pores.elasticsearch.service.EsUtilService;
import com.igot.cb.pores.util.CbServerProperties;
import com.igot.cb.pores.util.Constants;
import com.igot.cb.pores.util.PayloadValidation;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
public class DesignationServiceImpl implements DesignationService {

  @Autowired
  ObjectMapper objectMapper;

  @Autowired
  private DesignationRepository designationRepository;

  @Autowired
  private PayloadValidation payloadValidation;

  @Autowired
  private EsUtilService esUtilService;

  @Autowired
  private CacheService cacheService;

  @Autowired
  private CbServerProperties cbServerProperties;

  @Override
  public void loadDesignationFromExcel(MultipartFile file) {
    log.info("DesignationServiceImpl::loadDesignationFromExcel");
    List<Map<String, String>> processedData = processExcelFile(file);
    log.info("No.of processedData from excel: " + processedData.size());
    JsonNode designationJson = objectMapper.valueToTree(processedData);
    AtomicLong startingId = new AtomicLong(designationRepository.count());
    DesignationEntity designationEntity = new DesignationEntity();
    designationJson.forEach(
        eachDesignation -> {
          String formattedId = String.format("DESG-%06d", startingId.incrementAndGet());
          if (!eachDesignation.isNull()) {
            ((ObjectNode) eachDesignation).put(Constants.ID, formattedId);
            if (eachDesignation.has(Constants.UPDATED_DESIGNATION) && !eachDesignation.get(
                Constants.UPDATED_DESIGNATION).isNull()) {
              ((ObjectNode) eachDesignation).put(Constants.DESIGNATION,
                  eachDesignation.get(Constants.UPDATED_DESIGNATION));
            }
            String descriptionValue =
                (eachDesignation.has(Constants.DESCRIPTION_PAYLOAD) && !eachDesignation.get(
                    Constants.DESCRIPTION_PAYLOAD).isNull())
                    ? eachDesignation.get(Constants.UPDATED_DESIGNATION).asText("")
                    : "";
            ((ObjectNode) eachDesignation).put(Constants.DESCRIPTION, descriptionValue);
            payloadValidation.validatePayload(Constants.DESIGNATION_PAYLOAD_VALIDATION,
                eachDesignation);
            ((ObjectNode) eachDesignation).put(Constants.STATUS, Constants.ACTIVE);
            Timestamp currentTime = new Timestamp(System.currentTimeMillis());
            ((ObjectNode) eachDesignation).put(Constants.CREATED_ON, String.valueOf(currentTime));
            designationEntity.setId(formattedId);
            designationEntity.setData(eachDesignation);
            designationEntity.setIsActive(true);
            designationEntity.setCreatedOn(currentTime);
            designationEntity.setUpdatedOn(currentTime);
            designationRepository.save(designationEntity);
            log.info(
                "DesignationServiceImpl::loadDesignationFromExcel::persited designation in postgres with id: "
                    + formattedId);
            Map<String, Object> map = objectMapper.convertValue(eachDesignation, Map.class);
            esUtilService.addDocument(Constants.DESIGNATION_INDEX_NAME, Constants.INDEX_TYPE,
                formattedId, map, cbServerProperties.getElasticDesignationJsonPath());
            cacheService.putCache(formattedId, eachDesignation);
            log.info(
                "DesignationServiceImpl::loadDesignationFromExcel::created the designation with: "
                    + formattedId);
          }

        });
    log.info("DesignationServiceImpl::loadDesignationFromExcel::created the designations");
  }

  private List<Map<String, String>> processExcelFile(MultipartFile incomingFile) {
    log.info("DesignationServiceImpl::processExcelFile");
    try {
      return validateFileAndProcessRows(incomingFile);
    } catch (Exception e) {
      log.error("Error occurred during file processing: {}", e.getMessage());
      throw new RuntimeException(e.getMessage());
    }
  }

  private List<Map<String, String>> validateFileAndProcessRows(MultipartFile file) {
    log.info("DesignationServiceImpl::validateFileAndProcessRows");
    try (InputStream inputStream = file.getInputStream();
        Workbook workbook = WorkbookFactory.create(inputStream)) {
      Sheet sheet = workbook.getSheetAt(0);
      return processSheetAndSendMessage(sheet);
    } catch (IOException e) {
      log.error("Error while processing Excel file: {}", e.getMessage());
      throw new RuntimeException(e.getMessage());
    }
  }

  private List<Map<String, String>> processSheetAndSendMessage(Sheet sheet) {
    log.info("DesignationServiceImpl::processSheetAndSendMessage");
    try {
      DataFormatter formatter = new DataFormatter();
      Row headerRow = sheet.getRow(0);
      List<Map<String, String>> dataRows = new ArrayList<>();
      for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
        Row dataRow = sheet.getRow(rowIndex);
        if (dataRow == null) {
          break; // No more data rows, exit the loop
        }
        boolean allBlank = true;
        Map<String, String> rowData = new HashMap<>();
        for (int colIndex = 0; colIndex < headerRow.getLastCellNum(); colIndex++) {
          Cell headerCell = headerRow.getCell(colIndex);
          Cell valueCell = dataRow.getCell(colIndex);
          if (headerCell != null && headerCell.getCellType() != CellType.BLANK) {
            String excelHeader =
                formatter.formatCellValue(headerCell).replaceAll("[\\n*]", "").trim();
            String cellValue = "";
            if (valueCell != null && valueCell.getCellType() != CellType.BLANK) {
              if (valueCell.getCellType() == CellType.NUMERIC
                  && DateUtil.isCellDateFormatted(valueCell)) {
                // Handle date format
                Date date = valueCell.getDateCellValue();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                cellValue = dateFormat.format(date);
              } else {
                cellValue = formatter.formatCellValue(valueCell).replace("\n", ",").trim();
              }
              allBlank = false;
            }
            rowData.put(excelHeader, cellValue);
          }
        }
        log.info("Data Rows: " + rowData);
        if (allBlank) {
          break; // If all cells are blank in the current row, stop processing
        }
        dataRows.add(rowData);
      }
      log.info("Number of Data Rows Processed: " + dataRows.size());
      return dataRows;
    } catch (Exception e) {
      log.error(e.getMessage());
      throw new RuntimeException(e.getMessage());
    }
  }

}
