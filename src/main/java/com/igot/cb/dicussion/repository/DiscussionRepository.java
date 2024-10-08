package com.igot.cb.dicussion.repository;

import com.igot.cb.dicussion.entity.DiscussionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DiscussionRepository extends JpaRepository<DiscussionEntity, String> {
}
