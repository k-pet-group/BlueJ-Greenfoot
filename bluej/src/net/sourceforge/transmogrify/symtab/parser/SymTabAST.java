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

import java.io.File;

import net.sourceforge.transmogrify.symtab.*;
import net.sourceforge.transmogrify.symtab.printer.PrettyPrinter;

import antlr.collections.AST;
import antlr.ASTFactory;
import antlr.Token;

// $Id: SymTabAST.java 1011 2001-11-22 10:36:26Z ajp $

/**
 * an extension of <code>antlr.CommonAST</code> that includes
 * extra information about the AST's location.  This information
 * is the file and line number where the AST was created.
 *
 * To use this AST node in your tree structure, assuming your
 * antlr.TreeParser is called parser, use
 *
 * parser.setASTNOdeCLass(SymTabAST.class.getName());
 *
 * make sure you also call setTokenObjectClass for the lexer as well
 *
 * @see SymTabToken
 */
public class SymTabAST extends antlr.CommonASTWithHiddenTokens {
  private Scope _scope;
  private IDefinition _definition = null;
  private boolean _isMeaningful = true;

  private File _file;
  private int _line;
  private int _column;

  private SymTabAST parent;
  private SymTabAST prevSibling;

  private Span _span;

  private boolean dirty = false;

  /**
   * gets parent of this node
   * @return <code>SymTabAST</code>
   */
  public SymTabAST getParent() {
    return parent;
  }

  /**
   * gets previous sibling of this node
   * @return <code>SymTabAST</code>
   */
  public SymTabAST getPreviousSibling() {
    return prevSibling;
  }

  /**
   * sets parent of this node
   * @param parent
   * @return <code>void</code>
   */
  public void setParent(SymTabAST parent) {
    this.parent = parent;
  }

  /**
   * sets previous sibling of this node
   * @param prevSibling
   * @return <code>void</code>
   */
  public void setPreviousSibling(SymTabAST prevSibling) {
    this.prevSibling = prevSibling;
  }

  /**
   * gets the scope of this node
   * @return <code>Scope</code>
   */
  public Scope getScope() {
    return _scope;
  }

  /**
   * sets the scope of this node
   * @param scope
   * @return <code>void</code>
   */
  public void setScope( Scope scope ) {
    _scope = scope;
  }

  /**
   * sets <code>Definition</code> for this node
   * @param definition
   * @param scope
   * @return <code>void</code>
   * @see #setDefinition(IDefinition, Scope, boolean)
   */
  public void setDefinition(IDefinition definition, Scope scope) {
    setDefinition(definition, scope, true);
  }

  /**
   * sets <code>Definition</code> for this node and adds <code>Reference</code>
   * to the <code>_definition</code> and <code>scope</code>
   * @param definition
   * @param scope
   * @param createReference
   * @return <code>void</code>
   * @see net.sourceforge.transmogrify.symtab.Reference
   */
  public void setDefinition(IDefinition definition, Scope scope, boolean createReference) {
    _definition = definition;
    Reference reference = new Reference(this);
    if (scope != null) {
      scope.addReferenceInScope(reference);
    }

    if (definition.isSourced() && createReference) {
      _definition.addReference(reference);
    }
  }

  /**
   * gets <code>_definitin</code>
   * @return <code>IDefinition</code>
   */
  public IDefinition getDefinition() {
    return _definition;
  }

  /**
   * tests if this node is meaningful or should be ignored
   * @return <code>boolean</code>
   */
  public boolean isMeaningful() {
    return _isMeaningful;
  }

  /**
   * sets <code>_isMeaningful</code> member
   * @param isMeaningful
   * @return <code>void</code>
   */
  public void setMeaningfulness(boolean isMeaningful) {
    _isMeaningful = isMeaningful;
  }

  /**
   * sets meaningfulness for this node and its children
   * @return <code>void</code>
   * @see #setMeaningfulness(boolean)
   */
  public void ignoreChildren() {
    if (getType() == JavaTokenTypes.IDENT) {
      setMeaningfulness(false);
    }
    SymTabAST child = (SymTabAST)getFirstChild();
    while (child != null) {
      child.ignoreChildren();
      child = (SymTabAST)child.getNextSibling();
    }
  }

  /**
   * sets file where this node belong to
   * @param file
   * @return <code>void</code>
   */
  public void setFile(File file) {
    _file = file;
  }

  /**
   * finishes process for adding node to its parent
   * @param file file where this node belongs to
   * @param parent parent of this node
   * @param previousSibling previous sibling of this node
   * @return <code>Span</code> the span of this node
   * @see #setFile(File)
   * @see #setParent(SymTabAST)
   * @see #setPreviousSibling(SymTabAST)
   * @see #finishChildren(File)
   * @see #setSpan(Span)
   */
  public Span finishDefinition(File file, SymTabAST parent,
                               SymTabAST previousSibling) {
    setFile(file);
    setParent(parent);
    setPreviousSibling(previousSibling);

    Span result = finishChildren(file);

    if (getLine() != 0) {
       result.compose(new Span(getLine(),
                               getColumn(),
                               getLine(),
                               getColumn()
                               + ((getText()==null)?0:getText().length()-1)));
    }

    setSpan(result);
    return result;
  }

