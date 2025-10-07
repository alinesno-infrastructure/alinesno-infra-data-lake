package com.alinesno.infra.data.lake.storage.utils;

import com.alinesno.infra.data.lake.storage.dto.InsertDataDto;
import org.apache.poi.ss.usermodel.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Excel解析工具类
 */
public class ExcelParserUtils {

    /**
     * 解析Excel文件并转换为InsertDataDto
     */
    public static InsertDataDto parseExcelFile(MultipartFile multipartFile, Long tableId) throws Exception {
        InsertDataDto insertDataDto = new InsertDataDto();
        insertDataDto.setTableId(tableId.toString());
        
        List<Map<String, Object>> dataMapList = new ArrayList<>();
        
        try (Workbook workbook = WorkbookFactory.create(multipartFile.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0); // 获取第一个工作表
            
            // 获取表头（第一行）
            Row headerRow = sheet.getRow(0);
            List<String> headers = new ArrayList<>();
            
            if (headerRow != null) {
                for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                    Cell cell = headerRow.getCell(i);
                    headers.add(getCellValueAsString(cell));
                }
            }
            
            // 遍历数据行（从第二行开始）
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row dataRow = sheet.getRow(i);
                if (dataRow == null) {
                    continue;
                }
                
                Map<String, Object> rowData = new HashMap<>();
                
                for (int j = 0; j < headers.size(); j++) {
                    String header = headers.get(j);
                    Cell cell = dataRow.getCell(j);
                    Object cellValue = getCellValue(cell);
                    rowData.put(header, cellValue);
                }
                
                dataMapList.add(rowData);
            }
        }
        
        insertDataDto.setDataMap(dataMapList);
        return insertDataDto;
    }

    /**
     * 获取单元格的值
     */
    private static Object getCellValue(Cell cell) {
        if (cell == null) {
            return null;
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue();
                } else {
                    return cell.getNumericCellValue();
                }
            case BOOLEAN:
                return cell.getBooleanCellValue();
            case FORMULA:
                return cell.getCellFormula();
            case BLANK:
                return null;
            default:
                return null;
        }
    }

    /**
     * 获取单元格的字符串值
     */
    private static String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf(cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            case BLANK:
                return "";
            default:
                return "";
        }
    }
}