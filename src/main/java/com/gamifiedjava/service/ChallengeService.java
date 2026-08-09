package com.gamifiedjava.service;

import com.gamifiedjava.model.ChallengeSubmission;
import com.gamifiedjava.model.CourseModule;
import com.gamifiedjava.model.ModuleProgress;
import com.gamifiedjava.repository.ChallengeSubmissionRepository;
import com.gamifiedjava.repository.ModuleProgressRepository;
import com.gamifiedjava.repository.ModuleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ChallengeService {

    private final ChallengeSubmissionRepository submissionRepository;
    private final ModuleRepository moduleRepository;
    private final ModuleProgressRepository progressRepository;
    private final OllamaService ollamaService;
    private final GamificationService gamificationService;

    private static final String WORK_DIR = System.getProperty("java.io.tmpdir") + "/java-challenges/";

    public ChallengeService(ChallengeSubmissionRepository submissionRepository,
                            ModuleRepository moduleRepository,
                            ModuleProgressRepository progressRepository,
                            OllamaService ollamaService,
                            GamificationService gamificationService) {
        this.submissionRepository = submissionRepository;
        this.moduleRepository = moduleRepository;
        this.progressRepository = progressRepository;
        this.ollamaService = ollamaService;
        this.gamificationService = gamificationService;
    }

    @Transactional
    public ChallengeResult submit(Integer moduleId, String sourceCode) {
        CourseModule mod = moduleRepository.findById(moduleId).orElse(null);
        if (mod == null) {
            return new ChallengeResult(false, "Module not found", "", null, false);
        }

        String className = extractClassName(sourceCode);
        if (className == null) className = "Challenge";

        String sessionId = UUID.randomUUID().toString().substring(0, 8);
        Path workDir = Path.of(WORK_DIR, sessionId);
        Path javaFile = workDir.resolve(className + ".java");

        CompileResult compileResult;
        String aiFeedback = null;
        Integer aiScore = null;

        try {
            Files.createDirectories(workDir);
            Files.writeString(javaFile, sourceCode);
            compileResult = compile(javaFile, workDir);
        } catch (IOException e) {
            compileResult = new CompileResult(false, "File error: could not prepare the workspace.", "");
        } finally {
            try {
                if (Files.exists(workDir)) {
                    try (var stream = Files.walk(workDir)) {
                        stream.sorted(java.util.Comparator.reverseOrder())
                                .forEach(p -> { try { Files.deleteIfExists(p); } catch (IOException ignored) {} });
                    }
                }
            } catch (IOException ignored) {}
        }

        boolean passed = false;

        if (compileResult.success()) {
            try {
                aiFeedback = ollamaService.gradeCode(sourceCode, moduleId);
                aiScore = extractScore(aiFeedback);
                passed = aiScore != null && aiScore >= 70;
            } catch (Exception e) {
                // Fail-closed: an unreachable AI reviewer must NOT auto-pass a challenge.
                aiFeedback = "Could not get AI review. Please try submitting again in a moment.";
                passed = false;
            }
        }

        ChallengeSubmission submission = new ChallengeSubmission();
        submission.setModule(mod);
        submission.setSourceCode(sourceCode);
        submission.setCompileOutput(compileResult.output());
        submission.setCompileSuccess(compileResult.success());
        submission.setAiFeedback(aiFeedback);
        submission.setAiScore(aiScore);
        submission.setPassed(passed);
        submissionRepository.save(submission);

        ModuleProgress prog = progressRepository.findByModuleId(moduleId).orElse(null);
        if (prog != null) {
            prog.setChallengeAttempts(prog.getChallengeAttempts() + 1);
            if (passed) {
                prog.setChallengePassed(true);
                if ("challenge_ready".equals(prog.getStatus())) {
                    gamificationService.addXp("challenge_passed", 150, "Passed code challenge for module " + moduleId);
                    if (prog.getChallengeAttempts() >= 3) {
                        gamificationService.unlockIf("Persistent", true);
                    }
                }
            }
            prog.setUpdatedAt(java.time.LocalDateTime.now());
            progressRepository.save(prog);
        }

        List<ChallengeSubmission> attempts = submissionRepository.findByModuleIdOrderBySubmittedAtDesc(moduleId);
        long passedCount = attempts.stream().filter(ChallengeSubmission::getPassed).count();
        if (passedCount >= 5) {
            gamificationService.unlockIf("Bug Hunter", true);
        }

        return new ChallengeResult(compileResult.success(), compileResult.output(), aiFeedback, aiScore, passed);
    }

    public List<ChallengeSubmission> getSubmissions(Integer moduleId) {
        return submissionRepository.findByModuleIdOrderBySubmittedAtDesc(moduleId);
    }

    private CompileResult compile(Path javaFile, Path workDir) {
        try {
            String javacPath = findJavac();
            if (javacPath == null) {
                return new CompileResult(false, "javac not found on system PATH", "");
            }

            ProcessBuilder pb = new ProcessBuilder(javacPath, javaFile.getFileName().toString());
            pb.directory(workDir.toFile());
            pb.redirectErrorStream(true);

            Process process = pb.start();
            boolean finished = process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return new CompileResult(false, "Compilation timed out.", "");
            }

            String output = new String(process.getInputStream().readAllBytes());
            int exitCode = process.exitValue();

            return new CompileResult(exitCode == 0, output, output);
        } catch (Exception e) {
            return new CompileResult(false, "Compilation error: could not complete compilation.", "");
        }
    }

    private String findJavac() {
        String pathEnv = System.getenv("PATH");
        if (pathEnv != null) {
            for (String dir : pathEnv.split(Pattern.quote(File.pathSeparator))) {
                Path javacPath = Path.of(dir, "javac.exe");
                if (Files.exists(javacPath)) return javacPath.toString();
                javacPath = Path.of(dir, "javac");
                if (Files.exists(javacPath)) return javacPath.toString();
            }
        }

        String javaHome = System.getProperty("java.home");
        if (javaHome != null) {
            Path javacPath = Path.of(javaHome, "bin", "javac.exe");
            if (Files.exists(javacPath)) return javacPath.toString();
            javacPath = Path.of(javaHome, "..", "bin", "javac.exe");
            if (Files.exists(javacPath)) return javacPath.toString().replace("/..", "");
        }

        return "javac";
    }

    private String extractClassName(String code) {
        Matcher m = Pattern.compile("(?:public\\s+)?(?:class|interface|enum)\\s+(\\w+)").matcher(code);
        return m.find() ? m.group(1) : null;
    }

    private Integer extractScore(String feedback) {
        if (feedback == null) return null;
        Matcher m = Pattern.compile("SCORE:\\s*(\\d+)").matcher(feedback);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    public record CompileResult(boolean success, String output, String errors) {}
    public record ChallengeResult(boolean compileSuccess, String compileOutput, String aiFeedback, Integer aiScore, boolean passed) {}
}