  /**
   * finishes children of this node definition process
   * @param file file where this node belongs to
   * @return <code>Span</code>
   * @see #finishDefinition(File, SymTabAST, SymTabAST)
   */
  public Span finishChildren(File file) {
    Span result = null;
    SymTabAST previousSibling = null;
    SymTabAST current = (SymTabAST)getFirstChild();


    if (current == null) {
      result = getSpan();
    }
    else {
      while (current != null) {
        Span childSpan = current.finishDefinition(file, this, previousSibling);

        if (childSpan != null) {
          if (result == null)  {
            result = new Span(childSpan);
          }
          else {
            result.compose(childSpan);
          }
        }

        SymTabAST temp = current;
        current = (SymTabAST)current.getNextSibling();
        previousSibling = temp;
      }
    }

    return result;
  }

  /**
   * gets file where this node belongs to
   * @return <code>File</code>
   */
  public File getFile() {
    return _file;
  }

  /**
   * sets the line where this node reside
   * @return <code>void</code>
   */
  public void setLine(int line) {
    _line = line;
  }

  /**
   * gets the line where this node reside
   * @return <code>int</code>
   */
  public int getLine() {
    return _line;
  }

  /**
   * sets the column where this node reside
   * @param column
   */
  public void setColumn(int column) {
    _column = column;
  }

  /**
   * gets the column where this node reside
   * @return <code>int</code>
   */
  public int getColumn() {
    return _column;
  }

  /**
   * gets the definition name of this node
   * @return <code>String</code>
   * @see net.sourceforge.transmogrify.symtab.IDefinition
   */
  public String getName() {
    String result = null;
    if ( _definition != null ) {
      result = _definition.getName();
    }

    return result;
  }

  /**
   * constructs a full copy of the <code>SymTabAST</code>.  i.e. a copy of
   * the AST is returned with all its children also copied, and backpointers
   * correctly constructed.
   *
   * @return <code>SymTabAST</code> a deep clone of the AST
   */
  public SymTabAST deepClone() {
    return treeCopy();
  }

  /**
   * makes a new copy of the current <code>SymTabAST</code>.  Uses the
   * initialize(AST t) method to copy properties for each cloned node.
   *
   * @return <code>SymTabAST</code> a full tree copy of the current tree
   */
  private SymTabAST treeCopy() {
    ASTFactory factory = new ASTFactory();
    factory.setASTNodeType(SymTabAST.class.getName());

    SymTabAST copyOfThis = (SymTabAST)factory.create(getType(), getText());
    copyOfThis.initialize(this);

    if (getFirstChild() != null) {
      copyOfThis.setFirstChild(((SymTabAST)getFirstChild()).treeCopy());
    }

    if (getNextSibling() != null) {
      copyOfThis.setNextSibling(((SymTabAST)getNextSibling()).treeCopy());
    }

    return copyOfThis;
  }

  /**
   * initialized this node with input node
   * @param t
   * @return <code>void</code>
   * @see #setFile(File)
   * @see #setLine(int)
   * @see #setColumn(int)
   * @see #setSpan(Span)
   * @see #setPreviousSibling(SymTabAST)
   * @see #setParent(SymTabAST)
   */
  public void initialize(AST t) {
    super.initialize(t);

    SymTabAST tree = (SymTabAST)t;
    setFile(tree.getFile());
    setLine(tree.getLine());
    setColumn(tree.getColumn());
    setSpan(tree.getSpan());
    setPreviousSibling(tree.getPreviousSibling());
    setParent(tree.getParent());
    if(tree.isDirty()) {
      dirty();
    }
  }

  /**
   * initializes the node with input <code>Token</code>
   * @param t
   * @return <code>void</code>
   * @see #setFile(File)
   * @see #setLine(int)
   * @see #setColumn(int)
   * @see #setSpan(Span)
   * @see net.sourceforge.transmogrify.symtab.parser.Span
   */
  public void initialize(Token t) {
    super.initialize(t);

    // we can assume this cast as long as the lines
    //  lexer.setTokenObjectClass(SymTabToken.class.getName());
    // and
    //  parser.setASTNodeClass(SymTabAST.class.getName());
    // appear in our processing function(s)
    SymTabToken tok = (SymTabToken)t;
    setFile(tok.getFile());
    setLine(tok.getLine());
    setColumn(tok.getColumn());

    if ( (getColumn() != 0) && (getText() != null) ) {
      setSpan( new Span( getLine(), getColumn(), getLine(),
                         getColumn() + getText().length() - 1 ) );
    }
  }

  /**
   * print this node to <code>PrettyPrinter</code>
   * @param printer
   * @return <code>void</code>
   */
  public void report(PrettyPrinter printer) {
    String text = ASTUtil.constructDottedName(this);
    printer.indent();
    printer.println(text + "@" + getFile().getAbsolutePath() + ":" + getLine());
    printer.unindent();
  }

