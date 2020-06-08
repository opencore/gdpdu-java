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

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class Media {

  private String name;

  private List<String> preCommands = new ArrayList<>();

  private List<Table> tables = new ArrayList<>();

  private List<String> postCommands = new ArrayList<>();

  private String acceptNotables;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<String> getPreCommands() {
    return preCommands;
  }

  public void setPreCommands(List<String> preCommands) {
    this.preCommands = preCommands;
  }

  public List<Table> getTables() {
    return tables;
  }

  public void setTables(List<Table> tables) {
    this.tables = tables;
  }

  public List<String> getPostCommands() {
    return postCommands;
  }

  public void setPostCommands(List<String> postCommands) {
    this.postCommands = postCommands;
  }

  public String getAcceptNotables() {
    return acceptNotables;
  }

  public void setAcceptNotables(String acceptNotables) {
    this.acceptNotables = acceptNotables;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", Media.class.getSimpleName() + "[", "]")
      .add("name='" + name + "'")
      .add("preCommands=" + preCommands)
      .add("tables=" + tables)
      .add("postCommands=" + postCommands)
      .add("acceptNotables=" + acceptNotables)
      .toString();
  }

}
