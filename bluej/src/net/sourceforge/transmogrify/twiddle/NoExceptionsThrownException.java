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

// $Id: NoExceptionsThrownException.java 1011 2001-11-22 10:36:26Z ajp $
package net.sourceforge.transmogrify.twiddle;

public class NoExceptionsThrownException extends Exception {
  public NoExceptionsThrownException() {
    this("No Exceptions Thrown");
  }

  public NoExceptionsThrownException(String message) {
    super(message);
  }
}