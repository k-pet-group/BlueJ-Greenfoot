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

// $Id: Scope.java 1011 2001-11-22 10:36:26Z ajp $

import java.util.*;

import net.sourceforge.transmogrify.symtab.parser.*;

/**
 * Represents a scope of Java code.
 *
 * @author andrew mccormick, dave wood
 * @version 1.0
 * @since 1.0
 * @see Definition
 * @see Resolvable
 */
public abstract class Scope extends Definition {

  private Vector unresolvedStuff = new Vector();

  // rename to references?
  protected SortedSet referencesInScope = new TreeSet();

  protected Hashtable elements = new Hashtable();
  protected Hashtable labels = new Hashtable();
  protected Hashtable classes = new Hashtable();

  public Scope( String name, Scope parentScope, SymTabAST node ) {
    super( name, parentScope, node );
  }

  public void addDefinition(VariableDef def) {
    elements.put(def.getName(), def);
  }

  public void addDefinition(BlockDef def) {
    elements.put(def.getName(), def);
  }

  public void addDefinition(ClassDef def) {
    classes.put(def.getName(), def);
  }

  public void addDefinition(LabelDef def) {
    labels.put(def.getName(), def);
  }

  public abstract void addDefinition(IPackage def);

  protected Enumeration getDefinitions() {
    Vector allElements = new Vector();

    allElements.addAll(elements.values());
    allElements.addAll(labels.values());
    allElements.addAll(classes.values());

    return allElements.elements();
  }

  protected Iterator getClasses() {
    return classes.values().iterator();
  }

  public abstract IMethod getMethodDefinition(String name, ISignature signature);
  public abstract IVariable getVariableDefinition(String name);
  public abstract LabelDef getLabelDefinition(String name);
  public abstract IClass getClassDefinition(String name);

  public Iterator getReferencesIn() {
    return referencesInScope.iterator();
  }

  public Reference getSymbol(String name, Occurrence location) {
    Reference result = null;

    for (Iterator it = getReferencesIn(); it.hasNext(); ) {
      Reference reference = (Reference)it.next();
//      if (name.equals(reference.getName())) {
        if (reference.getLine() == location.getLine() &&
            reference.getColumn() == location.getColumn()) {
          result = reference;
          break;
//        }
      }
    }
    return result;
  }

  public void addReferenceInScope( Reference reference ) {
    referencesInScope.add( reference );
  }

}
