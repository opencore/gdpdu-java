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
package com.opencore.gdpdu.data.deserializers;

import java.nio.charset.Charset;

public class DeserializationContext {

  private boolean trim;
  private String digitGroupingSymbol;
  private String decimalSymbol;

  private String recordDelimiter;
  private String columnDelimiter;
  private String textEncapsulator;

  private long skipNumBytes;

  private Charset charset;

  public boolean isTrim() {
    return trim;
  }

  public void setTrim(boolean trim) {
    this.trim = trim;
  }

  public String getDigitGroupingSymbol() {
    return digitGroupingSymbol;
  }

  public void setDigitGroupingSymbol(String digitGroupingSymbol) {
    this.digitGroupingSymbol = digitGroupingSymbol;
  }

  public String getDecimalSymbol() {
    return decimalSymbol;
  }

  public void setDecimalSymbol(String decimalSymbol) {
    this.decimalSymbol = decimalSymbol;
  }

  public String getRecordDelimiter() {
    return recordDelimiter;
  }

  public void setRecordDelimiter(String recordDelimiter) {
    this.recordDelimiter = recordDelimiter;
  }

  public String getColumnDelimiter() {
    return columnDelimiter;
  }

  public void setColumnDelimiter(String columnDelimiter) {
    this.columnDelimiter = columnDelimiter;
  }

  public String getTextEncapsulator() {
    return textEncapsulator;
  }

  public void setTextEncapsulator(String textEncapsulator) {
    this.textEncapsulator = textEncapsulator;
  }

  public long getSkipNumBytes() {
    return skipNumBytes;
  }

  public void setSkipNumBytes(long skipNumBytes) {
    if (skipNumBytes < 0) {
      throw new IllegalArgumentException("'skipNumBytes' must be 0 or larger");
    }
    this.skipNumBytes = skipNumBytes;
  }

  public Charset getCharset() {
    return charset;
  }

  public void setCharset(Charset charset) {
    this.charset = charset;
  }
}
