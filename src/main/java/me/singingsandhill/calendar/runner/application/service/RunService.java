package me.singingsandhill.calendar.runner.application.service;

import me.singingsandhill.calendar.runner.application.exception.RunNotFoundException;
import me.singingsandhill.calendar.runner.domain.Run;
import me.singingsandhill.calendar.runner.domain.RunCategory;
import me.singingsandhill.calendar.runner.domain.RunRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class RunService {

    private final RunRepository runRepository;

    public RunService(RunRepository runRepository) {
        this.runRepository = runRepository;
    }

    public List<Run> getAllRuns() {
        return runRepository.findAllOrderByDateDesc();
    }

    public Run getRunById(Long id) {
        return runRepository.findById(id)
                .orElseThrow(() -> new RunNotFoundException(id));
    }

    @Transactional
    public Run createRun(LocalDate date, LocalTime time, String location, RunCategory category) {
        Run run = new Run(date, time, location, category);
        return runRepository.save(run);
    }

    @Transactional
    public Run updateRun(Long id, LocalDate date, LocalTime time, String location, RunCategory category) {
        Run existing = runRepository.findById(id)
                .orElseThrow(() -> new RunNotFoundException(id));

        Run updated = new Run(id, date, time, location, category, existing.getCreatedAt());
        return runRepository.save(updated);
    }

    @Transactional
    public void deleteRun(Long id) {
        if (!runRepository.existsById(id)) {
            throw new RunNotFoundException(id);
        }
        runRepository.deleteById(id);
    }
}
