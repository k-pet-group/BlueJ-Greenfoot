package bluej.stride.framedjava.convert;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import bluej.parser.lexer.LocatableToken;

/**
 * A modifier.  Might be a keyword (e.g. "public", "final") or an annotation
 * e.g. "@Override", "@Test(true)"
 */
interface Modifier
{
    public boolean isKeyword(String modifier);

    public boolean isAnnotation(String annotation);

    static class KeywordModifier implements Modifier
    {
        private final String keyword;
        KeywordModifier(LocatableToken keyword)
        {
            this.keyword = keyword.getText();
        }

        @Override
        public boolean isKeyword(String modifier)
        {
            return this.keyword.equals(modifier);
        }

        @Override
        public boolean isAnnotation(String annotation)
        {
            return false;
        }

        @Override
        public String toString()
        {
            return keyword;
        }
    }

    static class AnnotationModifier implements Modifier
    {
        // Without the "@"
        private final String annotation;
        private final List<Expression> params;

        public AnnotationModifier(List<LocatableToken> annotation, List<Expression> params)
        {
            this.annotation = annotation.stream().map(LocatableToken::getText).collect(Collectors.joining());
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
            return "@" + annotation + params.stream().map(Expression::toString).collect(Collectors.joining(" , "));
        }
    }
}
