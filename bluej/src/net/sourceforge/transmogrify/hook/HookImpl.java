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
package net.sourceforge.transmogrify.hook;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;

import java.util.Enumeration;
import java.util.Vector;

import net.sourceforge.transmogrify.symtab.IDefinition;

import java.util.Collection;

public class HookImpl extends Hook {
  private File file;
  private int caretOffset;

  private int line;
  private int column;

  private int startSelection;
  private int endSelection;

  private String userInput;


  public void showReferencesSourced(IDefinition definition) {}
  public void showReferencesNonsourced(IDefinition defintion) {}

  public void openFile(String sourceFileName) {
    file = new File(sourceFileName);
  }

  public File[] getFiles() { return new File[0]; }

  public String getCurrentFile() { return file.getPath(); }

  public void setUserInput(String input) {
    userInput = input;
  }

  public String getUserInput(String prompt, String title) { return userInput; }
  public void displayMessage(String title, String message) {}

  public void displayException(Exception e, String description) {}

  public String getText() { return ""; }
  public String getText(int startPos, int endPos) { return ""; }
  public void insertText(int pos, String text) {}
  public void deleteText(int startPos, int endPos) {}
  public int getNumChars() { return 0; }

  public String getLine(int lineNumber) { return ""; }
  public void setLine(int lineNumber, String line) {}
  public void insertLine(int lineNumber, String line) {}
  public int getNumLines() { return 0;}

  public void selectText(int startLineNumber, int startOffset,
             int endLineNumber, int endOffset) {
    startSelection = calculateOffset(startLineNumber, startOffset);
    endSelection = calculateOffset(endLineNumber, endOffset);
  }


  public void selectText(int startPos, int endPos) {
    startSelection = startPos;
    endSelection = endPos;
  }

  public void deSelectText() {}

  public String getSelectedText() {
    String result = null;

    try {
      InputStream is = new FileInputStream(file);

      byte[] buf = new byte[endSelection-startSelection];

      is.skip(startSelection);
      is.read(buf);

      result = new String(buf);
    }
    catch (Exception e) {
      e.printStackTrace();
    }

    return result;
  }

  public Enumeration getSelectedLines() { return new Vector().elements(); }

  public int getCaretLineNumber() { return line; }
  public int getCaretOffset() { return column; }
  public int getCaretPos() { return caretOffset; }

  public int getSelectionStart() {
    return startSelection;
  }

  public int getSelectionEnd() {
    return endSelection;
  }

  public void setCaretPos(int lineNumber, int offset) {
    caretOffset = calculateOffset(lineNumber, offset);
  }

  private int calculateOffset(int lineNumber, int offset) {
    int result = 0;

    try{
      InputStream stream = new FileInputStream(file);
      int current;
      result = offset-1;
      while((current = stream.read()) > -1 && lineNumber > 1){
        result++;
        if (current == '\n'){
          lineNumber--;
        }
      }
    }catch(IOException err){
      err.printStackTrace();
    }

    return result;
  }

  private void calculateLineAndColumn(int offset) {
    int lineNumber = 1;
    int column = 1;
    int current;

    int total = 0;

    try{
      InputStream stream = new FileInputStream(file);
      while((current = stream.read()) > -1 && total < offset) {
        column++;
        total++;
        if (current == '\n'){
          lineNumber++;
          column = 1;
        }
      }
    } catch(IOException err){
      err.printStackTrace();
    }

    line = lineNumber;
    this.column = column;
  }

  public void setCaretPos(int pos) {
    caretOffset = pos;
    calculateLineAndColumn(pos);
  }



  /**
   * Insert the method's description here.
   * Creation date: (4/25/01 1:22:10 PM)
   * @param parsedFile java.io.File
   */
  protected void collectionParsed(Collection col) {}
}