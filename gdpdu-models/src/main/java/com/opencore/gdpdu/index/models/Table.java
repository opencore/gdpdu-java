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

import java.util.StringJoiner;

public class Table {

  private String name;
  private String url;
  private String description;
  private Validity validity;
  private Encoding encoding = Encoding.ANSI;
  private String decimalSymbol = ",";
  private String digitGroupingSymbol = ".";
  private long skipNumBytes;
  private Range range;
  private String epoch = "30";
  private VariableLength variableLength;
  private FixedLength fixedLength;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Validity getValidity() {
    return validity;
  }

  public void setValidity(Validity validity) {
    this.validity = validity;
  }

  public Encoding getEncoding() {
    return encoding;
  }

  public void setEncoding(Encoding encoding) {
    this.encoding = encoding;
  }

  public String getDecimalSymbol() {
    return decimalSymbol;
  }

  public void setDecimalSymbol(String decimalSymbol) {
    this.decimalSymbol = decimalSymbol;
  }

  public String getDigitGroupingSymbol() {
    return digitGroupingSymbol;
  }

  public void setDigitGroupingSymbol(String digitGroupingSymbol) {
    this.digitGroupingSymbol = digitGroupingSymbol;
  }

  public long getSkipNumBytes() {
    return skipNumBytes;
  }

  public void setSkipNumBytes(long skipNumBytes) {
    this.skipNumBytes = skipNumBytes;
  }

  public Range getRange() {
    return range;
  }

  public void setRange(Range range) {
    this.range = range;
  }

  public String getEpoch() {
    return epoch;
  }

  public void setEpoch(String epoch) {
    this.epoch = epoch;
  }

  public VariableLength getVariableLength() {
    return variableLength;
  }

  public void setVariableLength(VariableLength variableLength) {
    this.variableLength = variableLength;
  }

  public FixedLength getFixedLength() {
    return fixedLength;
  }

  public void setFixedLength(FixedLength fixedLength) {
    this.fixedLength = fixedLength;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", Table.class.getSimpleName() + "[", "]")
      .add("name='" + name + "'")
      .add("url='" + url + "'")
      .add("description='" + description + "'")
      .add("validity=" + validity)
      .add("encoding=" + encoding)
      .add("decimalSymbol='" + decimalSymbol + "'")
      .add("digitGroupingSymbol='" + digitGroupingSymbol + "'")
      .add("skipNumBytes=" + skipNumBytes)
      .add("range=" + range)
      .add("epoch=" + epoch)
      .add("variableLength=" + variableLength)
      .add("fixedLength=" + fixedLength)
      .toString();
  }

}
