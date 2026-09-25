package br.com.clinton.executor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Resolves npm launchers without invoking cmd.exe, PowerShell or a shell. */
public final class RunnerCommandResolver {
    public record Command(List<String> prefix, Map<String, String> environment, String mode) {
        public Command {
            prefix = List.copyOf(prefix);
            environment = Map.copyOf(environment);
        }
    }

    public static Command resolve(String executable, String variable) throws IOException {
        return resolve(
                executable,
                variable,
                System.getenv(),
                System.getProperty("os.name").startsWith("Windows"));
    }

    // Explicit platform/environment makes Windows lookup testable on all build agents.
    static Command resolve(
            String executable, String variable, Map<String, String> env, boolean windows)
            throws IOException {
        boolean bruno = variable.equals("BRU_EXECUTABLE");
        String tool = bruno ? "bru" : "newman";
        String display = bruno ? "Bruno CLI" : "Newman";
        Path launcher = find(executable, env, windows, true);
        if (launcher == null)
            throw new IllegalStateException(
                    display
                            + " não encontrado. Configure "
                            + variable
                            + " ou PATH. No Windows, verifique também o prefixo npm e"
                            + " APPDATA/npm.");
        Map<String, String> child = new LinkedHashMap<>(env);
        String name = launcher.getFileName().toString().toLowerCase(Locale.ROOT);
        if (windows && (name.endsWith(".cmd") || name.endsWith(".bat") || name.endsWith(".ps1"))
                || name.endsWith(".js")
                || name.endsWith(".cjs")
                || name.endsWith(".mjs")) {
            Path entry = launcher;
            if (!name.endsWith(".js") && !name.endsWith(".cjs") && !name.endsWith(".mjs")) {
                Path pkg =
                        launcher.getParent()
                                .resolve("node_modules")
                                .resolve(bruno ? "@usebruno/cli" : "newman");
                Path manifest = pkg.resolve("package.json");
                if (!Files.isRegularFile(manifest))
                    throw new IllegalStateException(
                            display
                                    + ": launcher npm encontrado, mas package.json ausente."
                                    + " Reinstale o pacote no mesmo prefixo npm ou configure "
                                    + variable
                                    + " com o entrypoint .js.");
                JsonNode json;
                try {
                    json = new ObjectMapper().readTree(manifest.toFile());
                } catch (IOException e) {
                    throw new IllegalStateException(
                            display + ": package.json inválido. Reinstale o pacote npm.");
                }
                JsonNode bin = json.path("bin");
                String script = bin.isTextual() ? bin.asText() : bin.path(tool).asText("");
                entry = pkg.resolve(script).normalize();
                if (script.isBlank()
                        || !entry.startsWith(pkg.normalize())
                        || !Files.isRegularFile(entry))
                    throw new IllegalStateException(
                            display
                                    + ": entrypoint bin ausente/inválido no pacote npm. Reinstale o"
                                    + " pacote.");
            }
            String nodeOverride = environmentValue(env, "NODE_EXECUTABLE");
            Path sibling = launcher.getParent().resolve(windows ? "node.exe" : "node");
            Path node =
                    nodeOverride != null
                            ? find(nodeOverride, env, windows, false)
                            : usable(sibling, windows)
                                    ? sibling
                                    : find("node", env, windows, false);
            if (node == null
                    || (windows
                            && !node.toString()
                                    .toLowerCase(Locale.ROOT)
                                    .matches(".*\\.(exe|com)$")))
                throw new IllegalStateException(
                        "Node.js não encontrado. Configure NODE_EXECUTABLE com node.exe/node ou"
                            + " ajuste PATH.");
            prependPath(child, List.of(node.getParent(), launcher.getParent()), windows);
            return new Command(
                    List.of(node.toString(), entry.toAbsolutePath().toString()),
                    child,
                    "node + entrypoint npm (sem shell)");
        }
        prependPath(child, List.of(launcher.getParent()), windows);
        return new Command(List.of(launcher.toString()), child, "executável direto (sem shell)");
    }

    public static String environmentValue(Map<String, String> env, String key) {
        return env.entrySet().stream()
                .filter(e -> e.getKey().equalsIgnoreCase(key))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    private static Path find(
            String executable, Map<String, String> env, boolean windows, boolean npmFallback) {
        if (executable == null || executable.isBlank()) return null;
        Path supplied = Path.of(executable);
        if (supplied.isAbsolute() || supplied.getParent() != null)
            return usable(supplied, windows) ? supplied.toAbsolutePath().normalize() : null;
        List<Path> directories = new ArrayList<>();
        String path = Objects.toString(environmentValue(env, "PATH"), "");
        for (String p : path.split(windows ? ";" : File.pathSeparator)) {
            if (!p.isBlank()) directories.add(Path.of(unquote(p)));
        }
        if (windows && npmFallback) {
            String appData = environmentValue(env, "APPDATA");
            if (appData != null && !appData.isBlank()) directories.add(Path.of(appData, "npm"));
        }
        List<String> suffixes = new ArrayList<>();
        if (windows && !executable.contains(".")) {
            String extensions =
                    Objects.toString(environmentValue(env, "PATHEXT"), ".COM;.EXE;.BAT;.CMD");
            for (String ext : extensions.split(";"))
                if (Set.of(".exe", ".com", ".cmd", ".bat").contains(ext.toLowerCase(Locale.ROOT)))
                    suffixes.add(ext.toLowerCase(Locale.ROOT));
            // npm's extensionless file is a POSIX shell script, not a Windows executable.
            if (!suffixes.contains(".cmd")) suffixes.add(".cmd");
        } else suffixes.add("");
        for (Path dir : directories)
            for (String suffix : suffixes) {
                Path candidate = dir.resolve(executable + suffix);
                if (usable(candidate, windows)) return candidate.toAbsolutePath().normalize();
            }
        return null;
    }

    private static String unquote(String s) {
        return s.startsWith("\"") && s.endsWith("\"") ? s.substring(1, s.length() - 1) : s;
    }

    private static boolean usable(Path p, boolean windows) {
        return Files.isRegularFile(p)
                && (windows || Files.isExecutable(p) || p.toString().matches(".*\\.(js|cjs|mjs)$"));
    }

    private static void prependPath(Map<String, String> env, List<Path> dirs, boolean windows) {
        String old = Objects.toString(environmentValue(env, "PATH"), "");
        String key =
                env.keySet().stream()
                        .filter(k -> k.equalsIgnoreCase("PATH"))
                        .findFirst()
                        .orElse("PATH");
        env.keySet().removeIf(k -> k.equalsIgnoreCase("PATH"));
        String separator = windows ? ";" : File.pathSeparator;
        env.put(
                key,
                String.join(separator, dirs.stream().map(Path::toString).distinct().toList())
                        + separator
                        + old);
    }
}
