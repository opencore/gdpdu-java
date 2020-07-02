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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import com.opencore.gdpdu.common.exceptions.ParsingException;
import com.opencore.gdpdu.index.models.DataType;

// TODO: Format support
public class LocalDateDeserializer extends Deserializer<LocalDate> {

  @Override
  protected LocalDate deserializeInternal(String value, DataType dataType, DeserializationContext context) throws ParsingException {
    try {
      return LocalDate.parse(value, DateTimeFormatter.ISO_DATE);
    } catch (DateTimeParseException e) {
      throw new ParsingException(e);
    }
  }

}
