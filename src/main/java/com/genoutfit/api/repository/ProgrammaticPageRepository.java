package com.genoutfit.api.repository;

import com.genoutfit.api.model.ProgrammaticPage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProgrammaticPageRepository extends JpaRepository<ProgrammaticPage, Long> {
    Optional<ProgrammaticPage> findBySlugAndIsActiveTrue(String slug);
    List<ProgrammaticPage> findByPageTypeAndIsActiveTrue(String pageType);
    List<ProgrammaticPage> findBySlugInAndIsActiveTrue(List<String> slugs);
    List<ProgrammaticPage> findTop4ByPageTypeAndIsActiveTrueAndIdNot(String pageType, Long id);

    @Query("SELECT p FROM ProgrammaticPage p WHERE p.pageType = :pageType AND p.isActive = true AND p.id <> :id ORDER BY FUNCTION('RAND')")
    List<ProgrammaticPage> findRandomByPageType(@Param("pageType") String pageType, @Param("id") Long id, Pageable pageable);

}