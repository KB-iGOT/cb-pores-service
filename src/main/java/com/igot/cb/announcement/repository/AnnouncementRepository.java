package com.igot.cb.announcement.repository;

import com.igot.cb.announcement.entity.AnnouncementEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnnouncementRepository extends JpaRepository<AnnouncementEntity, String> {

}