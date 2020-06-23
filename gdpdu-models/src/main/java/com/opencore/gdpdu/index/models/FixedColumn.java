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
import java.util.List;
import java.util.StringJoiner;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.PositiveOrZero;

import com.opencore.gdpdu.index.validation.ValidFixedColumn;

/**
 * Definiert eine Spalte (=Column) in einer Datei vom Typ FixedLength.
 */
@ValidFixedColumn
public class FixedColumn {

  @NotBlank private String name;
  private String description;
  private DataType dataType;
  private AccuracyType accuracyType;
  @PositiveOrZero private long accuracy;  // Numeric
  private String format = "DD.MM.YYYY";  // Date
  @Valid private List<Mapping> mappings = new ArrayList<>();
  @Valid private FixedRange fixedRange;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public DataType getDataType() {
    return dataType;
  }

  public void setDataType(DataType dataType) {
    this.dataType = dataType;
  }

  public AccuracyType getAccuracyType() {
    return accuracyType;
  }

  public void setAccuracyType(AccuracyType accuracyType) {
    this.accuracyType = accuracyType;
  }

  /**
   * Anzahl der Nachkommastellen.
   * Vorsicht bei Daten, deren Genauigkeit grösser ist, als in Accuracy angegeben.
   */
  public long getAccuracy() {
    return accuracy;
  }

  public void setAccuracy(long accuracy) {
    this.accuracy = accuracy;
  }

  public String getFormat() {
    return format;
  }

  public void setFormat(String format) {
    this.format = format;
  }

  public List<Mapping> getMappings() {
    return mappings;
  }

  public void setMappings(List<Mapping> mappings) {
    this.mappings = mappings;
  }

  public FixedRange getFixedRange() {
    return fixedRange;
  }

  public void setFixedRange(FixedRange fixedRange) {
    this.fixedRange = fixedRange;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", FixedColumn.class.getSimpleName() + "[", "]")
      .add("name='" + name + "'")
      .add("description='" + description + "'")
      .add("dataType=" + dataType)
      .add("accuracyType=" + accuracyType)
      .add("accuracy=" + accuracy)
      .add("format='" + format + "'")
      .add("mappings=" + mappings)
      .add("fixedRange=" + fixedRange)
      .toString();
  }

}
