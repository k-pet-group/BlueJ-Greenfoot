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

import java.util.*;

import antlr.collections.AST;

import net.sourceforge.transmogrify.symtab.parser.*;

// $Id: ClassDef.java 1011 2001-11-22 10:36:26Z ajp $

/**
 * <code>ClassDef</code> contains all the information needed to
 * represent a java class or interface.  This includes the superclass,
 * whether it's a class or an interface, the interfaces it implements,
 * a list of its (direct?) subclasses, and the classes that implement
 * it if it is an interface
 *
 * @see Scope
 */
public class ClassDef extends DefaultScope implements IClass {
  private long id = 0;

  private IClass superclass = null;
  private List interfaces = new Vector();

  private List subclasses = new Vector();
  private List implementors = new Vector();

  private Set importedPackages = new HashSet();

  // variable definitions will use elements from parent
  private Set methods = new HashSet();

  private Hashtable imports = new Hashtable();
  private Vector unprocessedImports = null;

  protected MethodDef _defaultConstructor;

  public ClassDef(String name, Scope parentScope, SymTabAST node) {
    super(name, parentScope, node);
    _defaultConstructor = new DefaultConstructor(this);
    addDefinition(_defaultConstructor);
  }

  public long getNextAnonymousId() {
    return ++id;
  }

  public void setSuperclass( IClass superclass ) {
    this.superclass = superclass;
  }

  public IClass getSuperclass() {
    return superclass;
  }

  public void addUnprocessedImports( Vector imports ) {
    unprocessedImports = (Vector)(imports.clone());
  }

  public Vector getUnprocessedImports() {
    return unprocessedImports;
  }

  public void importPackage(IPackage pkg) {
    importedPackages.add(pkg);
  }

  public void importClass(IClass imported) {
    imports.put( imported.getName(), imported );
  }

  // begin definitions interface

  public void addDefinition(MethodDef method) {
    if (method.getName().equals(getName())) {
      methods.remove(_defaultConstructor);
    }
    methods.add(method);
  }

  protected Enumeration getDefinitions() {
    Vector allElements = new Vector();

    allElements.addAll(elements.values());
    allElements.addAll(methods);
    allElements.addAll(labels.values());
    allElements.addAll(classes.values());

    return allElements.elements();
  }

  public IClass getClassDefinition(String name) {
    IClass result = null;

    result = (ClassDef)(classes.get(name));

    if ( result == null ) {
      result = (IClass)(imports.get(name));
    }

    if (result == null) {
      Iterator it = importedPackages.iterator();
      while (it.hasNext() && result == null) {
        IPackage pkg = (IPackage)it.next();
        result = pkg.getClass(name);
      }
    }

    if ( result == null ) {
      result = getParentScope().getClassDefinition( name );
    }

    return result;
  }

  public IMethod getMethodDefinition(String name,
                                     ISignature signature) {
    IMethod result = null;

    result = getDeclaredMethod(name, signature);

    if (result == null) {
      result = getMostCompatibleMethod(name, signature);
    }

    if ( result == null ) {
      if ( superclass != null ) {
        result = superclass.getMethodDefinition( name, signature );
      }
    }

    if (result == null){
        IClass[] interfaces = getInterfaces();
        for (int index = 0; index < interfaces.length && result == null; index++){
            result = interfaces[index].getMethodDefinition(name, signature);
        }
    }

    // not sure why this is here -- inner classes, maybe?
    // regardless, write better
    if ( result == null ) {
      if ( getParentScope() != null ) {
        result = getParentScope().getMethodDefinition( name, signature );
      }
    }


    return result;
  }

  public IMethod getMostCompatibleMethod(String name, ISignature signature) {
    IMethod result = null;

    SortedSet compatibleMethods
      = new TreeSet(new MethodSpecificityComparator());

    Iterator it = methods.iterator();
    while (it.hasNext()) {
      MethodDef method = (MethodDef)it.next();
      if ( name.equals( method.getName() ) ) {
        if ( method.hasCompatibleSignature( signature ) ) {
          compatibleMethods.add(method);
        }
      }
    }

    if (!compatibleMethods.isEmpty()) {
      result = (IMethod)compatibleMethods.first();
    }

    return result;
  }

  public IMethod getDeclaredMethod(String name, ISignature signature) {
    // finds methods declared by this class with the given signature

    IMethod result = null;

    Iterator it = methods.iterator();
    while (it.hasNext()) {
      MethodDef method = (MethodDef)it.next();
      if ( name.equals( method.getName() ) ) {
        if ( method.hasSameSignature( signature ) ) {
          result = method;
          break;
        }
      }
    }

    return result;
  }

  public IVariable getVariableDefinition(String name) {
    IVariable result = null;

    // in keeping with getField in java.lang.Class
    // 1) current class
    // 2) direct superinterfaces
    // 3) superclass
    // then we do the parent scope in case its an inner class

    result = (VariableDef)(elements.get( name ));

    if (result == null) {
      IClass[] superinterfaces = getInterfaces();
      for (int i = 0; i < superinterfaces.length && result == null; i++) {
        result = superinterfaces[i].getVariableDefinition(name);
      }
    }

    if ( result == null ) {
      if ( superclass != null ) {
        result = superclass.getVariableDefinition( name );
      }
    }

    if ( result == null ) {
      if ( getParentScope() != null ) {
        result = getParentScope().getVariableDefinition( name );
      }
    }

    return result;
  }

  // end definitions interface

  public void addInterface(IClass implemented) {
    interfaces.add( implemented );
  }

  public IClass[] getInterfaces() {
    IClass[] type = new IClass[0];
    return (IClass[])interfaces.toArray(type);
  }

  public ClassDef getEnclosingClass() {
    return this;
  }

  public void addSubclass(ClassDef subclass) {
    subclasses.add(subclass);
  }

  public List getSubclasses() {
    return subclasses;
  }

  public void addImplementor(ClassDef implementor) {
    implementors.add(implementor);
  }

  public List getImplementors() {
    return implementors;
  }

  public IClass[] getInnerClasses() {
    Iterator it = getClasses();
    List result = new ArrayList();

    while(it.hasNext()) {
      result.add(it.next());
    }

    return (IClass[])result.toArray(new IClass[0]);
  }

  public boolean isSuperclassOf(IClass possibleChild) {
    // justify my existence
    boolean result = subclasses.contains(possibleChild);

    /*
    Iterator it = subclasses.iterator();
    while (it.hasNext() && !result) {
      IClass child = (IClass)it.next();
      result = child.isSuperclassOf(possibleChild);
    }
    */
    return result;
  }

  public boolean isCompatibleWith(IClass type) {
    boolean result = false;

    // check myself
    if (type.equals(this)) {
      result = true;
    }
    // check my superclass
    else if (superclass != null && superclass.isCompatibleWith(type)) {
      result = true;
    }
    // check my interfaces
    else if (!interfaces.isEmpty()) {
      Iterator it = interfaces.iterator();

      while(it.hasNext() && !result) {
        IClass current = (IClass)it.next();

        if (current.isCompatibleWith(type)) {
          result = true;
        }
      }
    }

    return result;
  }

  public boolean isPrimitive() {
    return false;
  }

  public void addNewVariable(SymTabAST variableDef) {
    SymTabAST objblock = getObjblock();
    objblock.addFirstChild(variableDef);
  }

  private SymTabAST getObjblock() {
    return getTreeNode().getFirstChildOfType(JavaTokenTypes.OBJBLOCK);
  }

}
