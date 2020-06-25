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
package com.opencore.gdpdu.index.util;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ElementWrapperTest {

  private DocumentBuilder db;

  @BeforeEach
  void setUp() throws ParserConfigurationException {
    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    db = dbFactory.newDocumentBuilder();
  }

  @Test
  void testProcessOptionalTextElement() throws Exception {
    Document doc = db.parse("src/test/resources/simple.xml");
    Element documentElement = doc.getDocumentElement();
    ElementWrapper wrapper = new ElementWrapper(documentElement);

    // This one exists
    assertThrows(RuntimeException.class, () -> wrapper.processOptionalTextElement("child", s -> {
      assertEquals("TEST", s);
      throw new RuntimeException("should reach here");
    }));

    assertDoesNotThrow(() -> wrapper.processOptionalTextElement("doesntexist", s -> {
      throw new RuntimeException("foo");
    }));

  }

  @Test
  void name() {
  }
}
