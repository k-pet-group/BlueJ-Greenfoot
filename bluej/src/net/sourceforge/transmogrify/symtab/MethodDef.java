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

// $Id: MethodDef.java 1011 2001-11-22 10:36:26Z ajp $

import java.util.*;

import net.sourceforge.transmogrify.symtab.parser.*;

import antlr.collections.AST;

/**
 * <code>MethodDef</code> contains all the pertinent information for
 * a method, including return type, formal parameters, and exceptions
 * thrown
 *
 * @see ClassDef
 * @see MethodSignature
 */
public class MethodDef extends DefaultScope implements IMethod {

  private IClass returnType;
  private ISignature signature;
  private List exceptions;

  private List parameters;

  public MethodDef(String name, Scope parentScope, SymTabAST node) {
    super(name, parentScope, node);
    parameters = new Vector();
  }

  /**
   * Returns the <code>ClassDef</code> for the return type of this method.
   *
   * @return the <code>ClassDef</code> for the return type of this method
   */
  public IClass getType() {
    return returnType;
  }

  /**
   * Sets the return type of this method.
   *
   * @param type the <code>ClassDef</code> for the return type
   */
  public void setType(IClass type) {
    returnType = type;
  }

  /**
   * Adds a parameter to the collection of formal parameters
   *
   * @param parameter the <code>VariableDef</code> to add
   */
  public void addParameter(VariableDef parameter) {
    parameters.add( parameter );
    addDefinition(parameter);
  }

  /**
   * Whether this method has the same signature as the given signature.
   *
   * @param signature the <code>MethodSignature</code> to compare
   *
   * @return whether the signatures are equal
   */
  public boolean hasSameSignature(ISignature signature) {
    return getSignature().equals(signature);
  }

  /**
   * Whether this method has a signature compatible with the given signature.
   *
   * @param signature the signature being compared
   * @return whether the signatures are compatible
   */
  public boolean hasCompatibleSignature(ISignature signature) {
    return signature.isCompatibleWith(getSignature());
  }

  /**
   * Returns the signature of this method.
   *
   * @return the signature of this method
   */
  public ISignature getSignature() {
    Vector argTypes = new Vector();

    for (int i = 0; i < parameters.size(); i++) {
      argTypes.add(getParameterAt(i).getType());
    }

    return new MethodSignature(argTypes);
  }

  /**
   * Gets the <i>i</i>th parameter of this method
   *
   * @param i the index of the parameter
   *
   * @return the <code>VariableDef</code> of the <i>i</i>th parameter
   */
  private VariableDef getParameterAt( int i ) {
    return (VariableDef)(parameters.get( i ));
  }

  /**
   * Adds an exception that this method throws.
   *
   * @param exception the exception to add
   */
  public void addException(IClass exception) {
    if (exceptions == null) {
      exceptions = new Vector();
    }

    exceptions.add(exception);
  }

  /**
   * Returns the exceptions this method throws
   *
   * @return the exceptions this method throws
   */
  public IClass[] getExceptions() {
    return (IClass[])exceptions.toArray(new IClass[0]);
  }

  /**
   * Returns the parameter of the given name.
   *
   * @param name the name of the parameter to retrieve
   *
   * @return the parameter of the given name
   */
  private VariableDef getParameterByName(String name) {
    VariableDef result = null;

    for ( int i = 0; i < parameters.size(); i++ ) {
      if ( name.equals( getParameterAt(i).getName() ) ) {
        result = getParameterAt(i);
      }
    }

    return result;
  }

  public String getQualifiedName() {
    return super.getQualifiedName() + getSignature();
  }
}