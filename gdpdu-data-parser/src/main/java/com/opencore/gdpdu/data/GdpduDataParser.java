/*
 * Licensed to OpenCore GmbH & Co. KG under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information regarding copyright ownership.
 * OpenCore GmbH & Co. KG licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */
package com.opencore.gdpdu.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.validation.ConstraintViolation;

import com.opencore.gdpdu.common.exceptions.ParsingException;
import com.opencore.gdpdu.common.util.ClassRegistry;
import com.opencore.gdpdu.common.util.ColumnInfo;
import com.opencore.gdpdu.data.deserializers.DeserializationContext;
import com.opencore.gdpdu.data.deserializers.Deserializers;
import com.opencore.gdpdu.index.GdpduIndexParser;
import com.opencore.gdpdu.index.GdpduIndexValidator;
import com.opencore.gdpdu.index.annotations.Column;
import com.opencore.gdpdu.index.models.DataSet;
import com.opencore.gdpdu.index.models.Media;
import com.opencore.gdpdu.index.models.Range;
import com.opencore.gdpdu.index.models.Table;
import com.opencore.gdpdu.index.models.VariableColumn;
import com.opencore.gdpdu.index.models.VariableLength;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: Make it so that index.xml doesn't need to be parsed again for each table

/**
 * This class can be used to read GDPdu conformant data files into Java objects.
 * These objects have to be annotated with the {@link Column} annotation.
 *
 * This parser is not thread-safe.
 */
public class GdpduDataParser {

  private static final Logger LOG = LoggerFactory.getLogger(GdpduDataParser.class);

  /**
   * Parses a single {@link Table} from a specific {@code index.xml} file into a List of domain objects.
   *
   * This will search for all files in a specific directory.
   */
  public static <T> List<T> parseTable(String indexXml, String tableName, Class<T> clazz) throws ParsingException {
    Objects.requireNonNull(indexXml, "`indexXml` can't be null");

    // TODO Validate that it can be read etc. I have a method somewhere
    File indexXmlFile = new File(indexXml);

    DataSet dataSet;
    try {
      dataSet = parseIndexXml(new FileInputStream(indexXmlFile));
    } catch (FileNotFoundException e) {
      throw new ParsingException(e);
    }
    validateDataSet(dataSet);

    // Try to find the table in our index.xml file
    Table table = null;
    for (Media tmpMedia : dataSet.getMedia()) {
      for (Table tmpTable : tmpMedia.getTables()) {
        if (tableName.equals(tmpTable.getName()) || tableName.equals(tmpTable.getUrl())) {
          table = tmpTable;
          break;
        }
      }
    }
    if (table == null) {
      LOG.error("Table [{}] could not be found, aborting", tableName);
      throw new ParsingException("Table could not be found, aborting");
    }

    File dataFile = new File(indexXmlFile.getAbsoluteFile().getParentFile(), table.getUrl());
    try (InputStream fis = new FileInputStream(dataFile)) {
      return parseTable(fis, table, clazz);
    } catch (IOException e) {
      throw new ParsingException(e);
    }
  }

  /**
   * This parses a table from an InputStream.
   * It is your responsibility to pass in a properly constructed {@link Table} object.
   */
  @SuppressWarnings("WeakerAccess")
  public static <T> List<T> parseTable(InputStream tableStream, Table table, Class<T> clazz) throws ParsingException {
    Objects.requireNonNull(tableStream, "`tableStream` can't be null");
    Objects.requireNonNull(table, "'table' can't be null");
    Objects.requireNonNull(clazz, "'clazz' can't be null");

    ClassRegistry.registerClass(clazz);

    // TODO: Make this a choice or a separate step all together, this method starts to do a lot of different things
    //validateClass(clazz, table);
    List<String> errors = GdpduIndexValidator.validateTableAgainstClass(clazz, table);
    if (!errors.isEmpty()) {
      for (String error : errors) {
        LOG.warn(error);
      }
      // TODO: Make this more informative
      throw new ParsingException("index.xml does not match clazz");
    }

    if (table.getVariableLength() != null) {
      LOG.trace("[{}] is VariableLength table, parsing now", table.getName());
      return parseVariableLengthTable(tableStream, table, clazz);
    } else if (table.getFixedLength() != null) {
      LOG.trace("[{}] is FixedLength table, parsing now", table.getName());
      //TODO Support fixedLength
      throw new UnsupportedOperationException("FixedLength not supported yet");
    } else {
      throw new ParsingException("Neither VariableLength nor FixedLength found, aborting");
    }
  }

