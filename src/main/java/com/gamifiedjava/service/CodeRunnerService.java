package com.gamifiedjava.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Compiles AND runs a single Java source file in a throwaway temp dir, capturing
 * stdout/stderr with a hard timeout. Powers POST /api/run for the Lesson Player.
 *
 * This is intentionally a lightweight local sandbox (single-user local app): temp dir,
 * kill-on-timeout, output cap. It is NOT a hardened multi-tenant sandbox.
 */
@Service
public class CodeRunnerService {

    private static final String WORK_ROOT = System.getProperty("java.io.tmpdir") + "/java-runner/";
    private static final long TIMEOUT_SECONDS = 8;
    private static final int MAX_OUTPUT_CHARS = 20_000;
    private static final int MAX_SOURCE_CHARS = 50_000;
    private static final Pattern UNICODE_ESCAPE = Pattern.compile("\\\\u[0-9a-fA-F]{4}");
    private static final List<Pattern> BLOCKED_APIS = List.of(
            Pattern.compile("\\b(?:java\\.)?(?:io|net|nio|sql|rmi)\\b"),
            Pattern.compile("\\bjavax\\.(?:naming|script)\\b"),
            Pattern.compile("\\b(?:Runtime|ProcessBuilder|ProcessHandle|ClassLoader|SecurityManager)\\b"),
            Pattern.compile("\\bSystem\\s*\\.\\s*(?:exit|getenv|getProperties|getProperty|load|loadLibrary|setSecurityManager)\\s*\\("),
            Pattern.compile("\\b(?:Class\\s*\\.\\s*forName|getClass\\s*\\(|MethodHandles?|ManagementFactory|Unsafe)\\b"),
            Pattern.compile("\\b(?:exec|startPipeline|setAccessible)\\s*\\(")
    );
    private final boolean executionAllowed;

    public CodeRunnerService(
            @Value("${code.runner.execution-enabled:false}") boolean executionEnabled) {
        this.executionAllowed = executionEnabled;
    }

    public RunResult run(String sourceCode) {
        if (sourceCode == null || sourceCode.isBlank()) {
            return RunResult.error("No code submitted.");
        }
        if (sourceCode.length() > MAX_SOURCE_CHARS) {
            return new RunResult(true, false, "", "Source code too large (max " + MAX_SOURCE_CHARS + " chars).", false);
        }
        if (containsBlockedApi(sourceCode)) {
            return new RunResult(false, false, "",
                    "This runner only supports lesson-safe Java. File, network, process, environment, reflection, and native APIs are blocked.",
                    false);
        }

        String className = extractClassName(sourceCode);
        if (className == null) className = "Main";
        if (!className.matches("[A-Za-z_$][A-Za-z0-9_$]*")) {
            return new RunResult(true, false, "", "Invalid class name.", false);
        }

        String sessionId = UUID.randomUUID().toString().substring(0, 8);
        Path workDir = Path.of(WORK_ROOT, sessionId);
        Path javaFile = workDir.resolve(className + ".java");

        try {
            Files.createDirectories(workDir);
            Files.writeString(javaFile, sourceCode);

            // --- compile ---
            String javac = findTool("javac");
            ProcessResult compile = exec(workDir, javac, "-J-Xmx128m", "-J-Xss2m",
                    "-proc:none", "-implicit:none",
                    javaFile.getFileName().toString());
            if (compile.timedOut) {
                return new RunResult(false, false, "", "Compilation timed out.", true);
            }
            if (compile.exitCode != 0) {
                return new RunResult(true, false, "", cap(compile.output), false);
            }

            // A class with no main() still compiles — that's a valid outcome for many steps.
            if (!sourceCode.contains("static void main")) {
                return new RunResult(true, true, "", "", false);
            }

            if (!executionAllowed) {
                return new RunResult(true, true, "",
                        "Code compiled successfully. Execution is disabled by server configuration.", false);
            }

            // --- run (memory-capped JVM with a restricted API surface; full-sandbox
            //     isolation requires a container — see AGENTS.md) ---
            String java = findTool("java");
            ProcessResult exec = exec(workDir, java,
                    "-Xmx128m", "-Xss2m", "-XX:MaxMetaspaceSize=64m", "-XX:ActiveProcessorCount=1",
                    "-XX:+DisableAttachMechanism", "-Djava.awt.headless=true", "-Dfile.encoding=UTF-8",
                    "-Djava.io.tmpdir=" + workDir.toAbsolutePath(),
                    "-cp", ".", className);
            if (exec.timedOut) {
                return new RunResult(true, true, cap(exec.output), "Execution timed out (possible infinite loop).", true);
            }
            String stdout = exec.output;
            String stderr = exec.exitCode == 0 ? "" : "Exited with code " + exec.exitCode;
            return new RunResult(true, true, cap(stdout), stderr, false);

        } catch (IOException e) {
            return RunResult.error("Runner error: could not prepare the sandbox directory.");
        } finally {
            deleteQuietly(workDir);
        }
    }

