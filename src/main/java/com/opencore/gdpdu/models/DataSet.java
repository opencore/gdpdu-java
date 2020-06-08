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
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;

/**
 * Das Element DataSet ist das oberste Element (document-Element) in der Hierarchie des XML-Dokumentes.
 * Das Element DataSet ist Träger der Version, Datenherkunft, Vorlauf- und Nachlauf-Prozesse und der Medien, die die Tabellen enthalten.
 */
public class DataSet {

  private List<Extension> extensions = new ArrayList<>();
  @NotBlank
  private String version;
  private DataSupplier dataSupplier;
  private List<String> preCommands = new ArrayList<>();
  @NotEmpty
  private List<Media> media = new ArrayList<>();
  private List<String> postCommands = new ArrayList<>();

  public List<Extension> getExtensions() {
    return extensions;
  }

  public void setExtensions(List<Extension> extensions) {
    this.extensions = extensions;
  }

  /**
   * Enthält die Versionsnummer der Datenträgerbereitstellung.
   * Dieses Element hat keine technische Auswirkung, sondern dient zur Beschreibung.
   */
  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  /**
   * Enthält Angaben zum Datenlieferanten.
   */
  public DataSupplier getDataSupplier() {
    return dataSupplier;
  }

  public void setDataSupplier(DataSupplier dataSupplier) {
    this.dataSupplier = dataSupplier;
  }

  /**
   * Command definiert ein Betriebssystemkommando.
   *
   * Diese Commands werden vor dem gesamten Importprozess ausgeführt.
   */
  public List<String> getPreCommands() {
    return preCommands;
  }

  public void setPreCommands(List<String> preCommands) {
    this.preCommands = preCommands;
  }

  /**
   * Definiert den Inhalt der Datenträger.
   */
  public List<Media> getMedia() {
    return media;
  }

  public void setMedia(List<Media> media) {
    this.media = media;
  }

  /**
   * Command definiert ein Betriebssystemkommando.
   *
   * Diese Commands werden nach dem gesamten Importprozess ausgeführt.
   */
  public List<String> getPostCommands() {
    return postCommands;
  }

  public void setPostCommands(List<String> postCommands) {
    this.postCommands = postCommands;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", DataSet.class.getSimpleName() + "[", "]")
      .add("extensions=" + extensions)
      .add("version='" + version + "'")
      .add("dataSupplier=" + dataSupplier)
      .add("preCommands=" + preCommands)
      .add("media=" + media)
      .add("postCommands=" + postCommands)
      .toString();
  }

}
