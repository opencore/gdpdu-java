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
package com.opencore.gdpdu.index.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;
import javax.validation.Valid;

public class VariableLength {

  private String columnDelimiter = ";";
  private String recordDelimiter = "\r\n";
  private String textEncapsulator = "\"";
  @Valid private List<VariableColumn> variablePrimaryKeys = new ArrayList<>();
  @Valid private List<VariableColumn> variableColumns = new ArrayList<>();
  @Valid private List<ForeignKey> foreignKeys = new ArrayList<>();

  public String getColumnDelimiter() {
    return columnDelimiter;
  }

  public void setColumnDelimiter(String columnDelimiter) {
    this.columnDelimiter = columnDelimiter;
  }

  public String getRecordDelimiter() {
    return recordDelimiter;
  }

  public void setRecordDelimiter(String recordDelimiter) {
    this.recordDelimiter = recordDelimiter;
  }

  /**
   * Bei VariableLength-Dateien kann man Textfelder durch ein "Encapsulator"-Zeichen einschließen, z.B. für den Fall, dass der
   * Feldtrenner in den Daten vorkommt.
   * <p/>
   * Default: "
   */
  public String getTextEncapsulator() {
    return textEncapsulator;
  }

  public void setTextEncapsulator(String textEncapsulator) {
    this.textEncapsulator = textEncapsulator;
  }

  public List<VariableColumn> getVariablePrimaryKeys() {
    return Collections.unmodifiableList(variablePrimaryKeys);
  }

  public void setVariablePrimaryKeys(List<VariableColumn> variablePrimaryKeys) {
    this.variablePrimaryKeys = variablePrimaryKeys;
  }

  public List<VariableColumn> getVariableColumns() {
    return Collections.unmodifiableList(variableColumns);
  }

  public void setVariableColumns(List<VariableColumn> variableColumns) {
    this.variableColumns = variableColumns;
  }

  public List<ForeignKey> getForeignKeys() {
    return Collections.unmodifiableList(foreignKeys);
  }

  public void setForeignKeys(List<ForeignKey> foreignKeys) {
    this.foreignKeys = foreignKeys;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", VariableLength.class.getSimpleName() + "[", "]")
      .add("columnDelimiter='" + columnDelimiter + "'")
      .add("recordDelimited='" + recordDelimiter + "'")
      .add("textEncapsulator='" + textEncapsulator + "'")
      .add("variablePrimaryKeys=" + variablePrimaryKeys)
      .add("variableColumn=" + variableColumns)
      .add("foreignKeys=" + foreignKeys)
      .toString();
  }

}
