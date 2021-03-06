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
package com.opencore.gdpdu.index.util;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * This ErrorHandler just rethrows all Exceptions that occur during parsing.
 * We rely on the document being correct/well formed for our own parsing later.
 */
public class ThrowingErrorHandler implements ErrorHandler {

  @Override
  public void warning(SAXParseException exception) throws SAXException {
    throw exception;
  }

  @Override
  public void error(SAXParseException exception) throws SAXException {
    throw exception;
  }

  @Override
  public void fatalError(SAXParseException exception) throws SAXException {
    throw exception;
  }

}
