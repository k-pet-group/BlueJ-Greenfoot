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

// $Id: ExternalClass.java 1011 2001-11-22 10:36:26Z ajp $

import java.lang.reflect.*;
import java.util.*;

import net.sourceforge.transmogrify.symtab.parser.DotUtils;

public class ExternalClass extends ExternalDefinition implements IClass {
  private Class _javaClass;

  private List _subclasses;
  private List _implementors;

  private Set _methods;

  public ExternalClass(Class javaClass) {
    _javaClass = javaClass;
    _subclasses = new ArrayList();
    _implementors = new ArrayList();
  }

  public Class getJavaClass() {
    return _javaClass;
  }

  public String getName() {
    return getLocalName();
  }

  private String getLocalName() {
    String fullName = _javaClass.getName();
    return fullName.substring(fullName.lastIndexOf(".") + 1);
  }

  public IClass getSuperclass() {
    IClass result = null;
    Class javaSuperclass = _javaClass.getSuperclass();

    if (javaSuperclass != null) {
      result = new ExternalClass(javaSuperclass);
    }
    else {
      if (_javaClass.isInterface()) {
        // according to Java, java.lang.Object is not the superclass of
        // interfaces, which makes sense.  However, we need Object to be
        // the superclass of everything to make type compatibility work.
        result = new ExternalClass(Object.class);
      }
    }

    return result;
  }

  public IClass[] getInterfaces() {
    Class[] javaInterfaces = _javaClass.getInterfaces();

    IClass[] result = new IClass[javaInterfaces.length];

    // should we cache this?
    for (int i = 0; i < javaInterfaces.length; i++) {
      result[i] = new ExternalClass(javaInterfaces[i]);
    }

    return result;
  }

  public IClass getClassDefinition(String name) {
    IClass result = null;

    String qualifiedName = getQualifiedName() + "$" + name;

    Class[] classes = getJavaClass().getClasses();
    for (int i = 0; i < classes.length; i++) {
      String candidateQualifiedName = classes[i].getName();

      if (qualifiedName.equals(candidateQualifiedName)) {
        result = new ExternalClass(classes[i]);
        break;
      }
    }

    return result;
  }

  // REDTAG -- this should be a template method!
  public IVariable getVariableDefinition(String name) {
    IVariable result = null;
    Field javaField = null;

    try {
      javaField = _javaClass.getDeclaredField(name);
    }
    catch (NoSuchFieldException ignoreMe) {}

    if (javaField == null) {
      Class[] interfaces = _javaClass.getInterfaces();
      for (int i = 0; i < interfaces.length && javaField == null; i++) {
        try {
          javaField = interfaces[i].getDeclaredField(name);
        }
        catch (NoSuchFieldException ignoreMe) {}
      }
    }

    if (javaField != null) {
      result = new ExternalVariable(javaField);
    }
    else {
      if (getSuperclass() != null) {
        result = getSuperclass().getVariableDefinition(name);
      }
    }

    return result;
  }

  public IMethod getMethodDefinition(String name,
                                     ISignature signature) {
    IMethod result = null;

    if (name.equals(getName())) {
      result = getConstructorDefinition(signature);
    }
    else {
      Method[] methods = _javaClass.getDeclaredMethods();

      for (int i = 0; i < methods.length; i++) {
        if (name.equals(methods[i].getName())) {
          IMethod method = new ExternalMethod(methods[i]);
          if (method.hasSameSignature(signature)) {
            result = method;
            break;
          }
        }
      }

      if (result == null) {
        result = getMostCompatibleMethod(name, signature);
      }

      if (result == null) {
        if (getSuperclass() != null) {
          result = getSuperclass().getMethodDefinition(name, signature);
        }
      }

      if (result == null) {
        IClass[] interfaces = getInterfaces();
        for (int i = 0; i < interfaces.length; i++) {
          result = interfaces[i].getMethodDefinition(name, signature);

          if (result != null) {
            break;
          }

        }
      }
    }

    return result;
  }

