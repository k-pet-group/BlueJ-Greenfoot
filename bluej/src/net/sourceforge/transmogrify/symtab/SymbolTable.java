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
package net.sourceforge.transmogrify.symtab;

import java.io.File;
import java.util.*;

import net.sourceforge.transmogrify.symtab.parser.*;

// $Id: SymbolTable.java 1011 2001-11-22 10:36:26Z ajp $

/**
 * this class contains all of the definitions, references, and scopes
 * created by the system.
 *
 * Other stuff this class does:
 * <ul>
 * <li> holds the "base" scope containing primitive definitions
 * <li> holds the java.lang package
 * <li> holds the definition of java.lang.Object, which is the base class
 *      of all class hierarchies
 * <li> kicks off the resolve step
 * <li> does some of the work of constructing object definitions
 * </ul>
 */

public class SymbolTable implements UpdateableObject {

  private Hashtable packages = new Hashtable();
  private Stack scopes = new Stack();
  private ScopeIndex index = new ScopeIndex();

  private File currentFile;
  private int currentLine;

  private BaseScope baseScope;

  private SymTabAST root;

  private boolean outOfDate;

  /**
   * constructor takes <code>SymTabAST</code>
   * @param root root of the <code>SymTabAST</code> tree
   */
  public SymbolTable(SymTabAST root) {
    scopes = new Stack();
    this.root = root;

    baseScope = new BaseScope( this );
    pushScope(baseScope);
    outOfDate = true;
  }

  /**
   * gets the root node
   * @return <code>SymTabAST</code>
   */
  public SymTabAST getTree() {
    return root;
  }

  /**
   * sets the <code>outOfDate</code> data member to <code>true</code>
   * @return <code>void</code>
   */
  public void expire() {
    outOfDate = true;
  }

  /**
   * @return <code>boolean</code>
   */
  public boolean isOutOfDate() {
    return outOfDate;
  }

  /**
   * sets <code>outOfDate</code> member to <code>false</code>
   * @param lastUpdated
   * @return <code>void</code>
   */
  public void update(long lastUpdated) {
    outOfDate = false;
  }

  /**
   * returns the "base" scope
   *
   * @return Scope the base scope
   */
  // REDTAG -- this should eventually be replaced by a call
  //  to the lookup method that traverses scopes
  public BaseScope getBaseScope() {
    return baseScope;
  }

  /**
   * returns the current scope.  Scopes are nested in a stack (FIFO queue)
   * and pushed/popped based on the structure of the AST
   * @return <code>Scope</code>
   */
  public Scope getCurrentScope() {
    return (Scope)scopes.peek();
  }

  /**
   * pushes a new scope onto the stack
   *
   * @param scope the <code>Scope</code> to push
   * @return <code>void</code>
   */
  public void pushScope(Scope scope) {
    scopes.push(scope);
  }

  /**
   * pops a scope from the stack.
   *
   * @return <code>Scope</code>
   *
   */
  public Scope popScope() {
    Scope scope = (Scope)(scopes.pop());
    return scope;
  }

  /**
   * gets all packages stored in this symbol table
   * @return <code>Hashtable</code>
   */
  public Hashtable getPackages() {
    // REDTAG -- think about making this available as something simpler,
    //           perhaps an enumeration
    return packages;
  }

  /**
   * gets package by its name
   * @param name
   * @return <code>PackageDef</code>
   */
  public PackageDef getPackage( String name ) {
    return (PackageDef)(packages.get( name ));
  }

  /**
   * adds <code>PackageDef</code> to its parent scope and stores the
   * <code>PackageDef</code> in <code>packages</code>
   * @param pkg
   * @param parent
   * @return <code>void</code>
   */
  public void definePackage( PackageDef pkg, Scope parent ) {
    parent.addDefinition(pkg);
    packages.put(pkg.getQualifiedName(), pkg);
  }

  /**
   * defines a class in the symbol table.
   *
   * @param def the class to define
   * @return <code>void</code>
   * @see #indexScope(Scope)
   * @see #getCurrentScope()
   */
  public void defineClass(ClassDef def) {
    indexScope(def);
    getCurrentScope().addDefinition(def);
  }

  /**
   * defines a method in the symbol table
   *
   * @param method the method to be defined
   * @return <code>void</code>
   * @see #indexScope(Scope)
   * @see #getCurrentScope()
   */
  public void defineMethod(MethodDef method) {
    indexScope(method);
    ((ClassDef)getCurrentScope()).addDefinition(method);
  }

  /**
   * defines a variable in the symbol table
   *
   * @param v the variable to define
   * @return <code>void</code>
   * @see #getCurrentScope()
   */
  public void defineVariable(VariableDef v) {
    getCurrentScope().addDefinition(v);
  }

  /**
   * defines a block within the symbol table
   *
   * @param blockDef the block to define
   * @return <code>void</code>
   * @see #indexScope(Scope)
   * @see #getCurrentScope()
   */
  public void defineBlock(BlockDef blockDef) {
    indexScope(blockDef);
    getCurrentScope().addDefinition(blockDef);
  }

  /**
   * defines a label within the symbol table
   *
   * @param labelDef the label to define
   * @return <code>void</code>
   * @see #getCurrentScope()
   */
  // REDTAG -- label does not define a new scope
  public void defineLabel(LabelDef labelDef) {
    getCurrentScope().addDefinition(labelDef);
  }

  /**
   * places a scope in the symbol table's index
   *
   * @param scope the scope to index
   * @return <code>void</code>
   */
  public void indexScope(Scope scope) {
    index.addScope(scope);
  }

  /**
   * gets the symbol table's scope index
   *
   * @return ScopeIndex
   */
  public ScopeIndex getScopeIndex() {
    return index;
  }

  /**
   * sets the current file that the symbol table is processing
   *
   * @param file the <code>File</code> to use
   * @return <code>void</code>
   */
  public void setCurrentFile(File file) {
    currentFile = file;
  }

  /**
   * gets the file that the symbol table is currently processing
   *
   * @return <code>File</code>
   */
  public File getCurrentFile() {
    return currentFile;
  }

  /**
   * sets the current line that the symbol table is processing
   *
   * @param line the current line
   * @return <code>void</code>
   */
  public void setCurrentLine(int line) {
    currentLine = line;
  }

  /**
   * gets the line that the symbol table is currently processing
   *
   * @return int the line
   * @return <code>int</code>
   */
  public int getCurrentLine() {
    return currentLine;
  }

}
