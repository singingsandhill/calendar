package me.singingsandhill.calendar.datedate.application.service;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import me.singingsandhill.calendar.datedate.application.dto.RecapDto;
import me.singingsandhill.calendar.datedate.application.exception.InvalidRecapYearException;
import me.singingsandhill.calendar.datedate.application.exception.UserNotFoundException;
import me.singingsandhill.calendar.datedate.domain.activity.ActivityType;
import me.singingsandhill.calendar.datedate.domain.activity.UserActivity;
import me.singingsandhill.calendar.datedate.domain.activity.UserActivityRepository;
import me.singingsandhill.calendar.datedate.domain.owner.Owner;
import me.singingsandhill.calendar.datedate.domain.owner.OwnerRepository;
import me.singingsandhill.calendar.datedate.domain.participant.Participant;
import me.singingsandhill.calendar.datedate.domain.participant.ParticipantRepository;
import me.singingsandhill.calendar.datedate.domain.schedule.Schedule;
import me.singingsandhill.calendar.datedate.domain.schedule.ScheduleRepository;
import me.singingsandhill.calendar.datedate.domain.schedule.YearMonth;
import me.singingsandhill.calendar.datedate.domain.user.AppUser;
import me.singingsandhill.calendar.datedate.domain.user.AppUserRepository;

/**
 * 연간 recap on-the-fly 집계 (ADR datedate/domain/0005 — 스냅샷 없음).
 * 오너 계열: 내 오너들의 해당 연도 일정. 활동 계열: 내 UserActivity 이벤트.
 */
@Service
@Transactional(readOnly = true)
public class RecapService {

    private static final int MIN_YEAR = 2024;
    private static final int TOP_LIMIT = 3;

    private final AppUserRepository appUserRepository;
    private final OwnerRepository ownerRepository;
    private final ScheduleRepository scheduleRepository;
    private final UserActivityRepository userActivityRepository;
    private final ParticipantRepository participantRepository;
    private final Clock clock;

    public RecapService(AppUserRepository appUserRepository,
                        OwnerRepository ownerRepository,
                        ScheduleRepository scheduleRepository,
                        UserActivityRepository userActivityRepository,
                        ParticipantRepository participantRepository,
                        Clock clock) {
        this.appUserRepository = appUserRepository;
        this.ownerRepository = ownerRepository;
        this.scheduleRepository = scheduleRepository;
        this.userActivityRepository = userActivityRepository;
        this.participantRepository = participantRepository;
        this.clock = clock;
    }

    public RecapDto buildRecap(Long userId, int year) {
        validateYear(year);
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // 오너 계열
        List<Schedule> mySchedules = ownerRepository.findAllByUserId(userId).stream()
                .map(Owner::getOwnerId)
                .flatMap(ownerId -> scheduleRepository.findAllByOwnerId(ownerId).stream())
                .filter(schedule -> schedule.getYear() == year)
                .toList();

        Set<Long> myScheduleIds = mySchedules.stream()
                .map(Schedule::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        int schedulesCreated = mySchedules.size();
        int totalParticipants = mySchedules.stream().mapToInt(Schedule::getParticipantCount).sum();

        Map<DayOfWeek, Integer> weekdayFreq = new HashMap<>();
        Map<Integer, Integer> monthFreq = new HashMap<>();
        Map<String, Integer> companionFreq = new HashMap<>();
        for (Schedule schedule : mySchedules) {
            for (Participant participant : schedule.getParticipants()) {
                companionFreq.merge(participant.getName(), 1, Integer::sum);
                for (Integer index : participant.getSelections()) {
                    if (index == null || index < 1 || index > YearMonth.FIXED_TOTAL_DAYS) {
                        continue;
                    }
                    LocalDate date = schedule.getYearMonth().indexToDate(index);
                    weekdayFreq.merge(date.getDayOfWeek(), 1, Integer::sum);
                    monthFreq.merge(date.getMonthValue(), 1, Integer::sum);
                }
            }
        }

        // 활동 계열
        List<UserActivity> activities = userActivityRepository.findAllByUserIdAndOccurredAtBetween(
                userId,
                LocalDateTime.of(year, 1, 1, 0, 0),
                LocalDateTime.of(year, 12, 31, 23, 59, 59));

        // 미연결 오너에서 로그인 생성한 일정도 집계 (ADR datedate/domain/0005)
        schedulesCreated += (int) activities.stream()
                .filter(activity -> activity.getType() == ActivityType.SCHEDULE_CREATED)
                .map(UserActivity::getScheduleId)
                .filter(Objects::nonNull)
                .distinct()
                .filter(scheduleId -> !myScheduleIds.contains(scheduleId))
                .count();

        List<UserActivity> participations = activities.stream()
                .filter(activity -> activity.getType() == ActivityType.PARTICIPATION)
                .toList();
        int participationCount = (int) participations.stream()
                .map(UserActivity::getScheduleId)
                .filter(Objects::nonNull)
                .distinct()
                .count();
        int daysSelected = participations.stream()
                .map(UserActivity::getTargetId)
                .filter(Objects::nonNull)
                .map(participantRepository::findById)
                .flatMap(java.util.Optional::stream)
                .mapToInt(participant -> participant.getSelections().size())
                .sum();

        List<String> topLocations = topDetails(activities, ActivityType.LOCATION_VOTE);
        List<String> topMenus = topDetails(activities, ActivityType.MENU_VOTE);

        boolean empty = schedulesCreated == 0 && activities.isEmpty();

        return new RecapDto(
                year,
                user.getNickname(),
                schedulesCreated,
                totalParticipants,
                participationCount,
                daysSelected,
                maxKey(weekdayFreq).map(DayOfWeek::name).orElse(null),
                maxKey(monthFreq).orElse(null),
                topLocations,
                topMenus,
                topKeys(companionFreq),
                empty
        );
    }

    private void validateYear(int year) {
        if (year < MIN_YEAR || year > Year.now(clock).getValue()) {
            throw new InvalidRecapYearException(year);
        }
    }

    private List<String> topDetails(List<UserActivity> activities, ActivityType type) {
        Map<String, Integer> freq = new HashMap<>();
        activities.stream()
                .filter(activity -> activity.getType() == type)
                .map(UserActivity::getDetail)
                .filter(Objects::nonNull)
                .forEach(detail -> freq.merge(detail, 1, Integer::sum));
        return topKeys(freq);
    }

    private <K> java.util.Optional<K> maxKey(Map<K, Integer> freq) {
        return freq.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey);
    }

    private <K extends Comparable<K>> List<K> topKeys(Map<K, Integer> freq) {
        return freq.entrySet().stream()
                .sorted(Map.Entry.<K, Integer>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(TOP_LIMIT)
                .map(Map.Entry::getKey)
                .toList();
    }
}
