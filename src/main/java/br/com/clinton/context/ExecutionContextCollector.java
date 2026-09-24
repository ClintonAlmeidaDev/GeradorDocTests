package br.com.clinton.context;

import java.nio.file.*;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class ExecutionContextCollector {
    public static String version() {
        Properties props = new Properties();
        try (var in = ExecutionContextCollector.class.getResourceAsStream("/version.properties")) {
            if (in != null) props.load(in);
        } catch (Exception ignored) {
        }
        return props.getProperty("version", "development");
    }

    public Map<String, String> collect(Path workspace, Map<String, String> env) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("Origem", "LOCAL");
        m.put("Usuário SO", System.getProperty("user.name", "indisponível"));
        m.put("Timezone", ZoneId.systemDefault().getId());
        m.put("Sistema", System.getProperty("os.name"));
        m.put("Tool Version", version());
        String hostname = env.getOrDefault("HOSTNAME", env.get("COMPUTERNAME"));
        if (hostname != null) m.put("Hostname", hostname);
        String commit = git(workspace, "rev-parse", "HEAD");
        if (commit == null) m.put("Git", "indisponível");
        else {
            m.put("Commit", commit);
            m.put("Short SHA", commit.substring(0, Math.min(7, commit.length())));
            put(m, "Branch", git(workspace, "rev-parse", "--abbrev-ref", "HEAD"));
            put(m, "Autor Git", git(workspace, "log", "-1", "--pretty=%an"));
            String status = git(workspace, "status", "--porcelain");
            m.put(
                    "Workspace",
                    status == null ? "indisponível" : status.isBlank() ? "CLEAN" : "DIRTY");
        }
        if ("true".equalsIgnoreCase(env.get("TF_BUILD"))) {
            ci(m, "Azure DevOps");
            bind(
                    m,
                    env,
                    "Pipeline",
                    "BUILD_DEFINITIONNAME",
                    "Build",
                    "BUILD_BUILDNUMBER",
                    "Build ID",
                    "BUILD_BUILDID",
                    "Branch",
                    "BUILD_SOURCEBRANCH",
                    "Commit",
                    "BUILD_SOURCEVERSION",
                    "Disparado por",
                    "BUILD_REQUESTEDFOR",
                    "PR",
                    "SYSTEM_PULLREQUEST_PULLREQUESTID");
        } else if ("true".equalsIgnoreCase(env.get("GITHUB_ACTIONS"))) {
            ci(m, "GitHub Actions");
            bind(
                    m,
                    env,
                    "Pipeline",
                    "GITHUB_WORKFLOW",
                    "Build",
                    "GITHUB_RUN_NUMBER",
                    "Build ID",
                    "GITHUB_RUN_ID",
                    "Branch",
                    "GITHUB_REF_NAME",
                    "Commit",
                    "GITHUB_SHA",
                    "Disparado por",
                    "GITHUB_ACTOR");
            String ref = env.getOrDefault("GITHUB_REF", "");
            if (ref.matches("refs/pull/\\d+/.*")) m.put("PR", ref.split("/")[2]);
        } else if ("true".equalsIgnoreCase(env.get("GITLAB_CI"))) {
            ci(m, "GitLab CI");
            bind(
                    m,
                    env,
                    "Pipeline",
                    "CI_PROJECT_PATH",
                    "Build",
                    "CI_PIPELINE_ID",
                    "Branch",
                    "CI_COMMIT_REF_NAME",
                    "Commit",
                    "CI_COMMIT_SHA",
                    "Disparado por",
                    "GITLAB_USER_NAME",
                    "PR",
                    "CI_MERGE_REQUEST_IID");
        } else if (env.containsKey("JENKINS_URL")) {
            ci(m, "Jenkins");
            bind(
                    m,
                    env,
                    "Pipeline",
                    "JOB_NAME",
                    "Build",
                    "BUILD_NUMBER",
                    "Branch",
                    "GIT_BRANCH",
                    "Commit",
                    "GIT_COMMIT",
                    "Disparado por",
                    "BUILD_USER_ID",
                    "PR",
                    "CHANGE_ID");
        }
        if (m.containsKey("Commit"))
            m.put("Short SHA", m.get("Commit").substring(0, Math.min(7, m.get("Commit").length())));
        return m;
    }

    private void ci(Map<String, String> m, String provider) {
        m.put("Origem", "CI/CD");
        m.put("Provider", provider);
    }

    private void bind(Map<String, String> m, Map<String, String> env, String... pairs) {
        for (int i = 0; i < pairs.length; i += 2) put(m, pairs[i], env.get(pairs[i + 1]));
    }

    private void put(Map<String, String> m, String k, String v) {
        if (v != null && !v.isBlank()) m.put(k, v);
    }

    protected String git(Path dir, String... args) {
        try {
            List<String> command = new ArrayList<>(List.of("git", "-C", dir.toString()));
            command.addAll(List.of(args));
            Process p =
                    new ProcessBuilder(command)
                            .redirectError(ProcessBuilder.Redirect.DISCARD)
                            .start();
            // Drain concurrently so a large dirty workspace cannot deadlock the process pipe.
            var bytes = new java.io.ByteArrayOutputStream();
            Thread reader =
                    Thread.ofPlatform()
                            .daemon()
                            .start(
                                    () -> {
                                        try (var in = p.getInputStream()) {
                                            in.transferTo(bytes);
                                        } catch (Exception ignored) {
                                        }
                                    });
            if (!p.waitFor(3, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                return null;
            }
            reader.join(1000);
            return p.exitValue() == 0
                    ? bytes.toString(java.nio.charset.StandardCharsets.UTF_8).trim()
                    : null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
