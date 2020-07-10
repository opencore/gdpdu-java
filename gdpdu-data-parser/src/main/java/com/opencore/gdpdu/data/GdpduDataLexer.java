package com.opencore.gdpdu.data;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.validation.constraints.NotNull;

import com.opencore.gdpdu.common.exceptions.ParsingException;
import com.opencore.gdpdu.data.deserializers.DeserializationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is not thread-safe.
 */
// TODO: Make sure that the builder doesn't grow unbounded, allow a limit in case we see malformed data files or have a bug while parsing
// TODO: Support Range in the Parser?
public class GdpduDataLexer {

  private static final Logger LOG = LoggerFactory.getLogger(GdpduDataLexer.class);

  private List<Record> records;
  private Record currentRecord;
  private StringBuilder builder;
  private final DeserializationContext context;

  // This Trie includes all states for when we're not sure yet where exactly we are
  private final Trie unknownTrie;

  // This Trie is used when we are within a column that is not encapsulated by the text encapsulator
  private final Trie unencapsulatedTrie;
  private final Trie encapsulatedTrie;

  private int currentIndex = 0;

  public GdpduDataLexer(@NotNull DeserializationContext context) {
    Objects.requireNonNull(context, "`context` can't be null");

    this.context = context;
    unknownTrie = buildTrie(context.getRecordDelimiter(), context.getColumnDelimiter(), context.getTextEncapsulator());
    unencapsulatedTrie = buildTrie2(context.getRecordDelimiter(), context.getColumnDelimiter());
    encapsulatedTrie = buildTrie3(context.getTextEncapsulator());
  }

  // TODO:IOException -> ParsingException
  public List<Record> parseData(@NotNull InputStream inputStream) throws ParsingException {
    Objects.requireNonNull(inputStream, "`inputStream` can't be null");

    records = new ArrayList<>();
    resetTries();
    currentIndex = 0;
    builder = new StringBuilder();
    currentRecord = new Record();

    try {
      long skip = inputStream.skip(context.getSkipNumBytes());
      if (skip != context.getSkipNumBytes()) {
        LOG.warn("Skipped only [{}] bytes instead of [{}]", skip, context.getSkipNumBytes());
      }
    } catch (IOException e) {
      throw new ParsingException(e);
    }

    try (InputStreamReader reader = new InputStreamReader(inputStream, context.getCharset())) {
      ParsingState state = ParsingState.UNKNOWN; // At the very beginning we have no idea what's to come
      int currentChar;
      while ((currentChar = reader.read()) != -1) {
        currentIndex++;

        switch (state) {
          /*
          At this point we can have one of these things coming up:
          * An unencapsulated text column followed by a column or record delimiter
          * A text encapsulator followed by text another text encapsulator and a column or record delimiter
          * No text at all just a column or record delimiter
          * EOF or Text until EOF
           */
          case UNKNOWN: {
            // We need to store the characters because we're not sure yet if it'll become regular text
            builder.appendCodePoint(currentChar);

            OUTPUT output = unknownTrie.search(currentChar);
            switch (output) {
              case NONE:
                // Not sure yet, don't do anything.
                // We'll have to check at least as many characters until we have checked the maximum of the length of Record-, Column Delimiter or TextEncapsulator
                break;

              case RECORD_DELIMITER:
                // We found a Record Delimiter and as we are not in an encapsulated string this will begin a new record
                LOG.trace("Found Record Delimiter [State: Unknown]");

                // We need to delete the captured chars from the record delimiter
                builder.delete(builder.length() - context.getRecordDelimiter().length(), builder.length());
                newRecord();
                break;

              case COLUMN_DELIMITER:
                LOG.trace("Found Column Delimiter [State: Unknown]");

                builder.delete(builder.length() - context.getColumnDelimiter().length(), builder.length());
                newColumn();
                break;

              case TEXT_ENCAPSULATOR:
                LOG.trace("Text Encapsulator [State: Unknown]");

                resetTries();
                builder = new StringBuilder(); // Everything we parsed so far can be ignored as it's just going to be the Encapsulator
                state = ParsingState.ENCAPSULATED_COLUMN;
                break;
            }

            // We have parsed more characters than we need to distinguish between all delimiters so now we can be sure that we're in a regular column
            if (state == ParsingState.UNKNOWN && currentIndex >= Math.max(Math.max(context.getTextEncapsulator().length(), context.getRecordDelimiter().length()), context.getColumnDelimiter().length())) {
              LOG.trace("Found unencapsulated column");
              state = ParsingState.UNENCAPSULATED_COLUMN;
            }

            break;
          }

          case UNENCAPSULATED_COLUMN: {
            // An unencapsulated column can only be ended by a column- or record delimiter
            OUTPUT output = unencapsulatedTrie.search(currentChar);

            switch (output) {
              case NONE:
                builder.appendCodePoint(currentChar);
                break;

              case RECORD_DELIMITER:
                LOG.trace("Found Record Delimiter [State: Unencapsulated]");

                builder.delete(builder.length() - context.getRecordDelimiter().length() + 1, builder.length());
                newRecord();
                state = ParsingState.UNKNOWN;
                break;

              case COLUMN_DELIMITER:
                LOG.trace("Found Column Delimiter [State: Unencapsulated]");

                builder.delete(builder.length() - context.getColumnDelimiter().length() + 1, builder.length());
                newColumn();
                state = ParsingState.UNKNOWN;
                break;
            }
            break;
          }

          case ENCAPSULATED_COLUMN: {
            OUTPUT output = encapsulatedTrie.search(currentChar);
            switch (output) {
              case NONE:
                builder.appendCodePoint(currentChar);
                break;

              case TEXT_ENCAPSULATOR:
                // TODO: Fail if not column separator next symbol
                LOG.trace("Found Text Encapsulator [State: Encapsulated]");

                resetTries();
                builder.delete(builder.length() - context.getTextEncapsulator().length() + 1, builder.length());
                currentIndex = 0;
                state = ParsingState.UNKNOWN;

            }
            break;
          }

        }
      }
    } catch (IOException e) {
      throw new ParsingException(e);
    }
    LOG.trace("End of File");
    if (builder.length() > 0) {
      newRecord();
    } else {
      records.add(currentRecord);
    }

    return records;
  }

