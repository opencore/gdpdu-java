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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;


import static java.util.Objects.requireNonNull;

public class ForeignKey {

  private List<String> names = new ArrayList<>();
  private String references;
  private Map<String, String> aliases = new HashMap<>();

  public List<String> getNames() {
    return Collections.unmodifiableList(names);
  }

  public void setNames(List<String> names) {
    this.names = new ArrayList<>(requireNonNull(names));
  }

  public void addName(String name) {
    names.add(requireNonNull(name));
  }

  /**
   * Enthält Informationen über Verknüpfungen.
   * In diesem Falle die referenzierte Tabelle.
   */
  public String getReferences() {
    return references;
  }

  public void setReferences(String references) {
    this.references = references;
  }

  public Map<String, String> getAliases() {
    return Collections.unmodifiableMap(aliases);
  }

  public void setAliases(Map<String, String> aliases) {
    this.aliases = new HashMap<>(requireNonNull(aliases));
  }

  public void addAlias(String from, String to) {
    aliases.put(requireNonNull(from), requireNonNull(to));
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", ForeignKey.class.getSimpleName() + "[", "]")
      .add("names=" + names)
      .add("references='" + references + "'")
      .add("aliases=" + aliases)
      .toString();
  }

}
