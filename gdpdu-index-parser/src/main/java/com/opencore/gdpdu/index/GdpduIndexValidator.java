/*
 * Licensed to OpenCore GmbH & Co. KG under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information regarding copyright ownership.
 * OpenCore GmbH & Co. KG licenses this file to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */
package com.opencore.gdpdu.index;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;

import com.opencore.gdpdu.common.exceptions.ParsingException;
import com.opencore.gdpdu.common.util.ClassRegistry;
import com.opencore.gdpdu.common.util.ColumnInfo;
import com.opencore.gdpdu.index.annotations.Column;
import com.opencore.gdpdu.index.models.DataSet;
import com.opencore.gdpdu.index.models.Table;
import com.opencore.gdpdu.index.models.VariableColumn;

public final class GdpduIndexValidator {

  private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

  private GdpduIndexValidator() {
  }

  /**
   * This runs validations on a {@link DataSet}.
   * When using {@link GdpduIndexParser} it should already be syntactically correct due to the DTD but this validates a few semantic things as well.
   */
  public static Set<ConstraintViolation<DataSet>> validateDataSet(DataSet dataSet) {
    return VALIDATOR.validate(dataSet);
  }

  /**
   * This method takes a {@link Table} object from an index.xml file as well as an arbitrary class.
   * It then checks whether the table has all the columns that are specified on the class with {@link Column} annotations and the correct data types.
   * <p/>
   * It currently returns a list of Strings containing error messages.
   * TODO: This is not optimal and could be improved later to e.g. return structured violation objects
   */
  public static <T> List<String> validateTableAgainstClass(Class<T> clazz, Table table) throws ParsingException {
    Objects.requireNonNull(table, "'table' can't be null");
    Objects.requireNonNull(clazz, "'clazz' can't be null");

    Map<String, ColumnInfo> infoMap = ClassRegistry.getClassInformation(clazz);
    Objects.requireNonNull(infoMap);

    List<String> errors = new ArrayList<>();
    // TODO: Validate the data types from the annotation against the index xml data
    if (table.getVariableLength() != null) {
      // First we need to build a Map of all column names to their column definition (this includes the primary keys)
      Map<String, VariableColumn> columnMap = Stream.concat(
        table.getVariableLength().getVariablePrimaryKeys().stream(),
        table.getVariableLength().getVariableColumns().stream()
      ).collect(Collectors.toMap(VariableColumn::getName, v -> v));

      for (Map.Entry<String, ColumnInfo> entry : infoMap.entrySet()) {
        Column annotation = entry.getValue().annotation;
        if (!columnMap.containsKey(annotation.value())) {
          errors.add("Class [" + clazz.getName() + "] specifies column [" + annotation.value() + "] for table [" + table.getName() + "] but index.xml does not have a correspending field");
        }
        if (annotation.type() != columnMap.get(annotation.value()).getDataType()) {
          errors.add("Class [" + clazz.getName() + "] specifies column [" + annotation.value() + "] with data type [" + annotation.type() + "] for table [" + table.getName() + "] but index.xml specifies type [" + columnMap.get(annotation.value()).getDataType() + "]");
        }
      }
    } else if (table.getFixedLength() != null) {
      // TODO
      throw new UnsupportedOperationException("FixedLength not supported yet");
    } else {
      throw new IllegalArgumentException("Neither VariableLength nor FixedLength found, aborting");
    }
    return errors;
  }

  /**
   * This validates a class against an index.xml file to make sure that each column in the index.xml has a field in the class.
   * The reverse can be checked using {@link #validateTableAgainstClass(Class, Table)}.
   */
  public static <T> void validateClassAgainstTable(Class<T> clazz, Table table) throws ParsingException {
    Objects.requireNonNull(table, "'table' can't be null");
    Objects.requireNonNull(clazz, "'clazz' can't be null");

    Map<String, ColumnInfo> infoMap = ClassRegistry.getClassInformation(clazz);
    Objects.requireNonNull(infoMap);

    if (table.getVariableLength() != null) {
      List<VariableColumn> columns = new ArrayList<>();
      columns.addAll(table.getVariableLength().getVariablePrimaryKeys());
      columns.addAll(table.getVariableLength().getVariableColumns());
      for (VariableColumn variablePrimaryKey : columns) {
        if (!infoMap.containsKey(variablePrimaryKey.getName())) {
          throw new ParsingException("index.xml specifies column [" + variablePrimaryKey.getName() + "] for table [" + table.getName() + "] but class [" + clazz.getName() + "] does not have a correspending field");
        }
      }
    } else if (table.getFixedLength() != null) {
      // TODO
      throw new UnsupportedOperationException("FixedLength not supported yet");
    } else {
      throw new ParsingException("Neither VariableLength nor FixedLength found, aborting");
    }
  }

}
