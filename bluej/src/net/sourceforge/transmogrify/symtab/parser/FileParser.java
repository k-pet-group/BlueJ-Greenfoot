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
package net.sourceforge.transmogrify.symtab.parser;

import java.io.*;
import antlr.*;
import antlr.collections.*;

// $Id: FileParser.java 1011 2001-11-22 10:36:26Z ajp $

/**
 * Title:        No Pants Refactorer
 * Description:
 * Copyright:    Copyright (c) 2000
 * Company:      ThoughtWorks
 * @author     A Team with No Pants
 * @version 1.0
 */


public class FileParser {
  private ASTFactory factory;
  private SymTabAST root;

  private long delta;

  public FileParser() {
    delta = 0;
    setupTree();
  }

/**
Returns the AST of the current file
@return AST the top node of the AST of all files parsed.
*/
  public SymTabAST getTree() {
    System.out.println("parsing took " + getParseSeconds() + " seconds");
    return root;
  }

  public double getParseSeconds() {
    return delta / 1000.0;
  }

/**
Creates a new AST root node.
*/
  private void setupTree() {
    factory = new ASTFactory();
    factory.setASTNodeType(SymTabAST.class.getName());
    root = (SymTabAST)factory.create(0, "AST Root");
  }

/**
Takes and AST root and a bunch of files and parses them into a single AST
@param File the file you wish to parse
@param String an array of files
@exception FileNotFoundException thrown if root of an invalid type
*/
  public void parseFiles(File root,
                         String [] files) throws FileNotFoundException,
                                                 ParsingException {
    for (int i = 0; i < files.length; i++) {
      doFile(new File(root, files[i]));
    }
  }

/**
Takes a File or directory and either traverses the directory or calls parseFile on the file.
@param File to be sent to parseFile or the directory to be traversed.
@exception FileNotFoundException thrown on invalid type.
*/
  public void doFile(File file) throws FileNotFoundException,
                                       ParsingException {
    if (file.isDirectory()) {
      parseFiles(file, file.list());
    }
    else if (isJavaFile(file)) {
      //System.out.println("   "+file.getAbsolutePath());
      parseFile( file );
    }
  }

/**
Attempts to Lex and parse a given file
@param String the absolute path of the file to be written to
@param InputStream the input to be turned into a file
@exception parser exception on inability to Lex/Parse input.
*/
  public void parseFile(File file) throws FileNotFoundException,
                                          ParsingException {
    try {
      long deltaStart = System.currentTimeMillis();
      String path = file.getAbsolutePath();
      FileInputStream stream = new FileInputStream(file);
      JavaLexer lexer = makeLexer(stream, path);

      TokenStreamHiddenTokenFilter filter;
      filter = new TokenStreamHiddenTokenFilter(lexer);
      filter.hide(JavaRecognizer.SL_COMMENT);
      filter.hide(JavaRecognizer.ML_COMMENT);

      JavaRecognizer parser = makeRecognizer(filter, path);
      parser.compilationUnit();
      doTreeAction( file, parser.getAST(), parser.getTokenNames());
      long deltaEnd = System.currentTimeMillis();

      delta += (deltaEnd - deltaStart);
    }
    catch (ANTLRException e) {
      throw new ParsingException(e.getMessage());
    }
  }

/**
Constructor, creates a new JavaLexer object based input stream.
@param Inputstream the input to be broken up
@param String the absoulute path of the file
@return JavaLexer object called result
*/
  private JavaLexer makeLexer(InputStream stream, String path) {
    SymTabLexer result = new SymTabLexer(stream);
    result.setFilename(path);
    result.setTokenObjectClass(SymTabToken.class.getName());

    return result;
  }

/**
Constructor, creates a new java Recognizer object based on a Lexer object
@param JavaLexer object to be worked on
@param String the absolute path of the file
@return JavaRecognizer object called result
*/
  private JavaRecognizer makeRecognizer(TokenStreamHiddenTokenFilter filter,
                                        String path) {
    JavaRecognizer result = new JavaRecognizer(filter);
    result.setFilename(path);
    result.setASTNodeClass(SymTabAST.class.getName());

    return result;
  }

/**
Boolean opperation that checks to see if a file is of type .java
@param File the file you wish to check
@return boolean
*/
  public static boolean isJavaFile(File file) {
    return ( (file.getName().length()>5) &&
             (file.getName().substring(file.getName().length()-5).equals(".java")) );
  }

/**
Processes a Tree to be modified
@param String the name of the file
@param AST the tree to be worked on
@param String an array of strings
*/
  private void doTreeAction(File file, AST tree, String[] tokenNames) {
    if (tree != null) {
      addToTree(file, tree, tokenNames);
    }
  }


/**
Adds a file to an AST tree
@param String the file you wish to add
@param AST the tree you are adding the file to
@param String the token names
*/
  private void addToTree(File file, AST tree, String[] tokenNames) {
    ((CommonAST)tree).setVerboseStringConversion(true, tokenNames);
    SymTabAST node = (SymTabAST)factory.create(0,file.getAbsolutePath());

    node.setFirstChild(tree);
    node.finishDefinition(file, root, null);
    root.addChild(node);
  }
}
