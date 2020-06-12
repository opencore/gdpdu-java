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

import java.util.function.Consumer;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class ElementWrapper {

  private final Element element;
  private Element currentChild;
  private Element nextChild;

  public ElementWrapper(Element element) {
    this.element = element;
    currentChild = findNextElement(element.getFirstChild());
    nextChild = currentChild;
  }

  public void processTextElement(String tagName, Consumer<String> consumer) {
    if (hasNext() && next().getTagName().equals(tagName)) {
      consumer.accept(current().getTextContent());
    } else {
      throw new IllegalStateException("Expected: " + tagName + ", but received: " + current().getTagName());
    }
  }

  public void processOptionalTextElement(String tagName, Consumer<String> consumer) {
    if (hasNext() && peek().getTagName().equals(tagName)) {
      consumer.accept(next().getTextContent());
    }
  }

  public void processElement(String tagName, Consumer<ElementWrapper> consumer) {
    if (hasNext() && peek().getTagName().equals(tagName)) {
      consumer.accept(new ElementWrapper(next()));
    } else {
      throw new IllegalStateException("Expected to find [" + tagName + "]");
    }
  }

  public void processOptionalElement(String tagName, Consumer<ElementWrapper> consumer) {
    if (hasNext() && peek().getTagName().equals(tagName)) {
      consumer.accept(new ElementWrapper(next()));
    }
  }

  public void processOptionalElements(String tagName, Consumer<ElementWrapper> consumer) {
    while (hasNext() && peek().getTagName().equals(tagName)) {
      consumer.accept(new ElementWrapper(next()));
    }
  }

  public void processOneOrMoreElements(String tagName, Consumer<ElementWrapper> consumer) {
    boolean foundOne = false;
    while (hasNext() && peek().getTagName().equals(tagName)) {
      consumer.accept(new ElementWrapper(next()));
      foundOne = true;
    }
    if (!foundOne) {
      throw new IllegalStateException("Expected to find at least one [" + tagName + "]");
    }
  }

  public void processOptionalTextElements(String tagName, Consumer<String> consumer) {
    processOptionalElements(tagName, ele -> consumer.accept(ele.getTextContent()));
  }

  private String getTextContent() {
    return element.getTextContent();
  }

  private Element findNextElement(Node node) {
    while (node != null && node.getNodeType() != Node.ELEMENT_NODE) {
      node = node.getNextSibling();
    }

    return (Element) node;
  }

  private Element current() {
    return currentChild;
  }

  private Element peek() {
    return nextChild;
  }

  private boolean hasNext() {
    return nextChild != null;
  }

  private Element next() {
    currentChild = nextChild;
    nextChild = findNextElement(currentChild.getNextSibling());
    return currentChild;
  }

}
