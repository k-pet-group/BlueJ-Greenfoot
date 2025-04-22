package bluej.editor.flow;

import bluej.extensions2.SourceType;

/**
 * The type of source code being edited.
 */
public enum FlowSource {
    Java,       // Java source code
    Kotlin,     // Kotlin source code
    PlainText;  // Plain text (not source code)

    /**
     * Creates a FlowSource from the given SourceType.
     * @param sourceType the source type to convert
     * @return the corresponding FlowSource
     */
    public static FlowSource fromSourceType(SourceType sourceType) {
        return switch (sourceType) {
            case Java -> Java;
            case Kotlin -> Kotlin;
            default -> PlainText;
        };
    }
}