package com.zorroa.archivist.processors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Maps;
import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.exception.IngestException;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import com.zorroa.archivist.sdk.util.Json;
import org.apache.poi.hssf.util.CellReference;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * A simple excel ingestor based on Apache POI.  This particular processor handles .xlsx files.
 **
 */
public class SimpleExcelIngestor extends IngestProcessor {

    private Logger logger = LoggerFactory.getLogger(SimpleExcelIngestor.class);

    private FileInputStream excelFile;
    private List<Mapping> mappings;
    private XSSFWorkbook workBook;
    private XSSFSheet sheet;

    /**
     * You must specify these args:
     *
     * - file: path to the excel file
     * - sheet: the name of the sheet the data is on. Defaults to first sheet if not specified.
     * - mappings: a big structure which maps cells to elastic attrs.
     *
     * @throws Exception
     */
    @Override
    public void init() {

        try {
            String fileName = getArg("file");
            excelFile = new FileInputStream(fileName);
            workBook = new XSSFWorkbook(excelFile);

            String sheetName = getArg("sheet");
            if (sheetName == null) {
                sheet = workBook.getSheetAt(0);
            } else {
                sheet = workBook.getSheet(sheetName);
            }

            mappings = Json.Mapper.convertValue(
                    getArg("mappings"), new TypeReference<List<Mapping>>() {
                    });
        }
        catch (Exception e) {
            throw new IngestException(e.getMessage(), e);
        }
    }

    @Override
    public void process(AssetBuilder assetBuilder) {

        for (Mapping mapping: mappings) {
            //row.getCell(CellReference.convertColStringToIndex(query.getColumn()));

            Row row = filter(mapping.getFilters());
            if (row == null) {
                continue;
            }

            for (Map.Entry<String, Field> entry: mapping.getFields().entrySet()) {
                String column = entry.getKey();
                Field field = entry.getValue();
                Cell cell = row.getCell(CellReference.convertColStringToIndex(column));

                switch (field.getType()) {
                    case integer:
                        assetBuilder.setAttr(field.getNamespace(), field.getField(), new Double(cell.getNumericCellValue()).intValue());
                        break;
                    case decimal:
                        assetBuilder.setAttr(field.getNamespace(), field.getField(),cell.getNumericCellValue());
                        break;
                    case bool:
                        assetBuilder.setAttr(field.getNamespace(), field.getField(), cell.getBooleanCellValue());
                        break;
                    default:
                        assetBuilder.setAttr(field.getNamespace(), field.getField(), cell.getStringCellValue());
                        break;
                }
            }
        }
    }

    private Row filter(List<Filter> filters) {

        for (Row row: sheet) {
            if (matchFilters(row, filters)) {
                return row;
            }
        }
        return null;
    }


    private boolean matchFilters(Row row, List<Filter> filters) {
        int matchesNeeded = filters.size();
        for (Filter filter: filters) {
            if (!matchFilter(row, filter)) {
                return false;
            }
            else {
                matchesNeeded--;
            }
        }
        return matchesNeeded == 0;
    }

    private boolean matchFilter(Row row, Filter filter) {
        Cell cell = row.getCell(CellReference.convertColStringToIndex(filter.getColumn()));
        int cellType = cell.getCellType();
        switch (cellType) {
            case Cell.CELL_TYPE_STRING:
                return compareValue(cell.getStringCellValue(), filter);
            case Cell.CELL_TYPE_NUMERIC:
                return compareValue(BigDecimal.valueOf(cell.getNumericCellValue()), filter);
            default:
                return false;
        }
    }

    private boolean compareValue(String cellValue, Filter filter) {
        switch (filter.getRelation()) {
            case is:
                return cellValue.equals(filter.getValue());
            case is_not:
                return !cellValue.equals(filter.getValue());
            default:
                return false;
        }
    }

    private boolean compareValue(BigDecimal cellValue, Filter filter) {
        boolean result;

        BigDecimal value = new BigDecimal(((Number)filter.getValue()).doubleValue());
        switch (filter.getRelation()) {
            case is:
                result = cellValue.compareTo(value) == 0;
                break;
            case is_not:
                result = cellValue.compareTo(value) != 0;
                break;
            default:
                result = false;
        }
        return result;
    }

    /**
     * All the classes associated with the mapping structure.  An example Json string
     * might look like this:
     *
     * This is an array of Mapping classes.  Each mapping class defines the fields
     * to be mapped and a list of filters. The key to the fields map is the column
     * letter in the spreadsheet, the value is the attribute you want to set on the Asset
     * along with a type.
     *
     * The filters will result in either a null (no match) or a single row.  The combination
     * of the column deinfed in fields and the row result in cells which contain data.  The
     * data in those cells is set on the AssetBuilder.
     *
     *
        [ {
            "fields" : {
                "B" : {
                    "attr" : "foo.B",
                            "type" : 0
                },
                "C" : {
                    "attr" : "foo.C",
                            "type" : 1
                },
                "D" : {
                    "attr" : "foo.D",
                            "type" : 0
                },
                "E" : {
                    "attr" : "foo.E",
                            "type" : 2
                }
            },
            "filters" : [ {
                "column" : "C",
                        "relation" : 0,
                        "value" : 7
            } ]
        } ]
    */

    public static class Field {

        private String attr;
        private Type type;

        public Field() { }

        public Field(String attr, Type type) {
            this.attr = attr;
            this.type = type;
        }

        public Type getType() {
            return type;
        }

        public void setType(Type type) {
            this.type = type;
        }

        @JsonIgnore
        public String getNamespace() {
            /*
             * A temp solution for if a namespace isn't defined.
             */
            if (!attr.contains(".")) {
                return "attrs";
            }
            return attr.split("\\.")[0];
        }

        @JsonIgnore
        public String getField() {
            /*
             * A temp solution for if a namespace isn't defined.
             */
            if (!attr.contains(".")) {
                return attr;
            }
            return attr.split("\\.")[1];
        }

        public String getAttr() {
            return attr;
        }

        public void setAttr(String attr) {
            this.attr = attr;
        }

    }

    public static class Mapping {
        Map<String, Field> fields = Maps.newHashMap();
        private List<Filter> filters;

        public Map<String, Field> getFields() {
            return fields;
        }

        public void setFields(Map<String, Field> fields) {
            this.fields = fields;
        }

        public void addField(String column, Field field) {
            fields.put(column, field);
        }

        public List<Filter> getFilters() {
            return filters;
        }

        public void setFilters(List<Filter> filters) {
            this.filters = filters;
        }
    }

    public enum Relation {
        is,
        is_not
    }

    public enum Type {
        string,
        integer,
        decimal,
        bool
    }

    public static class Filter {
        private String column;
        private Relation relation;
        private Object value;

        public String getColumn() {
            return column;
        }

        public void setColumn(String column) {
            this.column = column;
        }

        public Relation getRelation() {
            return relation;
        }

        public void setRelation(Relation relation) {
            this.relation = relation;
        }

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }
    }
}