  /**
   * prints the line, column and file for this node for debugging purpose
   * @return <code>String</code>
   */
  public String toString() {
    StringBuffer resultBuffer = new StringBuffer(super.toString());
    resultBuffer.append( "[" + getLine() + "," + getColumn() + "]" );
    //if ( getSpan() != null ) {
    //  resultBuffer.append( " spans " + getSpan() );
    //}
    resultBuffer.append(" in " + getFile());
    //resultBuffer.append(" type: " + getType());
    return resultBuffer.toString();
  }

  /**
   * gets <code>Span</code> of this node
   * @return <code>Span</code>
   */
  public Span getSpan() {
    return _span;
  }

  /**
   * sets <code>Span</code> for this node
   * @param span
   * @return <code>void</code>
   */
  public void setSpan( Span span ) {
    _span = span;
  }

  /**
   * tests if this node is inside the span
   * @param line
   * @param column
   * @return <code>boolean</code> <code>true</code> if this node is within the span
   *                              <code>false</code> otherwise
   */
  public boolean contains(int line, int column) {
    return getSpan().contains(line, column);
  }

  /**
   * gets enclosing node for this node based on line and column
   * @param line
   * @param column
   * @return <code>SymTabAST</code>
   * @see #getSpan()
   */
  public SymTabAST getEnclosingNode(int line, int column) {
    SymTabAST result = null;

    if ( (getSpan() != null) && (getSpan().contains(line, column)) ) {
      SymTabAST child = (SymTabAST)getFirstChild();
      while ( child != null && result == null ) {
        result = child.getEnclosingNode(line, column);
        child = (SymTabAST)child.getNextSibling();
      }

      // if none of the children contain it, I'm the best node
      if ( result == null ) {
        result = this;
      }

    }

    return result;
  }

  /**
   * adds child node to the first child of this node
   * @param child
   * @return <code>void</code>
   */
  public void addFirstChild(SymTabAST child) {
    SymTabAST previousFirstChild = (SymTabAST)getFirstChild();

    setFirstChild(child);
    child.setNextSibling(previousFirstChild);
    child.setParent(this);
    child.setPreviousSibling(null);

    if (previousFirstChild != null) {
      previousFirstChild.setPreviousSibling(child);
    }
  }

  /**
   * replaces one child to a new child of this node
   * @param oldChild node to be replaced
   * @param newChild node for replacement
   * @return <code>void</code>
   */
  public void replaceChild(SymTabAST oldChild, SymTabAST newChild) {
    SymTabAST prevSibling = oldChild.getPreviousSibling();
    SymTabAST nextSibling = (SymTabAST)oldChild.getNextSibling();

    if (prevSibling != null) {
      prevSibling.setNextSibling(newChild);
      newChild.setPreviousSibling(prevSibling);
    }
    else {
      setFirstChild(newChild);
    }

    newChild.setParent(this);
    if (nextSibling != null) {
      newChild.setNextSibling(nextSibling);
      nextSibling.setPreviousSibling(newChild);
    }

  }

  /**
   * gets first occurence of the child node with a certain type
   * @param type
   * @return <code>SymTabAST</code>
   * @see #getType()
   */
  public SymTabAST getFirstChildOfType(int type) {
    SymTabAST result = null;

    AST sibling = getFirstChild();
    while (sibling != null) {
      if (sibling.getType() == type) {
        result = (SymTabAST)sibling;
        break;
      }
      sibling = sibling.getNextSibling();
    }

    return result;
  }

  /**
   * append a node before this node
   * @param node node to be appended
   * @return <code>void</code>
   */
  public void prepend(SymTabAST node) {
    node.setPreviousSibling(getPreviousSibling());
    if (getPreviousSibling() != null) {
      getPreviousSibling().setNextSibling(node);
    }

    node.setNextSibling(this);
    setPreviousSibling(node);

    node.setParent(getParent());
    if (getParent().getFirstChild() == this) {
      getParent().setFirstChild(node);
    }
  }

  /**
   * adds a node to the last position of this node children
   * @param child
   * @return <code>void</code>
   */
  public void addChild(SymTabAST child) {
    SymTabAST lastChild = (SymTabAST)getFirstChild();
    if (lastChild == null) {
      setFirstChild(child);
      child.setParent(this);
      child.setNextSibling(null);
      child.setPreviousSibling(null);
    }
    else {
      while (lastChild.getNextSibling() != null) {
        lastChild = (SymTabAST)lastChild.getNextSibling();
      }
      lastChild.setNextSibling(child);
      child.setPreviousSibling(lastChild);
      child.setNextSibling(null);
      child.setParent(this);
    }
  }

  /**
   * gets Iterator for this node
   * @return <code>SymTabASTIterator</code>
   */
  public SymTabASTIterator getChildren() {
    return new SymTabASTIterator(this);
  }

  public boolean isDirty() {
    return dirty;
  }

  public void dirty() {
    dirty = true;
  }
}