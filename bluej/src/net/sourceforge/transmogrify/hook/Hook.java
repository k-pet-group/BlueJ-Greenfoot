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

import java.io.*;
import java.util.*;

import net.sourceforge.transmogrify.symtab.*;
import net.sourceforge.transmogrify.symtab.parser.*;

//  $Id: Hook.java 1011 2001-11-22 10:36:26Z ajp $

public abstract class Hook {

  protected Set parsedFiles;

  private SymbolTable table;

  private ParseThread parseThread;
  private long wait = 5000;

  public Hook() {
    parsedFiles = new TreeSet();
  }

  public Iterator getParsedFiles() {
    return parsedFiles.iterator();
  }

  /**
   * makes an Occurrence based on the cursor position in the
   * current file
   */
  public Occurrence makeOccurrence() throws Exception {
    return new Occurrence(new File(getCurrentFile()),
              getCaretLineNumber(),
              getCaretOffset());
  }

  /**
   * Displays a list of references.
   *
   * @param references The list of references to be displayed.
   */
  public void showReferences(IDefinition definition) {
    if (definition.isSourced()) {
      showReferencesSourced(definition);
    }
    else {
      showReferencesNonsourced(definition);
    }
  }

  public abstract void showReferencesSourced(IDefinition definition);
  public abstract void showReferencesNonsourced(IDefinition defintion);

  /**
   * Sets focus to specified java source file
   * @param sourceFileName fully qualified or relative java source file name
   * @exception java.lang.Exception thrown on any type of error
   */
  public abstract void openFile(String sourceFileName)
  throws Exception;

  /**
   * Returns an array of files selected by the user
   *
   * @return the selected files
   */
  public abstract File[] getFiles()
  throws Exception;

  /**
   * Gets the file name of the currently focused file.
   * @return focused file name
   * @exception java.lang.Exception thrown on any type of error
   */
  public abstract String getCurrentFile()
  throws Exception;

  /**
   * Prompts for and returns user input
   * @return String entered from user
   * @param prompt question to ask the user
   * @param title summary of the prompt
   * @exception java.lang.Exception thrown on any type of error
   */
  public abstract String getUserInput(String prompt, String title)
  throws Exception;

  public abstract void displayMessage(String title, String message)
  throws Exception;

  public abstract void displayException(Exception e, String description);

  /**
   * Returns the entire focused source file
   * @return String of file
   * @exception java.lang.Exception thrown on any type of error
   */
  public abstract String getText()
  throws Exception;


  /**
   * Retrieves a line from the focused file.
   *
   * @param lineNumber the line you wish to retrieve
   * @exception java.lang.Exception thrown on any type of error
   */
  public abstract String getLine(int lineNumber) throws Exception;

  /**
   * Selects a bunch of text.
   * @parm startLineNumber the line number you wish to start selection on.
   * @parm startOffset the position on the line where you with selection to start.
   * @parm endLineNumber the line number you wish to end selection on.
   * @parm endOffset the position on the line where you with selection to end.
   * @exception java.lang.Exception thrown on any type of error
   */
  public abstract void selectText(
  int startLineNumber, int startOffset, int endLineNumber, int endOffset)
  throws Exception;

  /**
   * Selects a bunch of text.
   * @parm startPos the charcter you wish to start selection on
   * @parm endPos the character you wish to end selection on.
   * @exception java.lang.Exception thrown on any type of error
   */
  public abstract void selectText(int startPos, int endPos)
  throws Exception;

  /**
   * Deselects text
   * @exception java.lang.Exception thrown on any type of error
   */
  public abstract void deSelectText()
  throws Exception;

  /**
   * Returns the selected text in the form of a String
   * @return String the selected text.
   * @exception java.lang.Exception thrown on any type of error
   */
  public abstract String getSelectedText()
  throws Exception;

  /**
   * Returns the selected lines in an Enumeration
   * @return Enumeration the selected lines
   * @exception java.lang.Exception thrown on any type of error
   */
  public abstract Enumeration getSelectedLines()
  throws Exception;

  /**
   * Returns the 1 based line number of where the cursor is.
   * @return int the line number that the cursor is on.
   * @exception java.lang.Exception thrown on any type of error
   */
  public abstract int getCaretLineNumber()
  throws Exception;

  /**
   * Returns the 1 based offset of where the cursor is.
   * @return int the offset of where on the line the cursor is.
   * @exception java.lang.Exception thrown on any type of error
   */
  public abstract int getCaretOffset()
  throws Exception;

