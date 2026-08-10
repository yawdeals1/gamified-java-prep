package com.gamifiedjava.repository;

import com.gamifiedjava.model.CourseModule;
import com.gamifiedjava.model.ModuleProgress;
import com.gamifiedjava.studio.StudioClient;
import com.gamifiedjava.auth.CurrentUserContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepositoryPerformanceTest {

    @Mock
    private StudioClient client;

    @Mock
    private ModuleRepository moduleRepository;

    @Test
    void countsAllModuleStepsFromOneBulkResponse() {
        when(client.list("lesson_step", null, 10000)).thenReturn(List.of(
                Map.of("module_id", 1),
                Map.of("module_id", 1),
                Map.of("module_id", 2)
        ));

        var repository = new LessonStepRepository(client, moduleRepository);

        assertThat(repository.countByModule()).containsExactlyInAnyOrderEntriesOf(
                Map.of(1, 2L, 2, 1L));
        verify(client).list("lesson_step", null, 10000);
    }

    @Test
    void countsCompletedProgressFromOneBulkResponse() {
        Map<String, String> owner = Map.of("auth_user_id", "user-1");
        when(client.list("step_progress", owner, 10000)).thenReturn(List.of(
                Map.of("id", 1, "step_id", 10, "module_id", 1,
                        "auth_user_id", "user-1", "attempts", 1, "completed_at", "2026-08-09T12:00:00"),
                Map.of("id", 2, "step_id", 11, "module_id", 1, "auth_user_id", "user-1", "attempts", 1),
                Map.of("id", 3, "step_id", 20, "module_id", 2,
                        "auth_user_id", "user-1", "attempts", 2, "completed_at", "2026-08-09T12:01:00")
        ));

        var users = new CurrentUserContext();
        users.set("user-1");
        var repository = new StepProgressRepository(client, users);

        assertThat(repository.completedCountByModule()).containsExactlyInAnyOrderEntriesOf(
                Map.of(1, 1L, 2, 1L));
        verify(client).list("step_progress", owner, 10000);
    }

    @Test
    void mapsForeignKeyToReferenceWithoutFetchingModuleAgain() {
        CourseModule reference = new CourseModule();
        reference.setId(1);
        when(moduleRepository.reference(1)).thenReturn(reference);
        Map<String, String> owner = Map.of("auth_user_id", "user-1");
        when(client.list("module_progress", owner, 10000)).thenReturn(List.of(
                Map.of("id", 7, "module_id", 1, "auth_user_id", "user-1", "status", "available")
        ));

        var users = new CurrentUserContext();
        users.set("user-1");
        var repository = new ModuleProgressRepository(client, moduleRepository, users);
        List<ModuleProgress> progress = repository.findAllByOrderByIdAsc();

        assertThat(progress).singleElement()
                .extracting(item -> item.getModule().getId())
                .isEqualTo(1);
        verify(moduleRepository).reference(1);
        verify(moduleRepository, never()).findById(any());
    }
}
