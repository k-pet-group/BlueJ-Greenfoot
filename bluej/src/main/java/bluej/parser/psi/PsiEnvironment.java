package bluej.parser.psi;

import org.jetbrains.kotlin.com.intellij.mock.MockProject;
import org.jetbrains.kotlin.com.intellij.openapi.Disposable;
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer;
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys;
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer;
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector;
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.config.CommonConfigurationKeys;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtPsiFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Singleton PSI environment manager for Kotlin parsing.
 * 
 * <p>Uses manual KotlinCoreEnvironment setup (no Analysis API dependency)
 * for minimal footprint PSI-only parsing.</p>
 * 
 * <p><b>Thread Safety:</b> Synchronized initialization, single-threaded usage assumed after init</p>
 * 
 * <p><b>Lifecycle:</b> Lazy init on first use, JVM shutdown hook for cleanup</p>
 * 
 * <p><b>Design Pattern:</b> Singleton with double-checked locking</p>
 * 
 * @see <a href="docs/planning/tasks/01-psi-environment.md">Task Specification</a>
 * @see <a href="docs/references/SimpleKotlinPsiEnvironment.java">Reference Pattern</a>
 */
public final class PsiEnvironment {
    
    // ==================== SINGLETON ====================
    
    /**
     * The singleton instance (volatile for double-checked locking).
     */
    private static volatile PsiEnvironment INSTANCE;
    
    /**
     * Lock object for synchronized initialization.
     */
    private static final Object LOCK = new Object();
    
    // ==================== LOGGING FLAGS ====================
    
    /**
     * Enable detailed initialization logging via system property: -Dbluej.psi.log.init=true
     */
    private static final boolean LOG_PSI_INIT = Boolean.getBoolean("bluej.psi.log.init");
    
    // ==================== FIELDS ====================
    
    /**
     * Disposable root for lifecycle management.
     */
    private final Disposable disposable;
    
    /**
     * Kotlin core environment for PSI operations.
     */
    private final KotlinCoreEnvironment coreEnvironment;
    
    /**
     * Initialization status flag.
     */
    private final boolean initialized;
    
    // ==================== CONSTRUCTOR (PRIVATE) ====================
    
