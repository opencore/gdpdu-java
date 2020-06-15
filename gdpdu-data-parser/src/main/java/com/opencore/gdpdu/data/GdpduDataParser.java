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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.validation.Validation;
import javax.validation.Validator;

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

public class GdpduDataParser {

  private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

  public static void main(String[] args)
    throws IOException, InvocationTargetException, NoSuchMethodException, InstantiationException,
    IllegalAccessException, IntrospectionException {
    //parse("testdata/DSFinV_K000000021208120200522073205/index.xml", "cashpointclosing.csv", );
  }

  /**
   * Parses a single {@link Table} from a specific {@code index.xml} file into a List of domain objects.
   */
  public static <T> List<T> parse(String indexPath, String tableName, Class<T> clazz)
    throws NoSuchMethodException, IntrospectionException, IOException, InstantiationException, IllegalAccessException,
    InvocationTargetException {
    File file = new File(indexPath);

    DataSet dataSet = GdpduIndexParser.parseXmlFile(file, false);
    //TODO: Validate dataSet

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
      System.out.println("Table not found, aborting");
      return null;
    }

    // TODO:Check if VL and also support FL
    File directory = file.getAbsoluteFile().getParentFile();
    return parseVariableLength(directory, media, table, clazz);
  }

  private static <T> List<T> parseVariableLength(File directory, Media media, Table table, Class<T> modelClass)
    throws IOException, NoSuchMethodException, IllegalAccessException, InvocationTargetException,
    InstantiationException, IntrospectionException {
    // assert parameters !=null

    VariableLength variableLength = table.getVariableLength();
    CSVFormat format = CSVFormat
      .newFormat(variableLength.getColumnDelimiter().charAt(0))
      .withQuote(variableLength.getTextEncapsulator().charAt(0));

    File csvFile = new File(directory, table.getUrl());
    // TODO:Check canRead() and abort/report
    if (!csvFile.canRead() || csvFile.isDirectory()) {
      return null;
    }

    CSVParser parser = CSVParser.parse(csvFile, Charset.forName("Cp1252"), format);

    List<T> results = new ArrayList<>();
    int index =
      0; // Die GdPDU scheint 1 basiert zu sein! Wir erhöhen den Index aber als erstes in der Scheife, daher beginnen wir hier bei 0
    long from = 1;
    // TODO: to & length
    if (table.getRange() != null && table.getRange().getFrom() != null) {
      from = Long.parseLong(table.getRange().getFrom());
    }

    for (CSVRecord record : parser) {
      index++;
      if (index < from) {
        continue;
      }
      results.add(parse(record, table, media, modelClass));
      System.out.println(record);
    }
    return results;
  }

  public static Map<String, Field> getAllFields(Class<?> type) {
    Map<String, Field> fieldMap = new HashMap<>();
    for (Class<?> c = type; c != null; c = c.getSuperclass()) {
      for (Field declaredField : c.getDeclaredFields()) {
        fieldMap.put(declaredField.getName(), declaredField);
      }
    }
    return fieldMap;
  }

  private static <T> T parse(CSVRecord record, Table table, Media media, Class<T> modelClass)
    throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException,
    IntrospectionException {

    //TODO Both of these maps should be cached really

    if (modelClass == null) {
      System.out.println("Error");
      return null;
    }

    // This is a map of field names to the Field implementation
    Map<String, Field> fieldMap = getAllFields(modelClass);

    BeanInfo info = Introspector.getBeanInfo(modelClass);

    // This is a map from the column names as they appear in index.xml to the write Methods to set them
    Map<String, Method> writeMethods = new HashMap<>();
    for (PropertyDescriptor propertyDescriptor : info.getPropertyDescriptors()) {
      Field field = fieldMap.get(propertyDescriptor.getName());
      if (field == null) {
        continue;
      }

      Column annotation = field.getAnnotation(Column.class);
      if (annotation == null) {
        continue;
      }

      writeMethods.put(annotation.value(), propertyDescriptor.getWriteMethod());
    }

    List<VariableColumn> columns = new ArrayList<>();
    columns.addAll(table.getVariableLength().getVariablePrimaryKeys());
    columns.addAll(table.getVariableLength().getVariableColumns());

    T t = modelClass.getDeclaredConstructor().newInstance();

    if (columns.size() != record.size()) {
      System.out.println("Columns: " + columns.size() + ", but record has: " + record.size());
      // TODO Handle, must exit
    }

    for (int i = 0; i < columns.size(); i++) {
      VariableColumn currentColumn = columns.get(i);
      String currentValue = record.get(i);
      if (currentValue == null || currentValue.isBlank()) {
        System.out.println("Skipping empty value");
        continue;
      }

      Method method = writeMethods.get(currentColumn.getName());
      try {
        if (method != null) {
          Class<?> parameterType = method.getParameterTypes()[0];
          // TODO Handle more than one
          if (parameterType == String.class) {
            method.invoke(t, currentValue);
          } else if (parameterType == LocalDateTime.class) {
            LocalDateTime localDateTime = LocalDateTime.parse(currentValue, DateTimeFormatter.ISO_DATE_TIME);
            method.invoke(t, localDateTime);
          } else if (parameterType == int.class) {
            method.invoke(t, Integer.parseInt(currentValue.replace(table.getDigitGroupingSymbol(), "")));
          } else if (parameterType == LocalDate.class) {
            LocalDate localDate = LocalDate.parse(currentValue, DateTimeFormatter.ISO_DATE);
            method.invoke(t, localDate);
          } else if (parameterType == BigDecimal.class) {
            String replaced = currentValue.replace(table.getDigitGroupingSymbol(), "");
            replaced = replaced.replace(table.getDecimalSymbol(), ".");
            method.invoke(t, new BigDecimal(replaced));
          } else {
            System.out.println("Unmapped type: " + parameterType);
          }
        }
      } catch (Exception e) {
        System.out.println("Error parsing " + currentValue + " for coulmn " + currentColumn);
        e.printStackTrace();
        System.exit(1);
      }
    }

    return t;
  }

}
