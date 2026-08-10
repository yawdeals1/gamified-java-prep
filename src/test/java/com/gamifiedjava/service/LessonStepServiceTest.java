package com.gamifiedjava.service;

import com.gamifiedjava.auth.CurrentUserContext;
import com.gamifiedjava.repository.LessonStepRepository;
import com.gamifiedjava.repository.ModuleProgressRepository;
import com.gamifiedjava.repository.ModuleRepository;
import com.gamifiedjava.repository.StepProgressRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class LessonStepServiceTest {

    @Test
    void startupSeedingDoesNotReadUserScopedProgressWithoutAuthenticatedUser() {
        LessonStepRepository steps = mock(LessonStepRepository.class);
        StepProgressRepository progress = mock(StepProgressRepository.class);
        ModuleRepository modules = mock(ModuleRepository.class);
        when(modules.findAllByOrderByOrderIndexAsc()).thenReturn(List.of());

        LessonStepService service = new LessonStepService(
                steps,
                progress,
                modules,
                mock(CodeRunnerService.class),
                mock(GamificationService.class),
                mock(ModuleProgressRepository.class),
                new CurrentUserContext(),
                mock(LessonCodeValidator.class));

        service.seedSteps();

        verifyNoInteractions(progress);
    }
}
