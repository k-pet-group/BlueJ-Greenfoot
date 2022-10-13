/*
 This file is part of the BlueJ program. 
 Copyright (C) 2016 Michael Kölling and John Rosenberg 
 
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
package bluej.stride.framedjava.convert;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import bluej.parser.lexer.LocatableToken;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * A modifier.  Might be a keyword (e.g. "public", "final") or an annotation
 * e.g. "@Override", "@Test(true)"
 */
interface Modifier
{
    /**
     * Returns true if this is a keyword modifier equal to the given string
     */
    public boolean isKeyword(String modifier);

    /**
     * Returns true if this is an annotation where the name matches the given name.
     */
    public boolean isAnnotation(String annotation);

    LocatableToken getStart();
    LocatableToken getEnd();

    static class KeywordModifier implements Modifier
    {
        private final LocatableToken keyword;
        
        KeywordModifier(LocatableToken keyword)
        {
            this.keyword = keyword;
        }

        @Override
        public boolean isKeyword(String modifier)
        {
            return this.keyword.getText().equals(modifier);
        }

        @Override
        public boolean isAnnotation(String annotation)
        {
            return false;
        }

        @Override
        public LocatableToken getStart()
        {
            return keyword;
        }

        @Override
        public LocatableToken getEnd()
        {
            return keyword;
        }

        @Override
        public String toString()
        {
            return keyword.getText();
        }
    }

    static class AnnotationModifier implements Modifier
    {
        // Without the "@"
        private final String annotation;
        private final LocatableToken start;
        private final LocatableToken end;
        private List<Expression> params; // null if not present

        public AnnotationModifier(List<LocatableToken> annotation)
        {
            this.annotation = annotation.stream().map(LocatableToken::getText).collect(Collectors.joining());
            this.start = annotation.get(0);
            this.end = annotation.get(annotation.size() - 1);
        }

        public void setParams(List<Expression> params)
        {
            this.params = new ArrayList<>(params);
        }

        @Override
        public boolean isKeyword(String modifier)
        {
            return false;
        }

        @Override
        public boolean isAnnotation(String annotation)
        {
            // Accept either with or without @:
            if (annotation.startsWith("@"))
                return this.annotation.equals(annotation.substring(1));
            else
                return this.annotation.equals(annotation);
        }

        @Override
        public String toString()
        {
            return "@" + annotation + (params == null ? "" : params.stream().map(Expression::toString).collect(Collectors.joining(" , ")));
        }

        @Override
        public LocatableToken getEnd()
        {
            return end;
        }

        @Override
        public LocatableToken getStart()
        {
            return start;
        }
    }
}
