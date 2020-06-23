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

import java.util.StringJoiner;
import javax.validation.constraints.NotBlank;

/**
 * Use Alias to reference columns with different names in a ForeignKey element.
 * These Alias elements are optional.
 * <p/>
 * The following rules apply to the Alias element:
 * <ul>
 *   <li>One Alias can be used per ForeignKey.</li>
 *   <li>Alias elements can appear in any order.</li>
 * </ul>
 * Example:
 *
 * Table Orders has a primary key OrderId
 * Table Accounts has a foreign key Order.
 * <p/>
 * You can use the Alias element to specify Order references OrderId.
 *
 * <pre>
 * {@code
 * <ForeignKey>
 *    <Name>Order</Name>
 *    <Name>Customer</Name>
 *    <References>Orders</References>
 *    <Alias>
 *      <From>Order</From>
 *      <To>OrderId</To>
 *    </Alias>
 *  </ForeignKey>
 * }
 * </pre>
 */
public class Alias {

  @NotBlank private String from;
  @NotBlank private String to;

  public String getFrom() {
    return from;
  }

  public void setFrom(String from) {
    this.from = from;
  }

  public String getTo() {
    return to;
  }

  public void setTo(String to) {
    this.to = to;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", Alias.class.getSimpleName() + "[", "]")
      .add("from='" + from + "'")
      .add("to='" + to + "'")
      .toString();
  }

}