    private ProcessResult exec(Path dir, String... command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(dir.toFile());
            pb.environment().clear();
            pb.redirectErrorStream(true);
            Process p = pb.start();
            p.getOutputStream().close();
            OutputCollector collector = new OutputCollector(p.getInputStream());
            Thread outputThread = Thread.ofVirtual().start(collector);
            boolean finished = p.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                p.descendants().forEach(ProcessHandle::destroyForcibly);
                p.destroyForcibly();
                p.waitFor(1, TimeUnit.SECONDS);
            }
            outputThread.join(TimeUnit.SECONDS.toMillis(1));
            return new ProcessResult(finished ? p.exitValue() : -1, collector.output(), !finished);
        } catch (IOException e) {
            return new ProcessResult(-1, "Process error: could not start the runner.", false);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ProcessResult(-1, "Process error: runner interrupted.", false);
        }
    }

    /** Locate javac/java: PATH first, then JAVA_HOME/java.home, else bare name. */
    private String findTool(String tool) {
        String exe = System.getProperty("os.name").toLowerCase().contains("win") ? tool + ".exe" : tool;
        String pathEnv = System.getenv("PATH");
        if (pathEnv != null) {
            for (String dir : pathEnv.split(Pattern.quote(File.pathSeparator))) {
                Path candidate = Path.of(dir, exe);
                if (Files.exists(candidate)) return candidate.toString();
            }
        }
        String javaHome = System.getProperty("java.home");
        if (javaHome != null) {
            Path candidate = Path.of(javaHome, "bin", exe);
            if (Files.exists(candidate)) return candidate.toString();
        }
        return tool;
    }

    private String extractClassName(String code) {
        Matcher m = Pattern.compile("(?:public\\s+)?(?:class|interface|enum|record)\\s+(\\w+)").matcher(code);
        return m.find() ? m.group(1) : null;
    }

    private boolean containsBlockedApi(String sourceCode) {
        // javac expands Unicode escapes before parsing comments and strings, so inspect
        // the original input before stripping non-executable text.
        if (UNICODE_ESCAPE.matcher(sourceCode).find()) return true;
        String executable = ChallengeValidator.executableText(sourceCode);
        return BLOCKED_APIS.stream().anyMatch(pattern -> pattern.matcher(executable).find());
    }

    private String cap(String s) {
        if (s == null) return "";
        s = s.strip();
        return s.length() > MAX_OUTPUT_CHARS ? s.substring(0, MAX_OUTPUT_CHARS) + "\n…(truncated)" : s;
    }

    private void deleteQuietly(Path dir) {
        try {
            if (Files.exists(dir)) {
                try (var stream = Files.walk(dir)) {
                    stream.sorted(Comparator.reverseOrder())
                            .forEach(p -> { try { Files.deleteIfExists(p); } catch (IOException ignored) {} });
                }
            }
        } catch (IOException ignored) {}
    }

    private record ProcessResult(int exitCode, String output, boolean timedOut) {}

    /** Drains child output continuously while retaining only the configured cap. */
    private static final class OutputCollector implements Runnable {
        private final InputStream input;
        private final ByteArrayOutputStream captured = new ByteArrayOutputStream();
        private boolean truncated;

        private OutputCollector(InputStream input) {
            this.input = input;
        }

        @Override
        public void run() {
            byte[] buffer = new byte[4096];
            try (input) {
                int read;
                while ((read = input.read(buffer)) != -1) {
                    int remaining = MAX_OUTPUT_CHARS - captured.size();
                    if (remaining > 0) captured.write(buffer, 0, Math.min(read, remaining));
                    if (read > remaining) truncated = true;
                }
            } catch (IOException ignored) {
                // Expected when a timed-out child is killed and its stream closes.
            }
        }

        private String output() {
            String value = captured.toString(StandardCharsets.UTF_8);
            return truncated ? value + "\n…(truncated)" : value;
        }
    }

    /** Result of a compile+run cycle. */
    public record RunResult(boolean compiled, boolean compileSuccess, String stdout, String stderr, boolean timedOut) {
        static RunResult error(String msg) {
            return new RunResult(false, false, "", msg, false);
        }
    }
}