    /**
     * Private constructor for singleton pattern.
     * 
     * <p>Initializes PSI environment using manual {@link KotlinCoreEnvironment#createForProduction}
     * instead of StandaloneProjectFactory to avoid Analysis API dependencies.</p>
     * 
     * <p><b>Initialization Steps:</b></p>
     * <ol>
     *   <li>Ensure idea.home.path system property is set</li>
     *   <li>Create disposable root for lifecycle management</li>
     *   <li>Configure compiler configuration (message collector, module name)</li>
     *   <li>Create KotlinCoreEnvironment with JVM config files</li>
     *   <li>Register JVM shutdown hook for cleanup</li>
     * </ol>
     * 
     * <p><b>Error Handling:</b> Failures are logged but don't crash JVM. 
     * The {@link #initialized} flag indicates success/failure.</p>
     */
    private PsiEnvironment() {
        if (LOG_PSI_INIT) {
            System.out.println("PSI: Initializing environment...");
        }
        
        boolean initSuccess = false;
        Disposable tempDisposable = null;
        KotlinCoreEnvironment tempEnvironment = null;
        
        try {
            // Step 1: Ensure idea.home.path is set
            ensureIdeaHomePath(null);
            
            // Step 2: Create disposable root
            tempDisposable = Disposer.newDisposable("BlueJ.PSI.Environment");
            
            // Step 3: Create compiler configuration
            CompilerConfiguration config = new CompilerConfiguration();
            
            // Configure message collector (for errors/warnings)
            config.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY,
                new PrintingMessageCollector(System.err, MessageRenderer.PLAIN_FULL_PATHS, false));
            
            // Configure module name
            config.put(CommonConfigurationKeys.MODULE_NAME, "bluej-psi-module");
            
            // Step 4: Create KotlinCoreEnvironment using manual setup
            // This is the key difference from SimpleKotlinPsiEnvironment - we use
            // createForProduction directly instead of StandaloneProjectFactory
            tempEnvironment = KotlinCoreEnvironment.createForProduction(
                tempDisposable,
                config,
                EnvironmentConfigFiles.JVM_CONFIG_FILES
            );
            
            // Step 5: Register shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (INSTANCE != null) {
                    INSTANCE.shutdown();
                }
            }, "PSI-Environment-Shutdown"));
            
            initSuccess = true;
            
            if (LOG_PSI_INIT) {
                System.out.println("PSI: Environment initialized successfully");
            }
            
        } catch (Exception e) {
            System.err.println("PSI environment initialization failed: " + e.getMessage());
            e.printStackTrace();
            
            // Cleanup on failure
            if (tempDisposable != null) {
                try {
                    Disposer.dispose(tempDisposable);
                } catch (Exception cleanupEx) {
                    // Ignore cleanup errors
                    if (LOG_PSI_INIT) {
                        System.err.println("PSI: Cleanup error during failed init: " + cleanupEx.getMessage());
                    }
                }
            }
        }
        
        this.initialized = initSuccess;
        this.disposable = tempDisposable;
        this.coreEnvironment = tempEnvironment;
    }
    
    // ==================== SINGLETON ACCESS ====================
    
    /**
     * Get the singleton PsiEnvironment instance.
     * 
     * <p><b>Thread-safe lazy initialization</b> using double-checked locking pattern.</p>
     * 
     * <p><b>Contract:</b> Never returns null. If initialization fails, returns an
     * instance with {@link #isInitialized()} returning false.</p>
     * 
     * @return The singleton instance (never null)
     */
    public static PsiEnvironment getInstance() {
        // First check (no synchronization)
        if (INSTANCE == null) {
            synchronized (LOCK) {
                // Second check (with synchronization)
                if (INSTANCE == null) {
                    INSTANCE = new PsiEnvironment();
                }
            }
        }
        return INSTANCE;
    }
    
    // ==================== PUBLIC API ====================
    
    /**
     * Check if PSI environment initialized successfully.
     * 
     * <p>Use this method to determine if PSI operations are available.
     * If false, all {@link #parseFile} calls will return null.</p>
     * 
     * @return true if ready for parsing, false if initialization failed
     */
    public boolean isInitialized() {
        return initialized;
    }
    
    /**
     * Parse Kotlin source code into PSI tree.
     *
     * <p><b>Usage Example:</b></p>
     * <pre>{@code
     * PsiEnvironment env = PsiEnvironment.getInstance();
     * if (env.isInitialized()) {
     *     try {
     *         KtFile ktFile = env.parseFile("Example.kt", "fun hello() = 42");
     *         // Process PSI tree
     *     } catch (PsiParseException e) {
     *         // Handle parsing failure
     *     }
     * }
     * }</pre>
     *
     * <p><b>Error Handling:</b> Throws {@link PsiParseException} on any failure.
     * All parsing errors are wrapped in PsiParseException with descriptive messages.</p>
     *
     * @param fileName The filename (e.g., "Example.kt" or "Script.kts")
     * @param sourceCode The Kotlin source code to parse
     * @return Parsed KtFile (never null)
     * @throws PsiParseException if parsing fails, environment not initialized, or source is invalid
     */
    public KtFile parseFile(String fileName, String sourceCode) throws PsiParseException {
        if (!initialized) {
            throw new PsiParseException("PSI environment not initialized");
        }
        
        if (sourceCode == null || sourceCode.isEmpty()) {
            throw new PsiParseException("Source code is null or empty for file: " + fileName);
        }
        
        try {
            KtPsiFactory factory = new KtPsiFactory(
                coreEnvironment.getProject(),
                true  // eventSystemEnabled
            );
            return factory.createFile(fileName, sourceCode);
        } catch (Exception e) {
            throw new PsiParseException("Failed to parse Kotlin file: " + fileName, e);
        }
    }
    
    /**
     * Get the underlying MockProject.
     *
     * <p><b>Note:</b> Only for advanced use cases. Most users should use {@link #parseFile} instead.</p>
     *
     * @return The project instance, or null if not initialized
     */
    public MockProject getProject() {
        return initialized ? (MockProject) coreEnvironment.getProject() : null;
    }
    
    /**
     * Shutdown the PSI environment and dispose resources.
     * 
     * <p><b>Called automatically by JVM shutdown hook.</b> Manual invocation
     * is optional and only needed for testing or controlled shutdown scenarios.</p>
     * 
     * <p><b>Idempotent:</b> Safe to call multiple times.</p>
     */
    public void shutdown() {
        if (disposable != null) {
            try {
                if (LOG_PSI_INIT) {
                    System.out.println("PSI: Shutting down environment...");
                }
                Disposer.dispose(disposable);
                if (LOG_PSI_INIT) {
                    System.out.println("PSI: Shutdown complete");
                }
            } catch (Exception e) {
                System.err.println("PSI shutdown error: " + e.getMessage());
                if (LOG_PSI_INIT) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    // ==================== PRIVATE HELPERS ====================
    
    /**
     * Ensure idea.home.path system property is set.
     * 
     * <p>Required by IntelliJ Platform for resource location. If not already set,
     * creates a temporary directory and sets the property to its absolute path.</p>
     * 
     * <p><b>Idempotent:</b> If property is already set, this method does nothing.</p>
     * 
     * @param ideaHomePath Custom path, or null to use default temp directory
     */
    private static void ensureIdeaHomePath(Path ideaHomePath) {
        final String key = "idea.home.path";
        
        // Check if already set
        if (System.getProperty(key) != null) {
            if (LOG_PSI_INIT) {
                System.out.println("PSI: idea.home.path already set to: " + System.getProperty(key));
            }
            return;
        }
        
        // Use provided path or default
        Path path = ideaHomePath;
        if (path == null) {
            path = defaultIdeaHomePath();
        }
        
        // Create directory (best effort)
        try {
            Files.createDirectories(path);
            if (LOG_PSI_INIT) {
                System.out.println("PSI: Created idea.home.path directory: " + path);
            }
        } catch (Exception e) {
            // Existing directory is OK, continue
            if (LOG_PSI_INIT) {
                System.out.println("PSI: Directory already exists or creation skipped: " + path);
            }
        }
        
        // Set system property with absolute path
        String absolutePath = path.toAbsolutePath().toString();
        System.setProperty(key, absolutePath);
        
        if (LOG_PSI_INIT) {
            System.out.println("PSI: Set idea.home.path to: " + absolutePath);
        }
    }
    
    /**
     * Generate default idea.home.path in system temp directory.
     * 
     * <p>Uses {@code $TMPDIR/bluej_psi_env} or equivalent on the current platform.</p>
     * 
     * @return Path to temporary IDEA home directory
     */
    private static Path defaultIdeaHomePath() {
        String tmp = System.getProperty("java.io.tmpdir");
        return Paths.get(tmp).resolve("bluej_psi_env");
    }
}