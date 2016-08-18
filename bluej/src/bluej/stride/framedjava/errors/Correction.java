/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2016 Michael Kölling and John Rosenberg
 
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
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package bluej.stride.framedjava.errors;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import bluej.utility.Utility;
import bluej.utility.javafx.FXPlatformConsumer;
import threadchecker.OnThread;
import threadchecker.Tag;

class Correction extends FixSuggestion
{
    private final String correction;
    private final String display;
    private final FXPlatformConsumer<String> replacer;

    // This doesn't have to be private, but in practice you'll only ever use the
    // winnowCorrections method below
    @OnThread(Tag.Any)
    private Correction(String correction, FXPlatformConsumer<String> replacer, String display)
    {
        this.correction = correction;
        this.display = display;
        this.replacer = replacer;
    }

    @Override
    public String getDescription()
    {
        return "Correct to: " + display;
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public void execute()
    {
        replacer.accept(correction);
    }
    
    private final static int MAX_EDIT_DISTANCE = 2;

    @OnThread(Tag.Any)
    private static class StringAndDist
    {
        public final CorrectionInfo value;
        public final int distance;
        
        public StringAndDist(CorrectionInfo value, int distance)
        {
            this.value = value;
            this.distance = distance;
        }
    }

    @OnThread(Tag.Any)
    public static interface CorrectionInfo
    {
        // The actual String to correct to (used for edit distance calculation):
        public String getCorrection();
        // The text to display to the user in the fix list:
        public String getDisplay();
    }

    @OnThread(Tag.Any)
    public static class SimpleCorrectionInfo implements CorrectionInfo
    {
        private String correction;
        public SimpleCorrectionInfo(String correction)
        {
            this.correction = correction;
        }
        public String getCorrection() { return correction; }
        public String getDisplay() { return correction; }
    }

    // List is in order, best correction first
    @OnThread(Tag.Any)
    public static List<Correction> winnowAndCreateCorrections(String cur, Stream<CorrectionInfo> possibleCorrections, FXPlatformConsumer<String> replacer)
    {
        return possibleCorrections
                .map(n -> new StringAndDist(n, Utility.editDistance(cur.toLowerCase(), n.getCorrection().toLowerCase())))
                .filter(sd -> sd.distance <= MAX_EDIT_DISTANCE)
                .sorted((a, b) -> Integer.compare(a.distance, b.distance))
                .limit(3)
                .map(sd -> new Correction(sd.value.getCorrection(), replacer, sd.value.getDisplay()))
                .collect(Collectors.toList());
    }

    
}