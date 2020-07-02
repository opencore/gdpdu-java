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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import com.opencore.gdpdu.common.exceptions.ParsingException;
import com.opencore.gdpdu.index.models.DataType;

public class Deserializers {

  private static final Map<Class<?>, Deserializer<?>> DESERIALIZER_MAP = new HashMap<>();

  public static Object deserialize(String currentValue, Class<?> parameterType, DataType dataType, DeserializationContext context) throws ParsingException {
    if (parameterType.isEnum()) {
      @SuppressWarnings("unchecked")
      Class<Enum<?>> enumClass = (Class<Enum<?>>) parameterType;
      return parseEnum(enumClass, currentValue);
    }

    Deserializer<?> deserializer = DESERIALIZER_MAP.get(parameterType);
    if (deserializer == null) {
      throw new ParsingException("Unmapped type [" + parameterType + "]");
    }

    return deserializer.deserialize(currentValue, dataType, context);
  }

  private static Enum<?> parseEnum(Class<Enum<?>> enumClass, String value) {
    for (Enum<?> enumConstant : enumClass.getEnumConstants()) {
      if (enumConstant.name().equalsIgnoreCase(value)) {
        return enumConstant;
      }
    }
    return null;
  }


  static {
    DESERIALIZER_MAP.put(BigDecimal.class, new BigDecimalDeserializer());

    BooleanDeserializer booleanDeserializer = new BooleanDeserializer();
    DESERIALIZER_MAP.put(Boolean.class, booleanDeserializer);
    DESERIALIZER_MAP.put(boolean.class, booleanDeserializer);

    IntegerDeserializer integerDeserializer = new IntegerDeserializer();
    DESERIALIZER_MAP.put(Integer.class, integerDeserializer);
    DESERIALIZER_MAP.put(int.class, integerDeserializer);

    DESERIALIZER_MAP.put(LocalDate.class, new LocalDateDeserializer());
    DESERIALIZER_MAP.put(LocalDateTime.class, new LocalDateTimeDeserializer());

    LongDeserializer longDeserializer = new LongDeserializer();
    DESERIALIZER_MAP.put(Long.class, longDeserializer);
    DESERIALIZER_MAP.put(long.class, longDeserializer);

    DESERIALIZER_MAP.put(String.class, new StringDeserializer());
  }

}
