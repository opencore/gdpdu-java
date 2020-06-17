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
import java.io.IOException;
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
import com.opencore.gdpdu.index.util.LoggingErrorHandler;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * This class can be used to parse an {@code index.xml} file into a {@link DataSet}.
 */
public class GdpduIndexParser {

  public static DataSet parseXmlFile(String path) throws IOException {
    return parseXmlFile(path, ParseMode.STRICT);
  }

  public static DataSet parseXmlFile(String path, ParseMode parseMode) throws IOException {
    return parseXmlFile(new File(path), parseMode);
  }

  public static DataSet parseXmlFile(File inputFile) throws IOException {
    return parseXmlFile(inputFile, ParseMode.STRICT);
  }

  public static DataSet parseXmlFile(File inputFile, ParseMode parseMode) throws IOException {
    validateInput(inputFile);

    DocumentBuilder db = getDocumentBuilder(parseMode);

    ElementWrapper rootElement = null;
    try {
      Document document = db.parse(inputFile);
      rootElement = new ElementWrapper(document.getDocumentElement());
    } catch (SAXException | IOException e) {
      throw new IOException("Failed parsing XML", e);
    }

    return parseDataSet(rootElement);
  }

  /**
   * This sets up a DocumentBuilder which can be used to parse the XML file.
   */
  private static DocumentBuilder getDocumentBuilder(ParseMode parseMode) {
    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();

    // These two are Sonar warnings https://sonarcloud.io/organizations/opencore/rules?open=java%3AS2755&rule_key=java%3AS2755
    dbFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    dbFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

    dbFactory.setValidating(parseMode == ParseMode.STRICT);

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
    db.setEntityResolver((publicId, systemId) -> {
      if (systemId.trim().toLowerCase().endsWith("gdpdu-01-09-2004.dtd")) {
        return new InputSource(GdpduIndexParser.class.getClassLoader().getResourceAsStream("gdpdu-01-09-2004.dtd"));
      } else if (systemId.trim().toLowerCase().endsWith("gdpdu-01-08-2002.dtd")) {
        return new InputSource(GdpduIndexParser.class.getClassLoader().getResourceAsStream("gdpdu-01-08-2002.dtd"));
      } else {
        return null;
      }
    });

    db.setErrorHandler(new LoggingErrorHandler());
    return db;
  }

  private static void validateInput(File inputFile) {
    if (inputFile == null) {
      throw new IllegalArgumentException("inputFile cannot be null");
    }
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
    element.processOptionalElements("Extension", ele -> dataSet.getExtensions().add(parseExtension(ele)));
    element.processTextElement("Version", dataSet::setVersion);
    element.processOptionalElement("DataSupplier", ele -> dataSet.setDataSupplier(parseDataSupplier(ele)));
    element.processOptionalTextElements("Command", ele -> dataSet.getPreCommands().add(ele));
    element.processOneOrMoreElements("Media", ele -> dataSet.getMedia().add(parseMedia(ele)));
    element.processOptionalTextElements("Command", ele -> dataSet.getPostCommands().add(ele));
    return dataSet;
  }

  private static Media parseMedia(ElementWrapper element) {
    Media media = new Media();
    element.processTextElement("Name", media::setName);
    element.processOptionalTextElements("Command", ele -> media.getPreCommands().add(ele));
    element.processOptionalElements("Table", ele -> media.getTables().add(parseTable(ele)));
    element.processOptionalTextElements("Command", ele -> media.getPostCommands().add(ele));
    element.processOptionalTextElement("AcceptNoTables", media::setAcceptNotables);
    return media;
  }

  private static Table parseTable(ElementWrapper element) {
    Table table = new Table();
    element.processTextElement("URL", table::setUrl);
    element.processOptionalTextElement("Name", table::setName);
    element.processOptionalTextElement("Description", table::setDescription);
    element.processOptionalElement("Validity", ele -> table.setValidity(parseValidity(ele)));
    element.processOptionalElement("ANSI", ele -> table.setEncoding(Encoding.ANSI));
    element.processOptionalElement("Macintosh", ele -> table.setEncoding(Encoding.Macintosh));
    element.processOptionalElement("OEM", ele -> table.setEncoding(Encoding.OEM));
    element.processOptionalElement("UTF16", ele -> table.setEncoding(Encoding.UTF16));
    element.processOptionalElement("UTF7", ele -> table.setEncoding(Encoding.UTF7));
    element.processOptionalElement("UTF8", ele -> table.setEncoding(Encoding.UTF8));
    element.processOptionalTextElement("DecimalSymbol", table::setDecimalSymbol);
    element.processOptionalTextElement("DigitGroupingSymbol", table::setDigitGroupingSymbol);
    element.processOptionalTextElement("SkipNumBytes", ele -> table.setSkipNumBytes(Long.parseLong(ele)));
    element.processOptionalElement("Range", ele -> table.setRange(parseRange(ele)));
    element.processOptionalTextElement("Epoch", table::setEpoch);
    element.processOptionalElement("VariableLength", ele -> table.setVariableLength(parseVariableLength(ele)));
    element.processOptionalElement("FixedLength", ele -> table.setFixedLength(parseFixedLength(ele)));
    return table;
  }

