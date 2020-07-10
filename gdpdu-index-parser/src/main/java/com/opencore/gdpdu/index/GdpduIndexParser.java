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
package com.opencore.gdpdu.index;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Objects;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import com.opencore.gdpdu.index.models.AccuracyType;
import com.opencore.gdpdu.index.models.DataSet;
import com.opencore.gdpdu.index.models.DataSupplier;
import com.opencore.gdpdu.index.models.DataType;
import com.opencore.gdpdu.index.models.Encoding;
import com.opencore.gdpdu.index.models.Extension;
import com.opencore.gdpdu.index.models.FixedColumn;
import com.opencore.gdpdu.index.models.FixedLength;
import com.opencore.gdpdu.index.models.ForeignKey;
import com.opencore.gdpdu.index.models.Mapping;
import com.opencore.gdpdu.index.models.Media;
import com.opencore.gdpdu.index.models.Range;
import com.opencore.gdpdu.index.models.Table;
import com.opencore.gdpdu.index.models.Validity;
import com.opencore.gdpdu.index.models.VariableColumn;
import com.opencore.gdpdu.index.models.VariableLength;
import com.opencore.gdpdu.index.util.ElementWrapper;
import com.opencore.gdpdu.index.util.ThrowingErrorHandler;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * This class can be used to parse an {@code index.xml} file into a {@link DataSet}.
 * <p/>
 * Implementation note: This does not use a more sophisticated (JAXB or otherwise) XML parser on purpose.
 * First, this does not require any dependencies and second the Commands for Media are problematic to map because they appear twice. Once before the tables and once after.
 * I did not find a way to map this correctly into two distinct lists using JAXB.
 */
public final class GdpduIndexParser {

  private GdpduIndexParser() {
  }

  public static DataSet parseXmlFile(String path) throws IOException {
    return parseXmlFile(new File(path));
  }

  public static DataSet parseXmlFile(File inputFile) throws IOException {
    validateInput(inputFile);
    return parseXmlFile(new FileInputStream(inputFile));
  }

  /**
   * This tries to parse a file according to the GDPdU/GoBD standard.
   * It does not do any validation apart from schema validation according to the DTD.
   */
  public static DataSet parseXmlFile(InputStream inputStream) throws IOException {
    Objects.requireNonNull(inputStream, "'inputStream' can't be null");
    DocumentBuilder db = getDocumentBuilder();

    ElementWrapper rootElement;
    try {
      Document document = db.parse(inputStream);
      rootElement = new ElementWrapper(document.getDocumentElement());
    } catch (SAXException | IOException e) {
      throw new IOException("Failed parsing XML", e);
    }

    return parseDataSet(rootElement);
  }

  /**
   * This sets up a DocumentBuilder which can be used to parse the XML file.
   */
  private static DocumentBuilder getDocumentBuilder() {
    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();

    // These two are Sonar warnings https://sonarcloud.io/organizations/opencore/rules?open=java%3AS2755&rule_key=java%3AS2755
    dbFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    dbFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

    dbFactory.setValidating(true);

    DocumentBuilder db;
    try {
      db = dbFactory.newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      throw new IllegalStateException("Setting up the XML Parser failed", e);
    }

    /*
    This makes it so that both published DTD versions can be read from classpath
    Usually the parser only looks relative to the source file.
    As per the GdPDU spec it is actually required to have the DTD file next to the index.xml file.
    We actually disable reading the DTD from the filesystem due to security concerns so if it doesn't match one of these well known ones we'll fail
     */
    db.setEntityResolver((String publicId, String systemId) -> {
      if (systemId.trim().toLowerCase(Locale.ROOT).endsWith("gdpdu-01-09-2004.dtd")) {
        return new InputSource(Thread.currentThread().getContextClassLoader().getResourceAsStream("gdpdu-01-09-2004.dtd"));
      } else if (systemId.trim().toLowerCase(Locale.ROOT).endsWith("gdpdu-01-08-2002.dtd")) {
        return new InputSource(Thread.currentThread().getContextClassLoader().getResourceAsStream("gdpdu-01-08-2002.dtd"));
      } else {
        return null;
      }
    });

    db.setErrorHandler(new ThrowingErrorHandler());
    return db;
  }

  private static void validateInput(File inputFile) {
    Objects.requireNonNull(inputFile, "'inputFile' can't be null");

    String msg = "inputFile [" + inputFile + "]";

    if (inputFile.isDirectory()) {
      throw new IllegalArgumentException(msg + " needs to be a file, is directory");
    }
    if (!inputFile.exists()) {
      throw new IllegalArgumentException(msg + " does not exist");
    }
    if (!inputFile.canRead()) {
      throw new IllegalArgumentException(msg + " can't be read");
    }
  }

