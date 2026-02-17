package com.igot.cb.knowledgecentre.repository;

import com.igot.cb.knowledgecentre.entity.KnowledgeSubCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KnowledgeSubCategoryRepository extends JpaRepository<KnowledgeSubCategoryEntity, String> {
}
