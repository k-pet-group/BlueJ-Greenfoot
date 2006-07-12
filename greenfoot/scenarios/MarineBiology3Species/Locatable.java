// AP(r) Computer Science Marine Biology Simulation:
// The Locatable interface is copyright(c) 2002 College Entrance
// Examination Board (www.collegeboard.com).
//
// This class is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation.
//
// This class is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.

/**
 *  AP&reg; Computer Science Marine Biology Simulation:<br>
 *  <code>Locatable</code> is an interface that guarantees that an object
 *  knows its location and returns it when the <code>location</code> 
 *  method is called.
 *
 *  <p>
 *  The <code>Locatable</code> interface is
 *  copyright&copy; 2002 College Entrance Examination Board
 *  (www.collegeboard.com).
 *
 *  @author Alyce Brady
 *  @author APCS Development Committee
 *  @version 1 July 2002
 *  @see Location
 **/

public interface Locatable
{
    /** Returns the location of this object.
     *  @return   the location of this object in the environment
     **/
    Location location();

}