  public IMethod getMostCompatibleMethod(String name, ISignature signature) {
    IMethod result = null;

    SortedSet compatibleMethods
      = new TreeSet(new MethodSpecificityComparator());

    Iterator it = getMethods().iterator();
    while (it.hasNext()) {
      IMethod method = (IMethod)it.next();
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

  private Collection getMethods() {
    if (_methods == null) {
      _methods = new HashSet();

      Method[] methods = _javaClass.getDeclaredMethods();
      for (int i = 0; i < methods.length; i++) {
        _methods.add(new ExternalMethod(methods[i]));
      }

    }

    return _methods;
  }

  public IMethod getConstructorDefinition(ISignature signature) {
    IMethod result = null;

    if (_javaClass.isInterface()) {
      result = getInterfaceConstructor(signature);
    }
    else {
      result = getClassConstructor(signature);
    }

    return result;
  }

  private IMethod getInterfaceConstructor(ISignature signature) {
    IMethod result = null;

    if (signature.getParameters().length == 0) {
      result = new InterfaceConstructor(_javaClass);
    }

    return result;
  }

  private IMethod getClassConstructor(ISignature signature) {
    IMethod result = null;

    Constructor[] constructors = _javaClass.getConstructors();

    for (int i = 0; i < constructors.length; i++) {
      IMethod constructor = new ExternalConstructor(constructors[i]);
      if (constructor.hasSameSignature(signature)) {
        result = constructor;
        break;
      }
    }

    if (result == null) {
      for (int i = 0; i < constructors.length; i++) {
        IMethod constructor = new ExternalConstructor(constructors[i]);
        if (constructor.hasCompatibleSignature(signature)) {
          result = constructor;
          break;
        }
      }
    }

    return result;
  }

  public boolean isCompatibleWith(IClass type) {
    boolean result = false;

    if (isPrimitive() && type.isPrimitive()) {
      result = PrimitiveClasses.typesAreCompatible((ExternalClass)type,
                                                     this);
    }
    else {
      // check myself
      if (type.equals(this)) {
        result = true;
      }
      // check my superclass
      else if (getSuperclass() != null && getSuperclass().isCompatibleWith(type)) {
        result = true;
      }
      // check my interfaces
      else if (_javaClass.getInterfaces().length > 0) {
        Class[] interfaces = _javaClass.getInterfaces();
        for (int i = 0; i < interfaces.length; i++) {
          if (new ExternalClass(interfaces[i]).isCompatibleWith(type)) {
            result = true;
            break;
          }
        }
      }
    }

    return result;
  }

  private boolean isPrimitiveCompatibleWith(IClass type) {
    boolean result = false;

    return result;
  }

  public void addImplementor(ClassDef implementor) {
    _implementors.add(implementor);
  }

  public List getImplementors() {
    return _implementors;
  }

  public void addSubclass(ClassDef subclass) {
    _subclasses.add(subclass);
  }

  public List getSubclasses() {
    return _subclasses;
  }

  public String getQualifiedName() {
    return _javaClass.getName();
  }

  public boolean isPrimitive() {
    return getJavaClass().isPrimitive();
  }

  public IClass[] getInnerClasses() {
    Class[] innerJavaClasses = getJavaClass().getDeclaredClasses();
    IClass[] result = new IClass[innerJavaClasses.length];

    for (int i = 0; i < result.length; i++) {
      result[i] = new ExternalClass(innerJavaClasses[i]);
    }

    return result;
  }

  public String toString() {
     return getClass() + "[" + getQualifiedName() + "]";
   }

  public boolean equals(Object o) {
    boolean result = false;

    if (o instanceof ExternalClass) {
      ExternalClass compared = (ExternalClass)o;
      result = (getJavaClass().equals(compared.getJavaClass()));
    }

    return result;
  }

  public int hashCode() {
    return getJavaClass().hashCode();
  }

}