  private void newColumn() {
    String column = builder.toString();
    currentRecord.addColumn(column);
    currentIndex = 0;

    resetTries();
    builder = new StringBuilder();
    LOG.trace("Found column [{}]", column);
  }

  private void newRecord() {
    newColumn();
    records.add(currentRecord);
    currentRecord = new Record();
  }

  private void resetTries() {
    unknownTrie.reset();
    encapsulatedTrie.reset();
    unencapsulatedTrie.reset();
  }

  private static Trie buildTrie(String recordDelimiter, String columnDelimiter, String textEncapsulator) {
    Trie trie = new Trie();
    trie.insert(recordDelimiter, OUTPUT.RECORD_DELIMITER);
    trie.insert(columnDelimiter, OUTPUT.COLUMN_DELIMITER);
    trie.insert(textEncapsulator, OUTPUT.TEXT_ENCAPSULATOR);
    return trie;
  }

  private static Trie buildTrie2(String recordDelimiter, String columnDelimiter) {
    Trie trie = new Trie();
    trie.insert(recordDelimiter, OUTPUT.RECORD_DELIMITER);
    trie.insert(columnDelimiter, OUTPUT.COLUMN_DELIMITER);
    return trie;
  }

  private static Trie buildTrie3(String textEncapsulator) {
    Trie trie = new Trie();
    trie.insert(textEncapsulator, OUTPUT.TEXT_ENCAPSULATOR);
    return trie;
  }

  private enum OUTPUT {
    NONE,
    RECORD_DELIMITER,
    COLUMN_DELIMITER,
    TEXT_ENCAPSULATOR
  }

  private enum ParsingState {
    UNKNOWN, // When we haven't checked yet whether there's a TextEncapsulator at the beginning of the column, a column can also be totally empty, we can be at the EOF, ...
    UNENCAPSULATED_COLUMN,
    ENCAPSULATED_COLUMN
  }

  private static class Trie {

    private final TrieNode rootNode = new TrieNode();

    private TrieNode currentNode = rootNode;

    private void reset() {
      currentNode = rootNode;
    }

    private void insert(String word, OUTPUT output) {
      Map<Integer, TrieNode> children = rootNode.getChildren();
      for (int i = 0; i < word.length(); i++) {
        int codePoint = word.codePointAt(i);
        TrieNode childNode = children.get(codePoint);
        if (childNode == null) {
          childNode = new TrieNode(codePoint);
          children.put(codePoint, childNode);
        }
        if (childNode.getState() != OUTPUT.NONE) {
          System.err.println("Already an output output");
        }
        children = childNode.getChildren();

        if (i == word.length() - 1) {
          childNode.setState(output);
        }

      }
    }

    private OUTPUT search(int c) {
      currentNode = currentNode.getChildren().get(c);
      if (currentNode == null) {
        reset();
        return OUTPUT.NONE;
      }
      OUTPUT resultState = currentNode.getState();
      if (resultState != OUTPUT.NONE) {
        reset();
      }
      return resultState;
    }
  }

  private static final class TrieNode {

    private int i;
    private final Map<Integer, TrieNode> children = new HashMap<>();
    private OUTPUT state = OUTPUT.NONE;

    private TrieNode() {
    }

    private TrieNode(int i) {
      this.i = i;
    }

    public Map<Integer, TrieNode> getChildren() {
      return children;
    }

    public OUTPUT getState() {
      return state;
    }

    public void setState(OUTPUT state) {
      this.state = state;
    }

  }

}
