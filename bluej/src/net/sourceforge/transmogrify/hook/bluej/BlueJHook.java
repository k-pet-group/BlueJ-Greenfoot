/*
Copyright (C) 2001  ThoughtWorks, Inc

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
*/
package net.sourceforge.transmogrify.hook.bluej;

// $Id: BlueJHook.java 1011 2001-11-22 10:36:26Z ajp $

import java.io.*;
import java.util.*;

import javax.swing.*;
import javax.swing.text.*;

import net.sourceforge.transmogrify.hook.Hook;
import net.sourceforge.transmogrify.symtab.IDefinition;

public class BlueJHook extends Hook
{
  private File f;
  private JEditorPane editorPane;

  private final String NEW_LINE = System.getProperty("line.separator");
  // NOTE: A new line is actually 2 characters long but 1 reflects how the
  // caret positioning works
  private final int NEW_LINE_LENGTH = 1;

  public BlueJHook(JEditorPane editorPane, File f)
  {
    this.editorPane = editorPane;
    this.f = f;
  }

  /**
   * Deletes a text string which is selected between startPos and endPos.
   * NOTE: startPos may also be to the right of (i.e., greater than) the
   * endPos
   *
   * @param startPos place to start deleting text
   * @param endPos   place to stop deleting text.
   * @throws Exception thrown on any type of error
   */
  public void deleteText(int startPos, int endPos) throws Exception
  {
    editorPane.setCaretPosition(startPos);
    editorPane.moveCaretPosition(endPos);
    editorPane.replaceSelection("");
  }


  public void reparse() throws Exception
  {
    System.out.println("reparsing");
//    nopants.reparse(this);
  }

  /**
   * Displays a list of references.
   */
  public void showReferencesSourced(IDefinition definition)
  {
    //(REDTAG) pending...
    throw new IllegalStateException("This method cannot be called.");
  }

  public void showReferencesNonsourced(IDefinition definition)
  {
    throw new IllegalStateException("This method cannot be called.");
  }

  /**
   * @param prompt question to ask user
   * @param title title of dialog box
   * @return String entered by user
   * @throws Exception thrown on any type of error
   */
  public String getUserInput(String prompt, String title) throws Exception
  {
    return JOptionPane.showInputDialog(null, prompt, title, JOptionPane.QUESTION_MESSAGE);
  }

  public void displayMessage(String title, String message) throws Exception
  {
    JOptionPane.showMessageDialog(null, message, title, JOptionPane.PLAIN_MESSAGE);
  }

  public void displayException(Exception e, String description)
  {
    StringWriter stackTrace = new StringWriter();
    e.printStackTrace(new PrintWriter(stackTrace));
    JTextArea errorDialog = new JTextArea(6, 60);
    errorDialog.append(description + stackTrace);
    errorDialog.setCaretPosition(0);
    JScrollPane errorPane = new JScrollPane(errorDialog);
    JOptionPane.showMessageDialog( null, errorPane, "Refactoring Error", JOptionPane.WARNING_MESSAGE);
  }

  /**
   * Sets focus to specified java source file
   *
   * @param sourceFileName
   * @throws Exception thrown on any type of error
   */
  public void openFile(String sourceFileName) throws Exception
  {
    //(REDTAG) doesn't work with absolute file names
    System.out.println("asked to open " + sourceFileName);

    f = new File(sourceFileName);
  }

  /**
   * Opens a file chooser
   *
   * @return an array of files selected by the user
   * @throws Exception thrown on any type of error
   */
  public File[] getFiles() throws Exception
  {
    JFileChooser chooser;

    chooser = new JFileChooser();

    chooser.setMultiSelectionEnabled( true );
    chooser.setFileSelectionMode( JFileChooser.FILES_AND_DIRECTORIES );

    int option = chooser.showOpenDialog( null );
    File[] result = new File[0];

    if (option == JFileChooser.APPROVE_OPTION) {
      result = chooser.getSelectedFiles();
    }

    return result;
  }

  /**
   * @return file name of currently focused file
   * @throws Exception thrown on any type of error
   */
  public String getCurrentFile() throws Exception
  {
    System.out.println("asked for current file returning " + f);
    return f.getAbsolutePath();
  }

