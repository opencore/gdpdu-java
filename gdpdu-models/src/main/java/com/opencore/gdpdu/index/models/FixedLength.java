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


import static java.util.Objects.requireNonNull;

public class FixedLength {

  private Long length;
  private String recordDelimiter = "\r\n";
  @Valid private List<FixedColumn> fixedPrimaryKeys = new ArrayList<>();
  @Valid private List<FixedColumn> fixedColumns = new ArrayList<>();
  @Valid private List<ForeignKey> foreignKeys = new ArrayList<>();

  public Long getLength() {
    return length;
  }

  public void setLength(Long length) {
    this.length = length;
  }

  public String getRecordDelimiter() {
    return recordDelimiter;
  }

  public void setRecordDelimiter(String recordDelimiter) {
    this.recordDelimiter = recordDelimiter;
  }

  public List<FixedColumn> getFixedPrimaryKeys() {
    return Collections.unmodifiableList(fixedPrimaryKeys);
  }

  public void setFixedPrimaryKeys(List<FixedColumn> fixedPrimaryKeys) {
    this.fixedPrimaryKeys = new ArrayList<>(requireNonNull(fixedPrimaryKeys));
  }

  public void addPrimaryKey(FixedColumn primaryKey) {
    fixedPrimaryKeys.add(primaryKey);
  }

  public List<FixedColumn> getFixedColumns() {
    return Collections.unmodifiableList(fixedColumns);
  }

  public void setFixedColumns(List<FixedColumn> fixedColumns) {
    this.fixedColumns = new ArrayList<>(requireNonNull(fixedColumns));
  }

  public void addFixedColumn(FixedColumn fixedColumn) {
    fixedColumns.add(requireNonNull(fixedColumn));
  }

  public List<ForeignKey> getForeignKeys() {
    return Collections.unmodifiableList(foreignKeys);
  }

  public void setForeignKeys(List<ForeignKey> foreignKeys) {
    this.foreignKeys = new ArrayList<>(requireNonNull(foreignKeys));
  }

  public void addForeignKey(ForeignKey foreignKey) {
    foreignKeys.add(foreignKey);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", FixedLength.class.getSimpleName() + "[", "]")
      .add("length=" + length)
      .add("recordDelimiter='" + recordDelimiter + "'")
      .add("fixedPrimaryKeys=" + fixedPrimaryKeys)
      .add("fixedColumns=" + fixedColumns)
      .add("foreignKeys=" + foreignKeys)
      .toString();
  }

}