  /**
   * Returns the 0 based position of where the cursor is.
   * @return int the position of where the cursor is in terms of total characters
   * @exception java.lang.Exception thrown on any type of error
   */
  public abstract int getCaretPos()
  throws Exception;

  public abstract int getSelectionStart() throws Exception;
  public abstract int getSelectionEnd() throws Exception;

  /**
   * Sets the 0 based position of where the cursor is.
   * @parm lineNumber the line number of where the cursor is to be set.
   * @parm offset the position on the line where the cursor is to be set.
   * @exception java.lang.Exception thrown on any type of error
   */
  public abstract void setCaretPos(int lineNumber, int offset)
  throws Exception;

  /**
   * Sets the 0 based position of where the cursor is.
   * @parm pos the position where the cursor is to be set in terms of total characters.
   * @exception java.lang.Exception thrown on any type of error
   */
  public abstract void setCaretPos(int pos) throws Exception;

  public void reparseFiles() throws Exception {
    parseFiles();
  }

  public void parseFiles() throws FileNotFoundException {
    FileParser parser = new FileParser();
    Iterator it = getParsedFiles();

    try {
      while (it.hasNext()) {
        ParserFile parserFile = (ParserFile)it.next();
        parser.doFile(parserFile.getFile());
        parserFile.update(System.currentTimeMillis());
      }

      table = new TableMaker(parser.getTree()).getTable();
      spawnParseThread();
    }
    catch (ParsingException e) {
      displayException(e, "Error parsing files");
    }
    catch (SymbolTableException e) {
      displayException(e, "Error resolving symbols");
    }
  }

  public void parseFiles(File[] files) throws Exception {
    addFilesToSet(files);
    parseFiles();
  }

  private void addFilesToSet(File[] files) {
    for (int i = 0; i < files.length; i++) {
      addFileToSet(files[i]);
    }
  }

  protected void addFileToSet(File file) {
    if (file.isDirectory()) {
      addFilesToSet(file.listFiles());
    }
    else {
      if (FileParser.isJavaFile(file)) {
      parsedFiles.add(new ParserFile(file));
      }
    }
  }

  public void clearFiles() {
    parsedFiles.clear();
    killParseThread();
    table = null;
  }

  public SymbolTable getSymbolTable() {
    return table;
  }

  public Set getDirtyFiles() {
    Set dirty = new HashSet();

    for (Iterator it = getParsedFiles(); it.hasNext(); ) {
      ParserFile file = (ParserFile)it.next();
      if (file.isOutOfDate()) {
      dirty.add(file);
      }
    }

    return dirty;
  }

  class ParseThread extends Thread {
    public volatile boolean keepRunning;
    private volatile boolean notified;

    public void run() {
      keepRunning = true;
      notified = false;

      while(keepRunning) {
        synchronized(Hook.this) {
          int numDirty = 0;
          try {
            long start = System.currentTimeMillis();
            Set dirtyFiles = getDirtyFiles();
            FileParser fp = new FileParser();
            for (Iterator it = dirtyFiles.iterator(); it.hasNext(); ) {
              numDirty++;
              ParserFile file = (ParserFile)it.next();
              fp.doFile(file.getFile());
            }
            if (numDirty > 0) {
              SymTabAST newTree = ASTUtil.spliceFiles(fp.getTree(), table.getTree());
              // REDTAG -- want to be able to specify which nodes
              //   are new and therefore worth re-analyzing
              table = new TableMaker(newTree).getTable();
            }

            // update the files only after we have successfully resolved
            // the changes
            for (Iterator it = dirtyFiles.iterator();  it.hasNext(); ) {
              ParserFile file = (ParserFile)it.next();
              file.update(start);
            }

            notified = false;
            long end = System.currentTimeMillis();
            System.out.println(numDirty + " files reparsed in " + (end-start)/1000.0 + " seconds");
          }
          catch (Exception e) {
            table.expire();
            if (!notified) {
              displayException(e, "Error in parse thread:");
              notified = true;
            }
          }
        }
        try {
          Thread.sleep(wait);
        }
        catch (InterruptedException e) {}
      }
      parseThread = null;
    }
  }

  public synchronized void spawnParseThread() {
    if (parseThread == null) {
      parseThread = new ParseThread();
      parseThread.setDaemon(true);
      parseThread.setName("Hook parser thread " + Integer.toHexString(System.identityHashCode(this)));
      parseThread.setPriority(Thread.MIN_PRIORITY);
      parseThread.start();
    }
  }

  public synchronized void killParseThread() {
    if (parseThread != null) {
      parseThread.keepRunning = false;
      parseThread.interrupt();
    }
  }
}
