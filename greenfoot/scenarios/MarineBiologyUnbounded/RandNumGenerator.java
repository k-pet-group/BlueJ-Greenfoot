// AP(r) Computer Science Marine Biology Simulation:
// The RandNumGenerator class is copyright(c) 2002 College Entrance
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

import java.util.Random;

/**
 *  AP&reg; Computer Science Marine Biology Simulation:<br>
 *  The <code>RandNumGenerator</code> class provides a singleton
 *  <code>java.util.Random</code> object for random number generation.  Using
 *  this class, many different objects can share a single source of random
 *  numbers.  This eliminates the potential problem of having multiple random
 *  number generators generating sequences of numbers that are too similar.
 *
 *  <p>
 *  Example of how to use <code>RandNumGenerator</code>:  
 *    <pre><code>
 *       import java.util.Random;
 *
 *       Random randNumGen = RandNumGenerator.getInstance();
 *       int randomNum = randNumGen.nextInt(4);
 *       double randomDouble = randNumGen.nextDouble();
 *    </code></pre>
 *
 *  <p>
 *  The <code>RandNumGenerator</code> class is
 *  copyright&copy; 2002 College Entrance Examination Board
 *  (www.collegeboard.com).
 *
 *  @author Alyce Brady
 *  @version 1 July 2002
 *  @see java.util.Random
 **/
public class RandNumGenerator
{
    // Class Variable: Only one generator is created by this class.
    private static Random theRandNumGenerator = new Random();

    /** Returns a random number generator.
     *  Always returns the same <code>Random</code> object to provide
     *  a better sequence of random numbers.
     **/
    public static Random getInstance()
    {
        return theRandNumGenerator;
    }
}
