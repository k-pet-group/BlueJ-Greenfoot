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

public class ParserFile implements UpdateableObject, Comparable {
  private File _file;
  private long _lastParsed;

  public ParserFile(File file) {
    _file = file;
    _lastParsed = 0;
  }

  public File getFile() {
    return _file;
  }

  public void update(long lastUpdated) {
    _lastParsed = lastUpdated;
  }

  public boolean isOutOfDate() {
    return _file.lastModified() > _lastParsed;
  }

  public long lastParsed() {
    return _lastParsed;
  }

  public boolean equals(Object o) {
    boolean result = false;

    if (o instanceof ParserFile) {
      result = _file.equals(((ParserFile)o).getFile());
    }

    return result;
  }

  public int hashCode() {
    return _file.hashCode();
  }

  public int compareTo(Object o) {
    return _file.compareTo(((ParserFile)o).getFile());
  }
}