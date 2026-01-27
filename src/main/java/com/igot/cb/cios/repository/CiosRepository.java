package com.igot.cb.cios.repository;



import com.igot.cb.cios.entity.CiosContentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;


@Repository
public interface CiosRepository extends JpaRepository<CiosContentEntity,String> {
    Optional<CiosContentEntity> findByExternalIdAndPartnerId(String externalId,String PartnerId);
    Optional<CiosContentEntity> findByExternalId(String externalId);

    Optional<CiosContentEntity> findByContentIdAndIsActive(String contentId, boolean b);

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE cios_content_entity
            SET cios_data = jsonb_set(cios_data,'{content,contentPartner,isActive}',to_jsonb(:isActive),true),is_active = :isActive,last_updated_on = now() WHERE content_id IN (:contentIds)
            """, nativeQuery = true)
    int bulkUpdateIsActiveAndJson(@Param("contentIds") List<String> contentIds, @Param("isActive") boolean isActive);
}