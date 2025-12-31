package me.singingsandhill.calendar.datedate.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import me.singingsandhill.calendar.datedate.application.exception.DuplicateMenuException;
import me.singingsandhill.calendar.datedate.application.exception.MenuNotFoundException;
import me.singingsandhill.calendar.datedate.application.exception.ScheduleNotFoundException;
import me.singingsandhill.calendar.datedate.domain.menu.Menu;
import me.singingsandhill.calendar.datedate.domain.menu.MenuRepository;
import me.singingsandhill.calendar.datedate.domain.schedule.ScheduleRepository;

@Service
@Transactional(readOnly = true)
public class MenuService {

    private final MenuRepository menuRepository;
    private final ScheduleRepository scheduleRepository;

    public MenuService(MenuRepository menuRepository,
                       ScheduleRepository scheduleRepository) {
        this.menuRepository = menuRepository;
        this.scheduleRepository = scheduleRepository;
    }

    public Menu getMenu(Long menuId) {
        return menuRepository.findById(menuId)
                .orElseThrow(() -> new MenuNotFoundException(menuId));
    }

    public List<Menu> getMenusByScheduleId(Long scheduleId) {
        return menuRepository.findAllByScheduleId(scheduleId);
    }

    @Transactional
    public Menu addMenu(Long scheduleId, String name, String url) {
        scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ScheduleNotFoundException(scheduleId));

        if (menuRepository.existsByScheduleIdAndName(scheduleId, name)) {
            throw new DuplicateMenuException(name);
        }

        Menu menu = new Menu(scheduleId, name, url);
        return menuRepository.save(menu);
    }

    @Transactional
    public void deleteMenu(Long menuId) {
        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new MenuNotFoundException(menuId));
        menuRepository.delete(menu);
    }

    @Transactional
    public Menu vote(Long menuId, String voterName) {
        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new MenuNotFoundException(menuId));

        menu.addVote(voterName);
        return menuRepository.save(menu);
    }

    @Transactional
    public Menu unvote(Long menuId, String voterName) {
        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new MenuNotFoundException(menuId));

        menu.removeVote(voterName);
        return menuRepository.save(menu);
    }
}
