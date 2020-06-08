/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2016,2019,2020 Michael Kölling and John Rosenberg
 
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
package bluej.editor.fixes;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import bluej.Config;
import bluej.parser.AssistContentThreadSafe;
import bluej.utility.Utility;
import bluej.utility.javafx.FXPlatformConsumer;
import threadchecker.OnThread;
import threadchecker.Tag;

public class Correction extends FixSuggestion
{
    private final String correction;
    private final String display;
    private final FXPlatformConsumer<String> replacer;
    private final static int MAX_EDIT_DISTANCE = 2;

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
    @OnThread(Tag.Any)
    public String getDescription()
    {
        return Config.getString("editor.quickfix.correctToSugg.fixMsg") + display;
    }

    @Override
    @OnThread(Tag.FXPlatform)
    public void execute()
    {
        replacer.accept(correction);
    }

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
        // The String use to make a comparison with (because the actual correction may differ)
        public String getCorrectionToCompareWith();

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

        public String getCorrection()
        {
            return correction;
        }

        public String getCorrectionToCompareWith()
        {
            return correction;
        }

        public String getDisplay()
        {
            return correction;
        }
    }

    @OnThread(Tag.Any)
    public static class TypeCorrectionInfo implements CorrectionInfo
    {
        private AssistContentThreadSafe acts = null;

        public TypeCorrectionInfo(AssistContentThreadSafe acts)
        {
            this.acts = acts;
        }

        public String getCorrection()
        {
            return acts.getPackage() == null || acts.getPackage().length() == 0 || acts.getPackage().equals("java.lang") ? acts.getName() : acts.getPackage() + "." + acts.getName();
        }

        public String getCorrectionToCompareWith()
        {
            return acts.getName();
        }

        public String getDisplay()
        {
            String pkg = acts.getPackage();
            return (pkg == null || pkg.length() == 0) ? acts.getName() : (acts.getName() + " (" + pkg + " package)");
        }
    }

    // List is in order, best correction first (case insensitive)
    public static List<Correction> winnowAndCreateCorrections(String cur, Stream<CorrectionInfo> possibleCorrections, FXPlatformConsumer<String> replacer)
    {
        return winnowAndCreateCorrections(cur, possibleCorrections, replacer, false);
    }

    //List in order, best correction first (case sensitivity can be chosen, if true, the correction with the same value and same case isn't returned)
    public static List<Correction> winnowAndCreateCorrections(String cur, Stream<CorrectionInfo> possibleCorrections, FXPlatformConsumer<String> replacer, boolean caseSensitive)
    {
        return possibleCorrections
            .map(n -> new StringAndDist(n, Utility.editDistance(cur.toLowerCase(), n.getCorrectionToCompareWith().toLowerCase())))
            .filter(sd -> sd.distance <= MAX_EDIT_DISTANCE && (!caseSensitive || (caseSensitive && !sd.value.getCorrectionToCompareWith().equals(cur))))
            .sorted((a, b) -> Integer.compare(a.distance, b.distance))
            .limit(3)
            .map(sd -> new Correction(sd.value.getCorrection(), replacer, sd.value.getDisplay()))
            .collect(Collectors.toList());
    }

    /**
     * Given a class, checks if it belongs to a package that is considered to be commonly used
     * in BlueJ. The list of packages is contained within this method.
     * Note: top level packages are used. For example, "java.util", but all their subpackages are
     * taken into consideration in in the method. There is no granularity.
     *
     * @param acts the AssistContentThreadSafe representing a class.
     * @return A boolean value indicating if the class belongs to the subset of packages of that method.
     */
    @OnThread(Tag.Any)
    public static boolean isClassInUsualPackagesForCorrections(AssistContentThreadSafe acts)
    {
        List<String> commmonPackages = Arrays.asList(
            "java.lang",
            "java.util",
            "javafx",
            "javax.swing");
        return commmonPackages.contains(acts.getPackage()) || commmonPackages.stream().filter(s -> acts.getPackage().startsWith(s + ".")).collect(Collectors.toList()).size() > 0;
      }
}