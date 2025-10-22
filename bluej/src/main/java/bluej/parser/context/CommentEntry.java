package bluej.parser.context;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * Represents a single comment entry (method, field, constructor, etc.)
 * Each entry contains a target signature, documentation text, and optional parameter names.
 *
 * <p>Package-private - internal to .ctxt file parsing implementation.
 */
class CommentEntry {
    // Method/field signature
    @NotNull
    private final String target;

    // Comment text
    @NotNull
    private final String text;

    // Parameter names (if applicable)
    @NotNull
    private final List<String> paramNames;

    /**
     * Creates a new comment entry.
     *
     * @param target     The target signature (e.g. "void test(int,String)")
     * @param text       The comment text
     * @param paramNames The parameter names
     */
    public CommentEntry(@NotNull String target, @NotNull String text, @NotNull List<String> paramNames) {
        this.target = target;
        this.text = text;
        this.paramNames = List.copyOf(paramNames);
    }

    /**
     * Creates a new comment entry with no parameter names.
     *
     * @param target The target signature (e.g. "void test(int,String)")
     * @param text   The comment text
     */
    public CommentEntry(@NotNull String target, @NotNull String text) {
        this(target, text, Collections.emptyList());
    }

    /**
     * Creates a new comment entry, with no text and parameter names.
     *
     * @param target The target signature (e.g. "void test(int,String)")
     */
    public CommentEntry(@NotNull String target) {
        this(target, "");
    }

    /**
     * Gets the target signature.
     *
     * @return The target signature
     */
    @NotNull
    public String getTarget() {
        return target;
    }

    /**
     * Gets the comment text.
     *
     * @return The comment text, or null if not present
     */
    @NotNull
    public String getText() {
        return text;
    }

    /**
     * Gets the parameter names.
     *
     * @return An unmodifiable list of parameter names
     */
    @NotNull
    public List<String> getParamNames() {
        return Collections.unmodifiableList(paramNames);
    }
}