  private static DataSet parseDataSet(ElementWrapper element) {
    DataSet dataSet = new DataSet();
    element.processOptionalElements("Extension", (ElementWrapper ele) -> dataSet.addExtension(parseExtension(ele)));
    element.processTextElement("Version", dataSet::setVersion);
    element.processOptionalElement("DataSupplier", (ElementWrapper ele) -> dataSet.setDataSupplier(parseDataSupplier(ele)));
    element.processOptionalTextElements("Command", dataSet::addPreCommand);
    element.processOneOrMoreElements("Media", (ElementWrapper ele) -> dataSet.addMedia(parseMedia(ele)));
    element.processOptionalTextElements("Command", dataSet::addPostCommand);
    return dataSet;
  }

  private static Media parseMedia(ElementWrapper element) {
    Media media = new Media();
    element.processTextElement("Name", media::setName);
    element.processOptionalTextElements("Command", media::addPreCommand);
    element.processOptionalElements("Table", (ElementWrapper ele) -> media.addTable(parseTable(ele)));
    element.processOptionalTextElements("Command", media::addPostCommand);
    element.processOptionalTextElement("AcceptNoTables", media::setAcceptNotables);
    return media;
  }

  private static Table parseTable(ElementWrapper element) {
    Table table = new Table();
    element.processTextElement("URL", table::setUrl);
    element.processOptionalTextElement("Name", table::setName);
    element.processOptionalTextElement("Description", table::setDescription);
    element.processOptionalElement("Validity", (ElementWrapper ele) -> table.setValidity(parseValidity(ele)));
    element.processOptionalElement("ANSI", (ElementWrapper ele) -> table.setEncoding(Encoding.ANSI));
    element.processOptionalElement("Macintosh", (ElementWrapper ele) -> table.setEncoding(Encoding.Macintosh));
    element.processOptionalElement("OEM", (ElementWrapper ele) -> table.setEncoding(Encoding.OEM));
    element.processOptionalElement("UTF16", (ElementWrapper ele) -> table.setEncoding(Encoding.UTF16));
    element.processOptionalElement("UTF7", (ElementWrapper ele) -> table.setEncoding(Encoding.UTF7));
    element.processOptionalElement("UTF8", (ElementWrapper ele) -> table.setEncoding(Encoding.UTF8));
    element.processOptionalTextElement("DecimalSymbol", table::setDecimalSymbol);
    element.processOptionalTextElement("DigitGroupingSymbol", table::setDigitGroupingSymbol);
    element.processOptionalTextElement("SkipNumBytes", (String ele) -> table.setSkipNumBytes(Long.parseLong(ele)));
    element.processOptionalElement("Range", (ElementWrapper ele) -> table.setRange(parseRange(ele)));
    element.processOptionalTextElement("Epoch", table::setEpoch);
    element.processOptionalElement("VariableLength", (ElementWrapper ele) -> table.setVariableLength(parseVariableLength(ele)));
    element.processOptionalElement("FixedLength", (ElementWrapper ele) -> table.setFixedLength(parseFixedLength(ele)));
    return table;
  }

  private static FixedLength parseFixedLength(ElementWrapper element) {
    FixedLength fixedLength = new FixedLength();
    element.processOptionalTextElement("Length", (String ele) -> fixedLength.setLength(Long.parseLong(ele)));
    element.processOptionalTextElement("RecordDelimiter", fixedLength::setRecordDelimiter);
    element.processOptionalElements("FixedPrimaryKey", (ElementWrapper ele) -> fixedLength.addPrimaryKey(parseFixedColumn(ele)));
    element.processOptionalElements("FixedColumn",
      (ElementWrapper ele) -> fixedLength.addFixedColumn(parseFixedColumn(ele)));
    element.processOptionalElements("ForeignKey", (ElementWrapper ele) -> fixedLength.addForeignKey(parseForeignKey(ele)));

    return fixedLength;
  }

  private static VariableLength parseVariableLength(ElementWrapper element) {
    VariableLength variableLength = new VariableLength();
    element.processOptionalTextElement("ColumnDelimiter", variableLength::setColumnDelimiter);
    element.processOptionalTextElement("RecordDelimiter", variableLength::setRecordDelimiter);
    element.processOptionalTextElement("TextEncapsulator", variableLength::setTextEncapsulator);
    element.processOptionalElements("VariablePrimaryKey", (ElementWrapper ele) -> variableLength.addVariablePrimaryKey(parseVariableColumn(ele)));
    element.processOptionalElements("VariableColumn", (ElementWrapper ele) -> variableLength.addVariableColumn(parseVariableColumn(ele)));
    element.processOptionalElements("ForeignKey", (ElementWrapper ele) -> variableLength.addForeignKey(parseForeignKey(ele)));
    return variableLength;
  }

