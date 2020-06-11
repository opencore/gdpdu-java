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
package com.opencore.gdpdu;

import java.io.File;
import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import com.opencore.gdpdu.models.AccuracyType;
import com.opencore.gdpdu.models.DataSet;
import com.opencore.gdpdu.models.DataSupplier;
import com.opencore.gdpdu.models.DataType;
import com.opencore.gdpdu.models.Encoding;
import com.opencore.gdpdu.models.Extension;
import com.opencore.gdpdu.models.FixedLength;
import com.opencore.gdpdu.models.ForeignKey;
import com.opencore.gdpdu.models.Mapping;
import com.opencore.gdpdu.models.Media;
import com.opencore.gdpdu.models.Range;
import com.opencore.gdpdu.models.Table;
import com.opencore.gdpdu.models.Validity;
import com.opencore.gdpdu.models.VariableColumn;
import com.opencore.gdpdu.models.VariableLength;
import com.opencore.gdpdu.util.DocumentWrapper;
import com.opencore.gdpdu.util.ElementWrapper;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

// TODO: Should add a method to validate the DataSet using Hibernate Validator
public class GdpduParser {

  public static DataSet parseXmlFile(String path) {
    return parseXmlFile(new File(path));
  }

  public static DataSet parseXmlFile(File inputFile) {
    if (!inputFile.canRead()) {
      throw new IllegalArgumentException("File does not exist or can not be read");
    }

    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    dbFactory.setValidating(true);

    DocumentBuilder db;
    try {
      db = dbFactory.newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      throw new IllegalStateException(e);
    }

    // This makes it so that both published DTD versions can be read from classpath
    // Usually the parser only looks relative to the source file.
    db.setEntityResolver((publicId, systemId) -> {
      if (systemId.trim().toLowerCase().endsWith("gdpdu-01-09-2004.dtd")) {
        return new InputSource(GdpduTool.class.getClassLoader().getResourceAsStream("gdpdu-01-09-2004.dtd"));
      } else if (systemId.trim().toLowerCase().endsWith("gdpdu-01-08-2002.dtd")) {
        return new InputSource(GdpduTool.class.getClassLoader().getResourceAsStream("gdpdu-01-08-2002.dtd"));
      } else {
        return null;
      }
    });

    // TODO: Should log all errors but then stop if it encountered any
    db.setErrorHandler(new ErrorHandler() {
      @Override
      public void warning(SAXParseException exception) throws SAXException {
        System.err.println(exception);
      }

      @Override
      public void error(SAXParseException exception) throws SAXException {
        System.err.println(exception);
      }

      @Override
      public void fatalError(SAXParseException exception) throws SAXException {
        System.err.println(exception);
      }
    });

    DocumentWrapper doc;
    try {
      doc = new DocumentWrapper(db.parse(inputFile));
    } catch (SAXException | IOException e) {
      // TODO: Proper exception
      throw new IllegalStateException(e);
    }

    ElementWrapper rootElement = doc.getDocumentElement();

    return parseDataSet(rootElement);
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
    element.processOptionalElements("ForeignKey", ele -> variableLength.getForeignKeys().add(parseForeingKey(ele)));
    return variableLength;
  }

  private static ForeignKey parseForeingKey(ElementWrapper element) {
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

}
