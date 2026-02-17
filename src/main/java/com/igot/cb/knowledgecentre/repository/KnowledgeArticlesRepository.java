package com.igot.cb.knowledgecentre.repository;

import com.igot.cb.knowledgecentre.entity.KnowledgeArticleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KnowledgeArticlesRepository extends JpaRepository<KnowledgeArticleEntity, String> {
}
