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
package net.sourceforge.transmogrify.symtab.parser.test;

import junit.framework.*;
import net.sourceforge.transmogrify.symtab.parser.Span;


public class SpanTest extends TestCase {

  private Span span = null;

  public SpanTest(String name) {
    super(name);
  }

  public void setUp() {
    span = new Span(7, 9, 15, 6);
  }

  public void testContainsEnclosedSpan() {
    Span contained = new Span(7, 13, 12, 9);
    assert(span.contains(contained));
  }

  public void testContainsIdenticalSpan() {
    Span identical = new Span(7, 9, 15, 6);
    assert(span.contains(identical));
  }

  public void testDoesNotContainDisjointSpan() {
    Span notContained = new Span(3, 3, 7, 8);
    assert(!span.contains(notContained));
  }

  public void testDoesNotContainLeftOverlappingSpan() {
    Span notContained = new Span(3, 3, 11, 9);
    assert(!span.contains(notContained));
  }

  public void testDoesNotContainRightOverlappingSpan() {
    Span notContained = new Span(8, 73, 21, 16);
    assert(!span.contains(notContained));
  }

  public void testContainsPoint() {
    assert(span.contains(8, 73));
  }

  public void testContainsPointAtBeginning() {
    assert(span.contains(7, 9));
  }

  public void testContainsPointAtEnd() {
    assert(span.contains(15,6));
  }

  public void testDoesNotContainPointBefore() {
    assert(!span.contains(6, 21));
  }

  public void testDoesNotContainPointBeforeOnSameLine() {
    assert(!span.contains(7, 8));
  }

  public void testDoesNotContainPointAfter() {
    assert(!span.contains(21, 15));
  }

  public void testDoesNotContainPointAfterOnSameLine() {
    assert(!span.contains(15, 7));
  }

  /* protected methods -- what's the best way to deal with this?
  public void testStartsBefore() {
    Span startsBefore = new Span(12,1,20,1);
    assert(span.startsBefore(startsBefore));
  }

  public void testDoesNotStartBefore() {
    Span doesNotStartBefore = new Span(1, 1, 20, 1);
    assert(!span.startsBefore(doesNotStartBefore));
  }

  public void testEndsAfter() {
    Span endsAfter = new Span(1,1,9,11);
    assert(span.endsAfter(endsAfter));
  }

  public void testDoesNotEndAfter() {
    Span doesNotEndAfter = new Span(1,1,20,1);
    assert(!span.endsAfter(doesNotEndAfter));
  }
  */

  public void testEquals() {
    assert(span.equals(new Span(7, 9, 15, 6)));
  }

  public void testNotEquals() {
    assert(!span.equals(new Span(7, 8, 15, 7)));
  }

  public void testComposeWithContainedSpan() {
    Span contained = new Span(7, 10, 14, 6);
    span.compose(contained);
    assert(span.equals(new Span(7, 9, 15, 6)));
  }

  public void testComposeWithEnclosingSpan() {
    Span contains = new Span(1, 1, 20, 1);
    span.compose(contains);
    assert(span.equals(new Span(1, 1, 20, 1)));
  }

  public void testComposeWithLeftOverlappingSpan() {
    Span overlaps = new Span(1, 1, 14, 5);
    span.compose(overlaps);
    assert(span.equals(new Span(1, 1, 15, 6)));
  }

  public void testComposedWithRightOverlappingSpan() {
    Span overlaps = new Span(7, 10, 20, 1);
    span.compose(overlaps);
    assert(span.equals(new Span(7, 9, 20, 1)));
  }

  public void testComposedWithLeftDisjointSpan() {
    Span disjoint = new Span(1, 1, 3, 1);
    span.compose(disjoint);
    assert(span.equals(new Span(1, 1, 15, 6)));
  }

  public void testComposedWithRightDisjointSpan() {
    Span disjoint = new Span(18, 1, 20, 1);
    span.compose(disjoint);
    assert(span.equals(new Span(7, 9, 20, 1)));
  }

  public void testToString() {
    assert(span.toString().equals("[7,9:15,6]"));
  }

  public static void main(String[] args) {
    junit.swingui.TestRunner.main(new String[] { SpanTest.class.getName() });
  }
}
