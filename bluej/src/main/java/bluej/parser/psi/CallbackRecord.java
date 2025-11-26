package bluej.parser.psi;

import bluej.parser.lexer.LocatableToken;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class CallbackRecord {
    private final String callbackName;
    private final Map<String, Object> parameters;
    private final long timestamp;
    private final StackTraceElement[] callStackTrace;

    /**
     * Creates a new callback record with explicit stacktrace.
     *
     * <p>This constructor is used by {@link CallbackRecorder} during actual parsing
     * to capture the real parsing code path.
     *
     * @param callbackName The name of the callback that was invoked
     * @param parameters   Map of parameter names to values
     * @param callStackTrace The execution stacktrace at callback invocation time (for debugging)
     */
    public CallbackRecord(String callbackName, Map<String, Object> parameters, StackTraceElement[] callStackTrace) {
        if (callStackTrace == null) {
            throw new IllegalArgumentException("callStackTrace cannot be null");
        }
        this.callbackName = callbackName;
        this.parameters = new HashMap<>(parameters); // Defensive copy
        this.timestamp = System.currentTimeMillis();
        this.callStackTrace = callStackTrace;
    }

    /**
     * Creates a new callback record with automatically captured stacktrace.
     *
     * <p>This convenience constructor is primarily for test code that manually creates
     * CallbackRecord objects. It automatically captures the current stacktrace at the
     * point of construction.
     *
     * @param callbackName The name of the callback that was invoked
     * @param parameters   Map of parameter names to values
     */
    public CallbackRecord(String callbackName, Map<String, Object> parameters) {
        this(callbackName, parameters, Thread.currentThread().getStackTrace());
    }

    /**
     * Returns the callback name.
     *
     * @return The callback name (e.g., "beginClass", "gotMethodDeclaration")
     */
    public String getCallbackName() {
        return callbackName;
    }

    /**
     * Returns all parameters passed to the callback.
     *
     * <p>The returned map is unmodifiable to preserve record immutability.
     *
     * @return Unmodifiable map of parameter names to values
     */
    public Map<String, Object> getParameters() {
        return Collections.unmodifiableMap(parameters);
    }

    /**
     * Returns a specific parameter value.
     *
     * <p>Returns null if the parameter doesn't exist. Tests should use
     * appropriate casting based on the callback's expected parameter types.
     *
     * @param name The parameter name
     * @return The parameter value, or null if not present
     */
    public Object getParameter(String name) {
        return parameters.get(name);
    }

    public LocatableToken getParameterToken() {
        return (LocatableToken) parameters.values().stream().filter(o -> o instanceof LocatableToken).findFirst().orElse(null);
    }

    /**
     * Returns the timestamp when this callback was invoked.
     *
     * @return Milliseconds since epoch
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Returns the execution stacktrace captured when this callback was invoked.
     *
     * <p>This stacktrace captures the actual parsing code path, showing where the
     * callback was recorded during PSI traversal. This is critical for debugging
     * as it shows the real invocation site, not the validation site.
     *
     * @return The stacktrace at callback invocation time
     */
    public StackTraceElement[] getCallStackTrace() {
        return callStackTrace;
    }

    @Override
    public String toString() {
        return callbackName + parameters.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CallbackRecord that = (CallbackRecord) o;
        return timestamp == that.timestamp &&
                Objects.equals(callbackName, that.callbackName) &&
                Objects.equals(parameters, that.parameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(callbackName, parameters, timestamp);
    }
}
