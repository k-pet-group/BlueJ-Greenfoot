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

import java.io.*;
import java.util.*;

import net.sourceforge.transmogrify.symtab.parser.*;

/**
 * This class is for counting the reference
 */
public class ReferenceCounter extends ReferenceTool {
  private List _references = new ArrayList();
  private List _resolvedReferences = new ArrayList();
  private List _unresolvedReferences = new ArrayList();

  public ReferenceCounter(SymbolTable table) {
    super(table.getTree());
    countReferences();
  }

  /**
   * Return the number of references
   */
  public int numberOfReferences() {
    return _references.size();
  }

  /**
   * Return the number of resolved references
   */
  public int numberOfResolvedReferences() {
    return _resolvedReferences.size();
  }

  public int numberOfUnresolvedReferences() {
    return _unresolvedReferences.size();
  }

  public ListIterator getUnresolvedReferences() {
    return _unresolvedReferences.listIterator();
  }

  private void countReferences() {
    handleNode( _tree );
  }

  protected void handleNode( SymTabAST node ) {
    if (node.getType() == JavaTokenTypes.IDENT && node.isMeaningful()) {
      _references.add( node );
      if (node.getDefinition() != null && !(node.getDefinition() instanceof UnknownClass)) {
            _resolvedReferences.add( node );
      }
      else {
            _unresolvedReferences.add( node );
      }
    }
    walkChildren( node );
  }

  public static void main( String[] args ) {
    FileParser parser = new FileParser();
    int firstFile = 0;
    boolean showUnresolved = false;

    if ( args[0].equals( "--verbose" ) ) {
      firstFile = 1;
      showUnresolved = true;
    }

    try {

      for ( int i = firstFile; i < args.length; i++ ) {
            parser.doFile( new File( args[i] ) );
      }
      SymTabAST tree = (SymTabAST)parser.getTree();
      SymbolTable table = new TableMaker( tree ).getTable();
      ReferenceCounter counter = new ReferenceCounter( table );
      System.out.println( "References found:      "
                                              + counter.numberOfReferences() );
      System.out.println( "References resolved:   "
                                              + counter.numberOfResolvedReferences() );
      System.out.println( "References unresolved: "
                                              + counter.numberOfUnresolvedReferences() );

      if ( showUnresolved ) {
            Iterator it = counter.getUnresolvedReferences();
            while ( it.hasNext() ) {
              System.out.println( "  " + it.next() );
            }
      }

    }
    catch ( Exception e ) {
      e.printStackTrace();
    }
  }
}