  /**
   * Inserts a line into the focused file.
   *
   * @param lineNumber line number after which you wish to insert the new line
   * @param line the new line to be inserted
   * @throws Exception thrown on any type of error
   */
  public void insertLine(int lineNumber, String line) throws Exception
  {
    if (lineNumber < 0) {
      throw new IllegalArgumentException("lineNumber is negative!: " + lineNumber);
    }

    BufferedReader reader = getDocumentTextReader();
    int lineCount = 0;
    int offset = 0;

    String currLine = reader.readLine();
    while (currLine != null && lineCount < lineNumber) {
      offset += currLine.length() + NEW_LINE_LENGTH;
      lineCount++;
      currLine = reader.readLine();
    }

    if (currLine == null) {
      if (lineCount < lineNumber) {
        throw new IllegalArgumentException(
         "lineNumber is past end of document: " + lineNumber);
      }

      if (lineCount > 0) {
        // no new line after last line
        offset--;
        line = NEW_LINE + line;
      }
    }

    editorPane.getDocument().insertString(offset, line + System.getProperty("line.separator"), null);
  }

  /**
   * @return entire text of focused source file
   * @throws Exception thrown on any type of error
   */
  public String getText() throws Exception
  {
    return getText(0, getNumChars());
  }

  /**
   * @param startPos
   * @param endPos
   * @return text of source file between startPos and endPos
   * @throws Exception thrown on any type of error
   */
  public String getText(int startPos, int endPos) throws Exception
  {
    System.out.println("getText()");
    Document document = editorPane.getDocument();
    return document.getText(startPos, endPos - startPos);
  }

  /**
   * Inserts a text string into the specified location
   * NOTE: Character based positions are 0 based.
   *
   * @param pos place after which to insert text
   * @param text text to be inserted
   * @throws Exception thrown on any type of error
   */
  public void insertText(int pos, String text) throws Exception
  {
    Document document = editorPane.getDocument();
    document.insertString(pos, text, null);
  }

  /**
   * @throws Exception thrown on any type of error
   */
  public void deSelectText() throws Exception
  {
    int currentPosition = editorPane.getCaretPosition();
    editorPane.setCaretPosition(currentPosition);
    editorPane.moveCaretPosition(currentPosition);
  }

  /**
   * @return number of characters in currently focused file
   * @throws Exception thrown on any type of error
   */
  public int getNumChars() throws Exception
  {
    return editorPane.getDocument().getLength();
  }

  /**
   * Retrieves a line from the focused file.
   *
   * @param lineNumber 1 based line number to be retrieved
   * @return
   * @throws Exception thrown on any type of error
   */
  public String getLine(int lineNumber) throws Exception
  {
    BufferedReader reader = getDocumentTextReader();

    String line = null;
    for (int i = 0; i < lineNumber; i++) {
      line = reader.readLine();
    }

    return line;
  }

  /**
   * Replaces a line from the focused file.
   *
   * @param lineNumber line to be replaced
   * @param line replacement line
   * @throws Exception thrown on any type of error
   */
  public void setLine(int lineNumber, String line) throws Exception
  {
    if (lineNumber < 1) {
      throw new IllegalArgumentException("lineNumber must be 1 or greater: " + lineNumber);
    }

    BufferedReader reader = getDocumentTextReader();

    String currLine = reader.readLine();
    int offset = 0;
    int lineCount = 1;
    while (currLine != null && lineCount < lineNumber) {
      offset += currLine.length() + NEW_LINE_LENGTH;
      currLine = reader.readLine();
      lineCount++;
    }

    if (currLine == null && lineCount < lineNumber) {
      throw new IllegalArgumentException(
        "lineNumber is past end of document!: " + lineNumber);
    }

    String targetLine = currLine;
    int length = targetLine.length();

    Document document = editorPane.getDocument();
    document.remove(offset, length);
    document.insertString(offset, line, null);
  }

  /**
   * Returns the 1 based offset of where the cursor is.
   *
   * @return the offset from the start of the current line
   * @throws Exception thrown on any type of error
   */
  public int getCaretOffset() throws Exception
  {
    int absoluteOffset = editorPane.getCaretPosition();

    BufferedReader reader = getDocumentTextReader();

    String currLine = reader.readLine();
    int roughOffset = currLine.length();
    while (roughOffset < absoluteOffset) {
      currLine = reader.readLine();
      roughOffset += currLine.length() + NEW_LINE_LENGTH;
    }

    int previousLinesOffset = roughOffset - currLine.length();

    System.out.println("getCaretOffset returns " + (absoluteOffset - previousLinesOffset));
    return absoluteOffset - previousLinesOffset;
  }

  /**
   * @return number of lines in currently focused file
   * @throws Exception thrown on any type of error
   */
  public int getNumLines() throws Exception {
    BufferedReader reader = getDocumentTextReader();

    int lineCount = 0;
    while (reader.readLine() != null) {
      lineCount++;
    }

    return lineCount;
  }

