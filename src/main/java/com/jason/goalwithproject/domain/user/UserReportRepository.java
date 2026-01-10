package com.jason.goalwithproject.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserReportRepository extends JpaRepository<UserReport, Long> {
    boolean existsByReporter_IdAndTarget_Id(Long reporter, Long reportedUser);
}
