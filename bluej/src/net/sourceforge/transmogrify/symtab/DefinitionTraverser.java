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

// $Id: DefinitionTraverser.java 1011 2001-11-22 10:36:26Z ajp $

import antlr.*;
import antlr.collections.AST;

import net.sourceforge.transmogrify.symtab.parser.*;

import java.io.File;
import java.util.*;

public class DefinitionTraverser {

  protected SymbolTable _symbolTable;

  public DefinitionTraverser( SymbolTable symbolTable ) {
    _symbolTable = symbolTable;
  }

  public void traverse() {
    Enumeration packages = _symbolTable.getPackages().elements();
    while ( packages.hasMoreElements() ) {
      traverse( (PackageDef)(packages.nextElement()) );
    }
  }

  private void traverse( Definition def ) {
    if ( def instanceof PackageDef ) {
      traverse( (PackageDef)def );
    }
    else if (def instanceof AnonymousInnerClass) {
      traverse((AnonymousInnerClass)def);
    }
    else if ( def instanceof ClassDef ) {
      traverse( (ClassDef)def );
    }
    else if (def instanceof DefaultConstructor) {
      traverse((DefaultConstructor)def);
    }
    else if ( def instanceof MethodDef ) {
      traverse( (MethodDef)def );
    }
    else if ( def instanceof BlockDef ) {
      traverse( (BlockDef)def );
    }
    else if ( def instanceof VariableDef ) {
      traverse( (VariableDef)def );
    }
    else if ( def instanceof LabelDef ) {
      traverse( (LabelDef)def );
    }
  }

  private void traverse(PackageDef pkg) {
    handlePackage(pkg);
    traversePackage(pkg);
  }

  private void traverse(AnonymousInnerClass innerClass) {
    handleAnonymousInnerClass(innerClass);
    traverseChildren(innerClass);
  }

  private void traverse( ClassDef classDef ) {
    handleClass( classDef );
    traverseChildren( classDef );
  }

  private void traverse(DefaultConstructor constructor) {
    handleDefaultConstructor(constructor);
  }

  private void traverse( MethodDef method ) {
    handleMethod( method );
    traverseChildren( method );
  }

  private void traverse( BlockDef block ) {
    handleBlock( block );
    traverseChildren( block );
  }

  private void traverse( VariableDef variable ) {
    handleVariable( variable );
  }

  private void traverse( LabelDef label ) {
    handleLabel( label );
  }

  private void traversePackage(PackageDef pkg) {
    Iterator definitions = pkg.getClasses();
    while (definitions.hasNext()) {
      ClassDef classDef = (ClassDef)definitions.next();
      traverse(classDef);
    }
  }

  private void traverseChildren(Scope scope) {
    Enumeration definitions = scope.getDefinitions();
    while ( definitions.hasMoreElements() ) {
      Definition def = (Definition)(definitions.nextElement());
      traverse(def);
    }
  }

  protected void handlePackage( PackageDef pkg ) {}
  protected void handleAnonymousInnerClass(AnonymousInnerClass innerClass) {}
  protected void handleClass( ClassDef classDef ) {}
  protected void handleDefaultConstructor(DefaultConstructor constructor) {}
  protected void handleMethod( MethodDef method ) {}
  protected void handleBlock( BlockDef block ) {}
  protected void handleVariable( VariableDef variable ) {}
  protected void handleLabel( LabelDef label ) {}
}
