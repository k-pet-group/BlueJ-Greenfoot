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

// $Id: StatementSpan.java 1011 2001-11-22 10:36:26Z ajp $
package net.sourceforge.transmogrify.hook;

import net.sourceforge.transmogrify.symtab.Occurrence;


import java.io.File;

public class StatementSpan {
  Occurrence _start;
  Occurrence _end;
  Hook _hook;

  public StatementSpan(Hook hook) {
    _hook = hook;
    _start = null;
    _end = null;
  }

  public Occurrence getStart() throws Exception {
    if (_start == null) {
      calculate();
    }
    return _start;
  }

  public Occurrence getEnd() throws Exception {
    if (_end == null) {
      calculate();
    }
    return _end;
  }

  public void calculate() throws Exception {
    String selectedText = _hook.getSelectedText();
    char[] chars = new char[selectedText.length()];

    selectedText.getChars(0, selectedText.length(), chars, 0);

    int leftBound;
    int rightBound;

    for (leftBound = 0;
         leftBound <= selectedText.length() && Character.isWhitespace(chars[leftBound]);
         leftBound++);

    boolean end = false;

    for (rightBound = selectedText.length()-1; rightBound > leftBound; rightBound--) {
      if (end) {
        break;
      }
      else if (Character.isWhitespace(chars[rightBound]) || isParen(chars[rightBound])) {}
      else if (chars[rightBound] == ';') {
        if (chars[rightBound-1] != ')') {
          end = true;
        }
      }
      else if (chars[rightBound] == '}') {
        end = true;
      }
      else if (chars[rightBound] =='(') {
        end = true;
      }
      else {
        break;
      }
    }

    int selectStartPos = _hook.getSelectionStart();
    int selectEndPos = _hook.getSelectionEnd();

    int startPosition = selectStartPos + leftBound;
    int endPosition = selectStartPos + rightBound;

    _hook.setCaretPos(startPosition);
    _start = new Occurrence(new File(_hook.getCurrentFile()),
                            _hook.getCaretLineNumber(),
                            _hook.getCaretOffset());

    _hook.setCaretPos(endPosition);
    _end  = new Occurrence(new File(_hook.getCurrentFile()),
                           _hook.getCaretLineNumber(),
                           _hook.getCaretOffset());

  }

  private boolean isParen(char c) {
    return ( c == '(' || c == ')' );
  }
}