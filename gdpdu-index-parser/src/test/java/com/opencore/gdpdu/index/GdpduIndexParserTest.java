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

import java.io.IOException;
import java.io.InputStream;

import com.opencore.gdpdu.index.models.DataSet;
import com.opencore.gdpdu.index.models.DataSupplier;
import com.opencore.gdpdu.index.models.Encoding;
import com.opencore.gdpdu.index.models.Extension;
import com.opencore.gdpdu.index.models.Media;
import com.opencore.gdpdu.index.models.Table;
import com.opencore.gdpdu.index.models.VariableLength;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GdpduIndexParserTest {

  @Test
  void testNonexistantFile() throws IOException {
    assertThrows(IllegalArgumentException.class, () -> GdpduIndexParser.parseXmlFile("doesnotexist.xml"));

    InputStream is = null;
    assertThrows(NullPointerException.class, () -> GdpduIndexParser.parseXmlFile(is));
  }

  @Test
  void testInvalidFile() {
    assertThrows(IOException.class, () -> GdpduIndexParser.parseXmlFile("src/test/resources/malformed-index.xml"));
  }

  @Test
  void testComplexParsing() throws IOException {
    DataSet dataSet = GdpduIndexParser.parseXmlFile("src/test/resources/complex-index.xml");

    assertEquals(1, dataSet.getExtensions().size());
    Extension extension = dataSet.getExtensions().get(0);
    assertEquals("Test", extension.getName());
    assertEquals("Test", extension.getUrl());

    assertEquals("1.0", dataSet.getVersion());

    DataSupplier supplier = dataSet.getDataSupplier();
    assertEquals("OpenCore GmbH & Co. KG", supplier.getName());
    assertEquals("Wedel, Deutschland", supplier.getLocation());
    assertEquals("Testdatei", supplier.getComment());

    assertEquals(1, dataSet.getPreCommands().size());
    assertEquals("echo Pre-Media", dataSet.getPreCommands().get(0));
    assertEquals(1, dataSet.getPostCommands().size());

    assertEquals(3, dataSet.getMedia().size());

    Media media = dataSet.getMedia().get(0);
    assertEquals("Media 1", media.getName());
    assertEquals("echo Pre-Tables", media.getPreCommands().get(0));
    assertEquals(1, media.getTables().size());

    Table table = media.getTables().get(0);
    assertEquals("data.csv", table.getUrl());
    assertEquals("Table 1", table.getName());
    assertEquals("Testdatei", table.getDescription());
    assertEquals("20.02.2020", table.getValidity().getRange().getFrom());
    assertEquals(Encoding.OEM, table.getEncoding());
    assertEquals(",", table.getDecimalSymbol());
    assertEquals("  ", table.getDigitGroupingSymbol());
    assertEquals(100, table.getSkipNumBytes());
    assertEquals("2", table.getRange().getFrom());
    assertEquals("3", table.getRange().getLength());
    assertEquals("75", table.getEpoch());
    assertNull(table.getFixedLength());

    VariableLength variableLength = table.getVariableLength();
    assertEquals("||", variableLength.getColumnDelimiter());
    assertEquals("\n", variableLength.getRecordDelimiter());
    assertEquals("'''", variableLength.getTextEncapsulator());
    assertEquals(1, variableLength.getVariablePrimaryKeys().size());
    assertEquals(7, variableLength.getVariableColumns().size());

  }
}
