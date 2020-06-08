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

/**
 * Supported codepages:
 * Be careful to explicitly define RecordDelimiter when using a non-default codepage.
 * <p/>
 * ANSI is the default codepage when not specified
 * <p/>
 * <ul>
 *   <li>Windows: 7-Bit-ASCII-Zeichensatz (7-Bit-ISO Code, US-Variante), nur Zeichen mit ASCII-Code kleiner 128, also Buchstaben, Ziffern und Sonderzeichen (Punkt, Komma etc.) ohne nationale Sonderzeichen (Umlaute etc.) und ohne Graphikzeichen.
 *   Zeilentrenner ist LF (Line Feed), das Zeichen 10.</li>
 *   <li>PC unter DOS: 8-Bit-Zeichensatz "IBM-PC-ASCII", der in den unteren 127 Zeichen dem 7-Bit-ASCII entspricht und ab Zeichen 128 nationale Sonderzeichen und (Semi-) Graphikzeichen enthält.
 *   Zeilentrenner ist die Folge CR und LF, das sind die Zeichen 12 und 10 (Carriage Return, Line Feed).</li>
 *   <li>PC unter Windows: 8-Bit-ANSI-Zeichensatz, der bis 127 ASCII entspricht und oberhalb 127 nationale Sonderzeichen enthält, die nicht mit denen des IBM-PC-ASCII übereinstimmen.
 *   Zeilentrenner ist wie unter DOS die Folge CR, LF.
 *   (Spezielle Anwendungen können auch einen 16-Bit-Code, den Unicode benutzen.)</li>
 *   <li>Apple Macintosh: Macintosh spezifischer 8-Bit-Zeichensatz "Mac-ASCII", der bis 127 ASCII entspricht und oberhalb 127 nationale Sonderzeichen enthält.
 *   Zeilentrenner ist CR.</li>
 * </ul>
 * Die Wahl der Codepage setzt keine Vorgabe für den RecordDelimiter.
 * Sie müssen den RecordDelimiter immer explizit angeben, wenn der Vorgabewert (CRLF) nicht geeignet ist.
 */
@SuppressWarnings("FieldNamingConvention")
public enum Encoding {

  /**
   * Legt die Verwendung der Codepage ANSI fest.
   */
  ANSI,

  /**
   * Legt die Verwendung der Codepage Macintosh fest.
   */
  Macintosh,

  /**
   * Legt die Verwendung der Codepage IBM-PC-ASCII fest.
   */
  OEM,

  /**
   * Legt die Verwendung der Codepage UTF16 fest.
   */
  UTF16,

  /**
   * Legt die Verwendung der Codepage UTF7 fest.
   */
  UTF7,

  /**
   * Legt die Verwendung der Codepage UTF8 fest.
   */
  UTF8

}