  /**
   * @throws Exception thrown on any type of error
   */
  public void selectText(int startLineNumber, int startOffset, int endLineNumber, int endOffset) throws Exception {
    if (startLineNumber > endLineNumber) {
      throw new IllegalArgumentException("Start line cannot be greater than end line");
    }

    BufferedReader reader = getDocumentTextReader();

    String currLine = null;
    int offset = 0;

    if (startLineNumber > 1) {
      for (int i = 1; i < startLineNumber; i++) {
        currLine = reader.readLine();
        offset += currLine.length() + NEW_LINE_LENGTH;
      }

      offset++; // increment to start of start line
    }

    int selectionStart = offset + startOffset;

    for (int j = startLineNumber; j < endLineNumber; j++) {
      currLine = reader.readLine();
      offset += currLine.length() + NEW_LINE_LENGTH;
    }

    int selectionEnd = offset + endOffset;

    selectText(selectionStart, selectionEnd);
  }

  /**
   * @param startPos 0 based start position
   * @param endPos 0 based end position
   * @throws Exception thrown on any type of error
   */
  public void selectText(int startPos, int endPos) throws Exception {
    editorPane.select(startPos, endPos);
  }

  /**
   * @throws Exception thrown on any type of error
   */
  public String getSelectedText() throws Exception {
    return editorPane.getSelectedText();
  }

  /**
   * @return Enumeration of selected lines, null if nothing selected
   * @throws Exception thrown on any type of error
   */
  public Enumeration getSelectedLines() throws Exception {
    String selected = getSelectedText();
    if (selected == null) {
      return null;
    }

    BufferedReader reader = new BufferedReader(new StringReader(selected));

    Vector lines = new Vector();
    String currLine = reader.readLine();
    while(currLine != null) {
      lines.add(currLine);
      currLine = reader.readLine();
    }

    return lines.elements();
  }

  /**
   * @return the 1 based line number of where the cursor is
   * @throws Exception thrown on any type of error
   */
  public int getCaretLineNumber() throws Exception {
    BufferedReader reader = getDocumentTextReader();
    int offset = editorPane.getCaretPosition();

    int lineNumber = 0;
    int currOffset = 0;

    while (currOffset <= offset) {
      String currLine = reader.readLine();
      currOffset += currLine.length() + NEW_LINE_LENGTH;
      lineNumber++;
    }

    System.out.println("getCaretLineNumber returns " + (lineNumber));

    return lineNumber;
  }

  /**
   * @return the 0 based position of where the cursor is
   * @throws Exception thrown on any type of error
   */
  public int getCaretPos() throws Exception {
    return editorPane.getCaretPosition();
  }

  public int getSelectionStart() throws Exception {
    return editorPane.getSelectionStart();
  }

  public int getSelectionEnd() throws Exception {
    return editorPane.getSelectionEnd();
  }

  /**
   * Sets the 0 based position of where the cursor is.
   *
   * @param lineNumber 1 based line number
   * @param offset
   * @throws Exception thrown on any type of error
   */
  public void setCaretPos(int lineNumber, int offset) throws Exception {
    if (lineNumber < 1) {
      throw new IllegalArgumentException("lineNumber must be 1 or greater: " + lineNumber);
    }

    BufferedReader reader = getDocumentTextReader();
    int targetOffset = offset;

    int lineCount = 1;
    String currLine = reader.readLine();
    while (currLine != null && lineCount < lineNumber) {
      targetOffset += currLine.length() + NEW_LINE_LENGTH;
      lineCount++;
      currLine = reader.readLine();
    }

    if (currLine == null) {
      if (lineCount < lineNumber) {
        throw new IllegalArgumentException(
         "lineNumber is past end of document!: " + lineNumber);
      }

      if (lineCount > 0) {
        // no new line after last line
        targetOffset--;
      }
    }
    editorPane.setCaretPosition(targetOffset);
  }

  /**
   * Sets the 0 based position of where the cursor is.
   *
   * @param pos position where the cursor is to be set in terms of total characters
   * @throws Exception thrown on any type of error
   */
  public void setCaretPos(int pos) throws Exception {
    editorPane.setCaretPosition(pos);
  }

  ////////////
  // helpers
  ////////////
  private BufferedReader getDocumentTextReader() throws BadLocationException
  {
    Document document = editorPane.getDocument();
    String text = document.getText(0, document.getLength());
    return new BufferedReader(new StringReader(text));
  }
}
