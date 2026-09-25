package br.com.clinton.executor;

import java.util.*;

/** Preserve argv for native Windows executables under the JDK's legacy command-line mode. */
final class ProcessArguments {
    static List<String> literal(List<String> command) {
        if (!System.getProperty("os.name").startsWith("Windows")) return command;
        if ("false".equalsIgnoreCase(System.getProperty("jdk.lang.Process.allowAmbiguousCommands"))) {
            // The JDK safe mode escapes interior quotes itself, but treats outer quotes as syntax.
            for (String arg : command.subList(1, command.size()))
                if (arg.length() >= 2 && arg.startsWith("\"") && arg.endsWith("\""))
                    throw new IllegalArgumentException(
                            "Argumento com aspas externas incompatível com o modo estrito de processos"
                                    + " do JDK. Use um arquivo de ambiente para esse valor.");
            return command;
        }
        List<String> encoded = new ArrayList<>();
        encoded.add(command.getFirst());
        for (String arg : command.subList(1, command.size())) {
            StringBuilder value = new StringBuilder("\"");
            int slashes = 0;
            for (char c : arg.toCharArray()) {
                if (c == '\\') { slashes++; continue; }
                value.append("\\".repeat(c == '"' ? slashes * 2 + 1 : slashes));
                value.append(c);
                slashes = 0;
            }
            value.append("\\".repeat(slashes * 2)).append('"');
            encoded.add(value.toString());
        }
        return encoded;
    }
}
