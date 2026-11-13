package com.opticsappbg.opticsapp.activity.service;

import com.opticsappbg.opticsapp.activity.model.ActivityAction;
import com.opticsappbg.opticsapp.activity.model.ActivityLog;
import com.opticsappbg.opticsapp.activity.repository.ActivityLogRepository;
import com.opticsappbg.opticsapp.security.AuthenticationDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;

    @Transactional
    public void log(
            ActivityAction action,
            String entityType,
            Long entityId,
            String description
    ) {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        Long userId = null;
        String username = "SYSTEM";
        String fullName = null;

        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof AuthenticationDetails user) {

            userId = user.getUserId();
            username = user.getUsername();
            fullName = user.getFullName();
        }

        ActivityLog activityLog = ActivityLog.builder()
                .userId(userId)
                .username(username)
                .fullName(fullName)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .description(description)
                .createdOn(LocalDateTime.now())
                .build();

        activityLogRepository.save(activityLog);
        activityLogRepository.deleteOlderThanLatest500();
    }

    @Transactional(readOnly = true)
    public List<ActivityLog> findLogs(
            Long userId,
            LocalDateTime dateFrom,
            LocalDateTime dateTo,
            ActivityAction action,
            String entityType
    ) {
        Specification<ActivityLog> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (userId != null) {
                predicates.add(
                        criteriaBuilder.equal(root.get("userId"), userId)
                );
            }

            if (dateFrom != null) {
                predicates.add(
                        criteriaBuilder.greaterThanOrEqualTo(
                                root.get("createdOn"), dateFrom
                        )
                );
            }

            if (dateTo != null) {
                predicates.add(
                        criteriaBuilder.lessThanOrEqualTo(
                                root.get("createdOn"), dateTo
                        )
                );
            }

            if (action != null) {
                predicates.add(
                        criteriaBuilder.equal(root.get("action"), action)
                );
            }

            if (entityType != null && !entityType.isBlank()) {
                predicates.add(
                        criteriaBuilder.equal(
                                root.get("entityType"),
                                entityType.trim().toUpperCase(Locale.ROOT)
                        )
                );
            }

            query.orderBy(
                    criteriaBuilder.desc(root.get("createdOn")),
                    criteriaBuilder.desc(root.get("id"))
            );

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        return activityLogRepository.findAll(specification);
    }
}