  /**
   * This method parses a variable length (i.e. "CSV"/"TSV", ...) table into domain objects.
   * It uses two phases: Parse into generic records and then map to domain objects.
   */
  // TODO: Switch to a custom parser and remove the Commons CSV dependency
  @SuppressWarnings("WeakerAccess")
  public static <T> List<T> parseVariableLengthTable(InputStream tableStream, Table table, Class<T> clazz) throws ParsingException {
    Objects.requireNonNull(tableStream, "'tableStream' can't be null");
    Objects.requireNonNull(table, "'table' can't be null");
    Objects.requireNonNull(clazz, "'clazz' can't be null");

    // Construct the proper CSVFormat
    // NOTE: Apache Commons CSV does not support specifying the line ending
    VariableLength variableLength = table.getVariableLength();
    CSVFormat format = CSVFormat
      .newFormat(variableLength.getColumnDelimiter().charAt(0))
      .withQuote(variableLength.getTextEncapsulator().charAt(0));
    LOG.debug("Parsing with settings: [{}]", format);

    // We first parse the file into a generic "record" that's based only on Strings
    CSVParser parser;
    try {
      // TODO: Don't hardcode the charset, take it from the index.xml file
      parser = CSVParser.parse(tableStream, Charset.forName("Cp1252"), format);
    } catch (IOException e) {
      throw new ParsingException(e);
    }

    LongRange range = fillDefaults(table.getRange());

    // Convert the "generic" record into the specific type
    DeserializationContext context = new DeserializationContext();
    context.setDecimalSymbol(table.getDecimalSymbol());
    context.setDigitGroupingSymbol(table.getDigitGroupingSymbol());
    context.setTrim(false);

    List<T> results = new ArrayList<>();
    try {
      // GDPdU seems to be "1" based. We increment the index at the beginning of the loop so we start at 0 here
      int index = 0;
      int count = 0;
      for (CSVRecord record : parser) {
        index++;
        if (index < range.from) {
          continue;
        }
        if (index > range.to) {
          break;
        }
        if (count > range.length) {
          break;
        }
        count++;

        // TODO: Do something with these errors and make it configurable whether to abort on error or not
        List<ParsingException> exceptions = new ArrayList<>();
        try {
          results.add(parseRecord(record, table, context, clazz));
        } catch (ParsingException e) {
          LOG.warn("Encountered error while parsing record [{}] from table [{}] into class [{}]", record, table.getName(), clazz, e);
          exceptions.add(e);
        }
      }
    } catch (IllegalStateException e) {
      // TODO Handle
      LOG.error("Exception parsing", e);
    }
    return results;
  }

  private static <T> T newInstance(Class<T> clazz) throws ParsingException {
    T t;
    try {
      t = clazz.getDeclaredConstructor().newInstance();
    } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
      throw new ParsingException(e);
    }
    return t;
  }

  /**
   * This takes a value that we read from an input data file and tries to serialize it into a Java object.
   */
  private static <T> void deserializeValue(T t, VariableColumn currentColumn, String currentValue, DeserializationContext context, Method method) throws ParsingException {
    if (method == null) {
      return;
    }

    try {
      Class<?> parameterType = method.getParameterTypes()[0]; // In the ClassRegistry we checked that every write method has exactly one parameter
      Object object = Deserializers.deserialize(currentValue, parameterType, currentColumn.getDataType(), context);
      method.invoke(t, object);
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw new ParsingException(e);
    }
  }

  private static void validateDataSet(DataSet dataSet) throws ParsingException {
    Set<ConstraintViolation<DataSet>> constraintViolations = GdpduIndexValidator.validateDataSet(dataSet);
    if (!constraintViolations.isEmpty()) {
      for (ConstraintViolation<DataSet> constraintViolation : constraintViolations) {
        LOG.warn("Violation found [{}] [{}]", constraintViolation.getPropertyPath(), constraintViolation.getMessage());
      }
      throw new ParsingException("invalid index.xml file");
    }
  }

  private static DataSet parseIndexXml(InputStream indexXmlFile) throws ParsingException {
    LOG.trace("Beginning to parse index.xml");
    DataSet dataSet;
    try {
      dataSet = GdpduIndexParser.parseXmlFile(indexXmlFile);
    } catch (IOException e) {
      throw new ParsingException(e);
    }
    LOG.debug("Successfully parsed index.xml");
    return dataSet;
  }

  // TODO: Need to deal with values that are not valid numbers
  private static LongRange fillDefaults(Range range) {
    LongRange longRange = new LongRange();
    if (range == null) {
      return longRange;
    }
    if (range.getFrom() != null && !range.getFrom().isBlank()) {
      longRange.from = Long.parseLong(range.getFrom());
    }
    if (range.getTo() != null && !range.getTo().isBlank()) {
      longRange.to = Long.parseLong(range.getTo());
    }
    if (range.getLength() != null && !range.getLength().isBlank()) {
      longRange.to = Long.parseLong(range.getLength());
    }
    return longRange;
  }

  // TODO: This is specific to variable length stuff now, need to see if this can be made generic enough to support fixed length as well
  private static <T> T parseRecord(CSVRecord record, Table table, DeserializationContext context, Class<T> clazz) throws ParsingException {
    Objects.requireNonNull(record, "'record' can't be null");
    Objects.requireNonNull(table, "'table' can't be null");
    Objects.requireNonNull(clazz, "'clazz' can't be null");

    Map<String, ColumnInfo> writeMethods = ClassRegistry.getClassInformation(clazz);

    // TODO: This repeats for every row, move this up
    List<VariableColumn> columns = new ArrayList<>();
    columns.addAll(table.getVariableLength().getVariablePrimaryKeys());
    columns.addAll(table.getVariableLength().getVariableColumns());

    if (columns.size() != record.size()) {
      throw new ParsingException("The table definition has [" + columns.size() + "] columns, but the parsed record has [" + record.size() + "]");
    }

    T t = newInstance(clazz);
    // The order of columns is not specified by a header in the file but by the order in the XML file
    // So we iterate over the defined columns here
    for (int i = 0; i < columns.size(); i++) {
      VariableColumn currentColumn = columns.get(i);
      String currentValue = record.get(i);

      // Validation happens later
      if (currentValue == null || currentValue.isBlank()) {
        LOG.trace("Skipping empty value for column [{}]", currentColumn.getName());
        continue;
      }

      // Here we deserialize the Strings into strongly typed values depending on their type
      Method method = writeMethods.get(currentColumn.getName()).setter;
      deserializeValue(t, currentColumn, currentValue, context, method);
    }

    return t;
  }

  private static class LongRange {

    long from = 1;
    long to = Long.MAX_VALUE;
    long length = Long.MAX_VALUE;
  }

}
