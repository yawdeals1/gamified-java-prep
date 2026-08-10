package com.gamifiedjava.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gamifiedjava.model.AppState;
import com.gamifiedjava.model.CourseModule;
import com.gamifiedjava.model.ModuleProgress;
import com.gamifiedjava.service.GamificationService;
import com.gamifiedjava.service.LessonStepService;
import com.gamifiedjava.service.ModuleService;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ConcurrentModel;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LessonControllerTest {

    @Test
    void continueTargetUnlocksWhenPreviousModuleIsMastered() {
        ModuleService modules = mock(ModuleService.class);
        LessonStepService steps = mock(LessonStepService.class);
        GamificationService gamification = mock(GamificationService.class);
        LessonController controller = new LessonController(modules, steps, gamification, new ObjectMapper());

        CourseModule previous = module(1, 1, "variables-and-data-types");
        CourseModule target = module(2, 2, "classes-and-objects");
        ModuleProgress locked = new ModuleProgress(target);
        ModuleProgress unlocked = new ModuleProgress(target);
        unlocked.setStatus("available");

        when(modules.getBySlug(target.getSlug())).thenReturn(target);
        when(modules.getProgress(target.getId())).thenReturn(locked);
        when(modules.getAllModules()).thenReturn(List.of(previous, target));
        when(steps.masteryPercent(previous.getId())).thenReturn(100);
        when(modules.unlockModule(target.getId())).thenReturn(unlocked);
        when(steps.getSteps(target.getId())).thenReturn(List.of());
        when(steps.completedStepIds(target.getId())).thenReturn(Set.of());
        when(steps.toClientSteps(List.of(), Set.of())).thenReturn(List.of());
        when(gamification.getState()).thenReturn(new AppState());

        String view = controller.lesson(target.getSlug(), new ConcurrentModel(), new RedirectAttributesModelMap());

        assertEquals("learn", view);
        verify(modules).unlockModule(target.getId());
    }

    private CourseModule module(int id, int order, String slug) {
        CourseModule module = new CourseModule();
        module.setId(id);
        module.setOrderIndex(order);
        module.setSlug(slug);
        module.setTitle(slug);
        return module;
    }
}
