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

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.opencore.gdpdu.index.GdpduIndexParser;
import com.opencore.gdpdu.index.annotations.Column;
import com.opencore.gdpdu.index.models.DataSet;
import com.opencore.gdpdu.index.models.Media;
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
 * This is not thread-safe.
 */
public class GdpduDataParser {

  private static final Logger LOG = LoggerFactory.getLogger(GdpduDataParser.class);

  /**
   * This maps (for each model class) from field names to the setter method for each field that is annotated with the "Column" annotation.
   */
  private final Map<Class<?>, Map<String, Method>> writeMethods = new HashMap<>();

  /**
   * This method returns all fields (private as well as public) for a Class including its superclasses.
   */
  private static Map<String, Field> getAllFields(Class<?> type) {
    Objects.requireNonNull(type, "'type' can't be null");

    Map<String, Field> fieldMap = new HashMap<>();
    for (Class<?> c = type; c != null; c = c.getSuperclass()) {
      for (Field declaredField : c.getDeclaredFields()) {
        fieldMap.put(declaredField.getName(), declaredField);
      }
    }
    return fieldMap;
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

  // TODO: All of these should be made pluggable
  private static <T> void deserializeValue(Table table, T t, VariableColumn currentColumn, String currentValue, Method method) throws ParsingException {
    try {
      if (method != null) {
        Class<?> parameterType = method.getParameterTypes()[0]; // In initClassMap we checked that every write method has exactly one parameter
        if (parameterType == String.class) {
          method.invoke(t, currentValue);
        } else if (parameterType == LocalDateTime.class) {
          LocalDateTime localDateTime;
          try {
            localDateTime = LocalDateTime.parse(currentValue, DateTimeFormatter.ISO_DATE_TIME);
          } catch (DateTimeParseException e) {
            throw new ParsingException(e);
          }
          method.invoke(t, localDateTime);
        } else if (parameterType == int.class || parameterType == Integer.class) {
          method.invoke(t, Integer.parseInt(currentValue.replace(table.getDigitGroupingSymbol(), "")));
        } else if (parameterType == LocalDate.class) {
          LocalDate localDate;
          try {
            localDate = LocalDate.parse(currentValue, DateTimeFormatter.ISO_DATE);
          } catch (DateTimeParseException e) {
            throw new ParsingException(e);
          }
          method.invoke(t, localDate);
        } else if (parameterType == BigDecimal.class) {
          String replaced = currentValue.replace(table.getDigitGroupingSymbol(), "");
          replaced = replaced.replace(table.getDecimalSymbol(), ".");
          method.invoke(t, new BigDecimal(replaced));
        } else if (parameterType == long.class || parameterType == Long.class) {
          method.invoke(t, Long.parseLong(currentValue.replace(table.getDigitGroupingSymbol(), "")));
        } else if (parameterType.isEnum()) {
          @SuppressWarnings("unchecked")
          Class<Enum<?>> enumClass = (Class<Enum<?>>) parameterType;
          method.invoke(t, parseEnum(enumClass, currentValue));
        } else if (parameterType == boolean.class || parameterType == Boolean.class) {
          method.invoke(t, currentValue.equals("1"));
        } else {
          LOG.warn("Unmapped type [{}] for column [{}]", parameterType, currentColumn.getName());
          throw new ParsingException("Unmapped type [" + parameterType + "] for column [" + currentColumn.getName() + "]");
        }
      }
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw new ParsingException(e);
    }
  }

  private static Enum<?> parseEnum(Class<Enum<?>> enumClass, String value) {
    for (Enum<?> enumConstant : enumClass.getEnumConstants()) {
      if (enumConstant.name().equalsIgnoreCase(value)) {
        return enumConstant;
      }
    }
    return null;
  }

  public <T> List<T> parseTable(String indexXml, String tableName, Class<T> clazz) throws ParsingException {
    return parseTable(new File(Objects.requireNonNull(indexXml)), tableName, clazz);
  }

  /**
   * Parses a single {@link Table} from a specific {@code index.xml} file into a List of domain objects.
   */
  public <T> List<T> parseTable(File indexXmlFile, String tableName, Class<T> clazz) throws ParsingException {
    Objects.requireNonNull(indexXmlFile, "'indexXmlFile' can't be null");
    Objects.requireNonNull(tableName, "'tableName' can't be null");
    Objects.requireNonNull(clazz, "'clazz' can't be null");

    LOG.trace("Beginning to parse [{}]", indexXmlFile);
    DataSet dataSet;
    try {
      dataSet = GdpduIndexParser.parseXmlFile(indexXmlFile, GdpduIndexParser.ParseMode.LENIENT);
    } catch (IOException e) {
      throw new ParsingException(e);
    }
    LOG.debug("Successfully parsed [{}]", indexXmlFile);

    //TODO: Validate that there's a setter/write method for each column in the Table object

    // Try to find the table in our index.xml file
    Table table = null;
    Media media = null;
    for (Media tmpMedia : dataSet.getMedia()) {
      for (Table tmpTable : tmpMedia.getTables()) {
        if (tableName.equals(tmpTable.getName()) || tableName.equals(tmpTable.getUrl())) {
          table = tmpTable;
          media = tmpMedia;
          break;
        }
      }
    }

    if (table == null) {
      LOG.error("Table [{}] could not be found, aborting", tableName);
      throw new ParsingException("Table could not be found, aborting");
    }

    initClassMap(clazz);

    // All files need to be relative to the index.xml file
    File directory = indexXmlFile.getAbsoluteFile().getParentFile();

    if (table.getVariableLength() != null) {
      LOG.trace("[{}] is VariableLength table, parsing now", tableName);
      return parseVariableLengthTable(directory, media, table, clazz);
    } else if (table.getFixedLength() != null) {
      LOG.trace("[{}] is FixedLength table, parsing now", tableName);
      //TODO Support fixedLength
      return new ArrayList<>();
    } else {
      LOG.error("Neither VariableLength nor FixedLength found, aborting");
      throw new ParsingException("Neither VariableLength nor FixedLength found, aborting");
    }
  }

  private <T> void initClassMap(Class<T> clazz) throws ParsingException {
    Map<String, Field> fieldMap = getAllFields(clazz);

    if (!writeMethods.containsKey(clazz)) {
      BeanInfo info;
      try {
        info = Introspector.getBeanInfo(clazz);
      } catch (IntrospectionException e) {
        throw new ParsingException(e);
      }

      Map<String, Method> newWriteMethods = new HashMap<>();
      for (PropertyDescriptor propertyDescriptor : info.getPropertyDescriptors()) {
        Field field = fieldMap.get(propertyDescriptor.getName());
        if (field == null) {
          continue;
        }

        // Get every field that's annotated by the Column annotation
        Column annotation = field.getAnnotation(Column.class);
        if (annotation == null) {
          continue;
        }

        Method writeMethod = propertyDescriptor.getWriteMethod();
        if (writeMethod.getParameterCount() != 1) {
          throw new ParsingException("We only support setters with exactly one parameter");
        }

        newWriteMethods.put(annotation.value(), propertyDescriptor.getWriteMethod());
      }
      writeMethods.put(clazz, newWriteMethods);
    }
  }

  /**
   * This method parses a variable length (i.e. "CSV"/"TSV", ...) table into domain objects.
   * It uses two phases: Parse into generic records and then map to domain objects.
   */
  private <T> List<T> parseVariableLengthTable(File directory, Media media, Table table, Class<T> clazz) throws ParsingException {
    Objects.requireNonNull(directory, "'directory' can't be null");
    Objects.requireNonNull(media, "'media' can't be null");
    Objects.requireNonNull(table, "'table' can't be null");
    Objects.requireNonNull(clazz, "'clazz' can't be null");

    // Construct the proper CSVFormat
    // NOTE: Apache Commons CSV does not support specifying the line ending
    VariableLength variableLength = table.getVariableLength();
    CSVFormat format = CSVFormat
      .newFormat(variableLength.getColumnDelimiter().charAt(0))
      .withQuote(variableLength.getTextEncapsulator().charAt(0));
    LOG.debug("Parsing with settings: [{}]", format);

    File csvFile = new File(directory, table.getUrl());
    if (!csvFile.canRead() || csvFile.isDirectory()) {
      LOG.error("Referenced file [{}] can't be read, aborting", csvFile);
      throw new ParsingException("Referenced file [" + csvFile + "] can't be read, aborting");
    }

    // We first parse the file into a generic "record" that's based only on Strings
    CSVParser parser;
    try {
      // TODO: Don't hardcode the charset, take it from the index.xml file
      parser = CSVParser.parse(csvFile, Charset.forName("Cp1252"), format);
    } catch (IOException e) {
      throw new ParsingException(e);
    }

    // TODO: index.xml also specifies "length" and "to" in the Range object, those need to be supported as well
    long from = 1;
    if (table.getRange() != null && table.getRange().getFrom() != null) {
      from = Long.parseLong(table.getRange().getFrom());
    }

    // Convert the "generic" record into the specific type
    List<T> results = new ArrayList<>();
    // GDPdU seems to be "1" based. We increment the index at the beginning of the loop so we start at 0 here
    int index = 0;

    try {
      for (CSVRecord record : parser) {
        index++;
        if (index < from) {
          continue;
        }

        // TODO: Make this configurable
        List<ParsingException> exceptions = new ArrayList<>();
        try {
          results.add(parseRecord(record, table, clazz));
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

  // TODO: This is specific to variable length stuff now, need to see if this can be made generic enough to support fixed length as well
  private <T> T parseRecord(CSVRecord record, Table table, Class<T> clazz) throws ParsingException {
    Objects.requireNonNull(record, "'record' can't be null");
    Objects.requireNonNull(table, "'table' can't be null");
    Objects.requireNonNull(clazz, "'clazz' can't be null");

    Map<String, Method> writeMethods = this.writeMethods.get(clazz);

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
      Method method = writeMethods.get(currentColumn.getName());
      deserializeValue(table, t, currentColumn, currentValue, method);
    }

    return t;
  }

}
