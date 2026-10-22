package module.evolview.moderntreeviewer.para;

import egps2.EGPSProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/** Reads and commits user text off the EDT; invalid files are never overwritten on load. */
public final class TreeSettingsPreferences {
    private static final Logger log = LoggerFactory.getLogger(TreeSettingsPreferences.class);
    private static final String EMPTY = "{\n    // Add only the settings you want to override.\n    // Copy names from Default settings on the left.\n}\n";
    private static final class Application {
        private static final TreeSettingsPreferences INSTANCE = new TreeSettingsPreferences(
                Path.of(EGPSProperties.JSON_DIR, "modern-tree-view.settings.jsonc"));
    }

    public record State(String text, Map<String, Object> values, String warning) { }
    public record Commit(State state, Map<String, Object> changes) { }

    private final Path path;
    private CompletableFuture<State> loading;
    private volatile State current;
    private boolean warningShown;

    public TreeSettingsPreferences(Path path) { this.path = path; }
    public static TreeSettingsPreferences application() { return Application.INSTANCE; }

    public synchronized CompletableFuture<State> loadAsync() {
        if (loading == null) loading = CompletableFuture.supplyAsync(() -> {
            String text = EMPTY;
            State state;
            try {
                if (Files.exists(path)) text = Files.readString(path, StandardCharsets.UTF_8);
                state = new State(text, java.util.Collections.unmodifiableMap(new java.util.LinkedHashMap<>(TreeSettings.parse(text))), null);
            } catch (IOException | IllegalArgumentException error) {
                String warning = "Unable to load tree preferences from " + path + ": " + error.getMessage()
                        + ". Program defaults will be used. Repair this file before saving user defaults.";
                log.warn(warning, error);
                state = new State(text, Map.of(), warning);
            }
            current = state;
            return state;
        });
        return loading.thenApply(ignored -> current);
    }

    public synchronized boolean claimWarning() {
        if (warningShown || current == null || current.warning == null) return false;
        warningShown = true;
        return true;
    }

    public CompletableFuture<Commit> saveDefaultsAsync(Map<String, Object> values) {
        Map<String, Object> reusable = new java.util.LinkedHashMap<>(values);
        reusable.remove("Node style/Scope");
        reusable.remove("Creative mode/Enabled");
        reusable.values().removeIf("Mixed"::equals);
        return loadAsync().thenComposeAsync(state -> {
            if (state.warning() != null) throw new CompletionException(new IOException("Repair the existing user defaults before saving: " + path + ". " + state.warning()));
            if (state.values().containsKey("Creative mode/Enabled")) reusable.put("Creative mode/Enabled", false);
            return saveAsync(SettingsJson.update(state.text(), reusable));
        });
    }

    public CompletableFuture<Commit> saveAsync(String text) {
        // Validate before scheduling any filesystem changes, including calls outside the dialog.
        Map<String, Object> parsed = java.util.Collections.unmodifiableMap(new java.util.LinkedHashMap<>(TreeSettings.parse(text)));
        return loadAsync().thenApplyAsync(ignored -> {
            synchronized (this) {
                Path temporary = null;
                try {
                    Path target = path.toAbsolutePath();
                    Files.createDirectories(target.getParent());
                    temporary = Files.createTempFile(target.getParent(), "tree-settings-", ".tmp");
                    Files.writeString(temporary, text, StandardCharsets.UTF_8);
                    try { Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
                    catch (AtomicMoveNotSupportedException unsupported) { Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING); }
                    Map<String, Object> changes = TreeSettings.changes(current.values, parsed);
                    current = new State(text, parsed, null);
                    return new Commit(current, changes);
                } catch (IOException error) {
                    throw new CompletionException(new IOException("Cannot save tree preferences to " + path + ": " + error.getMessage(), error));
                } finally {
                    if (temporary != null) {
                        try { Files.deleteIfExists(temporary); }
                        catch (IOException error) { log.warn("Unable to remove temporary settings file {}", temporary, error); }
                    }
                }
            }
        });
    }
}
