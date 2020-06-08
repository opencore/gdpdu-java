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
package com.opencore.gdpdu.models;

import java.util.StringJoiner;
import javax.validation.constraints.NotBlank;

/**
 * Enthält Angaben zum Datenlieferanten.
 */
public class DataSupplier {

  @NotBlank
  private String name;

  @NotBlank
  private String location;

  @NotBlank
  private String comment;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  /**
   * Beschreibt den Standort des Datenlieferanten.
   */
  public String getLocation() {
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  /**
   * Kommentarfeld für zusätzliche Informationen zum Datenlieferanten.
   */
  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", DataSupplier.class.getSimpleName() + "[", "]")
      .add("name='" + name + "'")
      .add("location='" + location + "'")
      .add("comment='" + comment + "'")
      .toString();
  }

}