  // TODO: This needs finishing
  private static FixedLength parseFixedLength(ElementWrapper element) {
    FixedLength fixedLength = new FixedLength();
    //    element.processTextElement("Name", fixedLength::setName);
    //    element.processOptionalTextElement("Description", fixedLength::setDescription);
    return fixedLength;
  }

  private static VariableLength parseVariableLength(ElementWrapper element) {
    VariableLength variableLength = new VariableLength();
    element.processOptionalTextElement("ColumnDelimiter", variableLength::setColumnDelimiter);
    element.processOptionalTextElement("RecordDelimiter", variableLength::setRecordDelimiter);
    element.processOptionalTextElement("TextEncapsulator", variableLength::setTextEncapsulator);
    element.processOptionalElements("VariablePrimaryKey",
      ele -> variableLength.getVariablePrimaryKeys().add(parseVariableColumn(ele)));
    element.processOptionalElements("VariableColumn",
      ele -> variableLength.getVariableColumns().add(parseVariableColumn(ele)));
    element.processOptionalElements("ForeignKey", ele -> variableLength.getForeignKeys().add(parseForeignKey(ele)));
    return variableLength;
  }

  private static ForeignKey parseForeignKey(ElementWrapper element) {
    ForeignKey foreignKey = new ForeignKey();
    element.processOptionalTextElements("Name", ele -> foreignKey.getNames().add(ele));
    element.processTextElement("References", foreignKey::setReferences);
    element.processOptionalElements("Alias", ele -> {
      String[] entry = new String[2];
      element.processTextElement("From", ele2 -> entry[0] = ele2);
      element.processTextElement("To", ele2 -> entry[1] = ele2);
      foreignKey.getAliases().put(entry[0], entry[1]);
    });
    return foreignKey;
  }

  private static VariableColumn parseVariableColumn(ElementWrapper element) {
    VariableColumn variableColumn = new VariableColumn();
    element.processTextElement("Name", variableColumn::setName);
    element.processOptionalTextElement("Description", variableColumn::setDescription);

    element.processOptionalElement("Numeric", ele -> {
      variableColumn.setDataType(DataType.Numeric);
      element.processOptionalTextElement("ImpliedAccuracy", ele2 -> {
        variableColumn.setAccuracyType(AccuracyType.ImpliedAccuracy);
        variableColumn.setAccuracy(Long.parseLong(ele2));
      });
      element.processOptionalTextElement("Accuracy", ele2 -> {
        variableColumn.setAccuracyType(AccuracyType.Accuracy);
        variableColumn.setAccuracy(Long.parseLong(ele2));
      });
    });
    element.processOptionalElement("AlphaNumeric", ele -> variableColumn.setDataType(DataType.AlphaNumeric));
    element.processOptionalTextElement("MaxLength", ele -> variableColumn.setMaxLength(Long.valueOf(ele)));
    element.processOptionalElement("Date", ele -> {
      variableColumn.setDataType(DataType.Date);
      element.processOptionalTextElement("Format", variableColumn::setFormat);
    });

    element.processOptionalElements("Map", ele -> variableColumn.getMappings().add(parseMapping(ele)));
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
    element.processElement("Range", ele -> validity.setRange(parseRange(ele)));
    element.processOptionalTextElement("Format", validity::setFormat);
    return validity;
  }

  private static Range parseRange(ElementWrapper element) {
    Range range = new Range();
    element.processTextElement("From", range::setFrom);
    element.processOptionalTextElement("To", range::setTo);
    element.processOptionalTextElement("To", range::setLength);
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

  public enum ParseMode {
    /**
     * Strict mode does validate DTD
     */
    STRICT,
    LENIENT
  }

}
