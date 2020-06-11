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
 * Use Extension when you want to add application specific functionality to the existing standard.
 * <p/>
 * <ul>
 *   <li>Name - the extension name or identifier.</li>
 *   <li>URL  - the supplementary .xml file that corresponds to the extension.</li>
 * </ul>
 * An application that extends the standard should scan the Dataset element for the presence of zero or more Extension elements.
 * The application can use the Name element to identify the extension.
 * <p/>
 * When choosing a name for your extension, do not choose a common name.
 * This will reduce undefined results for name conflicts.
 * <p/>
 * It is possible that future extensions will be ratified as mandatory to meet GDPdU guidelines.
 */
public class Extension {

  @NotBlank
  private String name;

  @NotBlank
  private String url;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", Extension.class.getSimpleName() + "[", "]")
      .add("name='" + name + "'")
      .add("url='" + url + "'")
      .toString();
  }

}
