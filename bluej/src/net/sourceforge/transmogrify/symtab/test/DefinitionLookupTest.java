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
package net.sourceforge.transmogrify.symtab.test;

// $Id: DefinitionLookupTest.java 1014 2001-11-30 03:28:10Z ajp $

import junit.framework.*;
import junit.extensions.*;

import net.sourceforge.transmogrify.symtab.*;
import net.sourceforge.transmogrify.symtab.parser.*;
import java.io.*;

public class DefinitionLookupTest extends TestCase {

  private File[] _files;
  protected QueryEngine query;
  protected SymbolTable table;

  public DefinitionLookupTest( String name ) {
    super( name );
  }

  public QueryEngine createQueryEngine(File[] files) throws Exception {
    FileParser parser = null;

    _files = files;

    parser = new FileParser();
    for ( int i = 0; i < _files.length; i++ ) {
      parser.doFile( _files[i] );
    }

    TableMaker maker = new TableMaker( (SymTabAST)(parser.getTree()) );
    table = maker.getTable();

    query = new QueryEngine( table );
    return query;
  }

  protected IDefinition getDefinition( File file, String name,
                                      int lineNumber, int columnNumber ) {
    return query.getDefinition( name, new Occurrence( file, lineNumber, columnNumber ) );
  }
}