  private static ForeignKey parseForeignKey(ElementWrapper element) {
    ForeignKey foreignKey = new ForeignKey();
    element.processOptionalTextElements("Name", foreignKey::addName);
    element.processTextElement("References", foreignKey::setReferences);
    element.processOptionalElements("Alias", (ElementWrapper ele) -> {
      String[] entry = new String[2];
      element.processTextElement("From", (String ele2) -> entry[0] = ele2);
      element.processTextElement("To", (String ele2) -> entry[1] = ele2);
      foreignKey.addAlias(entry[0], entry[1]);
    });
    return foreignKey;
  }

  private static FixedColumn parseFixedColumn(ElementWrapper element) {
    FixedColumn fixedColumn = new FixedColumn();
    element.processTextElement("Name", fixedColumn::setName);
    element.processOptionalTextElement("Description", fixedColumn::setDescription);

    element.processOptionalElement("Numeric", (ElementWrapper ele) -> {
      fixedColumn.setDataType(DataType.Numeric);
      element.processOptionalTextElement("ImpliedAccuracy", (String ele2) -> {
        fixedColumn.setAccuracyType(AccuracyType.ImpliedAccuracy);
        fixedColumn.setAccuracy(Long.parseLong(ele2));
      });
      element.processOptionalTextElement("Accuracy", (String ele2) -> {
        fixedColumn.setAccuracyType(AccuracyType.Accuracy);
        fixedColumn.setAccuracy(Long.parseLong(ele2));
      });
    });

    element.processOptionalElement("AlphaNumeric", (ElementWrapper ele) -> fixedColumn.setDataType(DataType.AlphaNumeric));

    element.processOptionalElement("Date", (ElementWrapper ele) -> {
      fixedColumn.setDataType(DataType.Date);
      element.processOptionalTextElement("Format", fixedColumn::setFormat);
    });

    element.processOptionalElements("Map", (ElementWrapper ele) -> fixedColumn.addMapping(parseMapping(ele)));

    element.processElement("FixedRange", (ElementWrapper ele) -> fixedColumn.setFixedRange(parseRange(ele)));

    return fixedColumn;
  }

  private static VariableColumn parseVariableColumn(ElementWrapper element) {
    VariableColumn variableColumn = new VariableColumn();
    element.processTextElement("Name", variableColumn::setName);
    element.processOptionalTextElement("Description", variableColumn::setDescription);

    element.processOptionalElement("Numeric", (ElementWrapper numericElement) -> {
      variableColumn.setDataType(DataType.Numeric);
      numericElement.processOptionalTextElement("ImpliedAccuracy", (String ele2) -> {
        variableColumn.setAccuracyType(AccuracyType.ImpliedAccuracy);
        variableColumn.setAccuracy(Long.parseLong(ele2));
      });
      numericElement.processOptionalTextElement("Accuracy", (String ele2) -> {
        variableColumn.setAccuracyType(AccuracyType.Accuracy);
        variableColumn.setAccuracy(Long.parseLong(ele2));
      });
    });

    element.processOptionalElement("AlphaNumeric", (ElementWrapper ele) -> variableColumn.setDataType(DataType.AlphaNumeric));
    element.processOptionalTextElement("MaxLength", (String ele) -> variableColumn.setMaxLength(Long.valueOf(ele)));

    element.processOptionalElement("Date", (ElementWrapper ele) -> {
      variableColumn.setDataType(DataType.Date);
      element.processOptionalTextElement("Format", variableColumn::setFormat);
    });

    element.processOptionalElements("Map", (ElementWrapper ele) -> variableColumn.addMapping(parseMapping(ele)));
    return variableColumn;
  }

  private static Mapping parseMapping(ElementWrapper element) {
    Mapping mapping = new Mapping();
    element.processOptionalTextElement("Description", mapping::setDescription);
    element.processOptionalTextElement("From", mapping::setFrom);
    element.processOptionalTextElement("To", mapping::setTo);
    return mapping;
  }

  private static Validity parseValidity(ElementWrapper element) {
    Validity validity = new Validity();
    element.processElement("Range", (ElementWrapper ele) -> validity.setRange(parseRange(ele)));
    element.processOptionalTextElement("Format", validity::setFormat);
    return validity;
  }

  private static Range parseRange(ElementWrapper element) {
    Range range = new Range();
    element.processTextElement("From", range::setFrom);
    element.processOptionalTextElement("To", range::setTo);
    element.processOptionalTextElement("Length", range::setLength);
    return range;
  }

  private static DataSupplier parseDataSupplier(ElementWrapper element) {
    DataSupplier dataSupplier = new DataSupplier();
    element.processTextElement("Name", dataSupplier::setName);
    element.processTextElement("Location", dataSupplier::setLocation);
    element.processTextElement("Comment", dataSupplier::setComment);
    return dataSupplier;
  }

  private static Extension parseExtension(ElementWrapper element) {
    Extension extension = new Extension();
    element.processTextElement("Name", extension::setName);
    element.processTextElement("URL", extension::setUrl);
    return extension;
  }

}
