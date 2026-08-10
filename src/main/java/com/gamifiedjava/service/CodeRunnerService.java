package com.gamifiedjava.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
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
    private final boolean executionAllowed;

    public CodeRunnerService(
            @Value("${code.runner.execution-enabled:false}") boolean executionEnabled,
            @Value("${server.address:127.0.0.1}") String bindAddress) {
        this.executionAllowed = executionEnabled && isLoopback(bindAddress);
    }

    public RunResult run(String sourceCode) {
        if (sourceCode == null || sourceCode.isBlank()) {
            return RunResult.error("No code submitted.");
        }
        if (sourceCode.length() > MAX_SOURCE_CHARS) {
            return new RunResult(true, false, "", "Source code too large (max " + MAX_SOURCE_CHARS + " chars).", false);
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
                        "Code compiled successfully. Execution is disabled on public deployments.", false);
            }

            // --- run (memory-capped JVM: heap 256m, stack 16m; full-sandbox
            //     isolation requires a container — see AGENTS.md) ---
            String java = findTool("java");
            ProcessResult exec = exec(workDir, java,
                    "-Xmx256m", "-Xss16m", "-Djava.awt.headless=true", "-Dfile.encoding=UTF-8",
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
            boolean finished = p.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                p.destroyForcibly();
                String partial = new String(p.getInputStream().readAllBytes());
                return new ProcessResult(-1, partial, true);
            }
            String output = new String(p.getInputStream().readAllBytes());
            return new ProcessResult(p.exitValue(), output, false);
        } catch (IOException | InterruptedException e) {
            return new ProcessResult(-1, "Process error: could not start the runner.", false);
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

    private boolean isLoopback(String address) {
        if (address == null) return false;
        String value = address.strip().toLowerCase();
        return "127.0.0.1".equals(value) || "localhost".equals(value)
                || "::1".equals(value) || "0:0:0:0:0:0:0:1".equals(value);
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

    /** Result of a compile+run cycle. */
    public record RunResult(boolean compiled, boolean compileSuccess, String stdout, String stderr, boolean timedOut) {
        static RunResult error(String msg) {
            return new RunResult(false, false, "", msg, false);
        }
    }
}
