package com.genoutfit.api.repository;

import com.genoutfit.api.model.ProgrammaticPage;
import com.genoutfit.api.model.ProgrammaticPageRelated;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProgrammaticPageRelatedRepository extends JpaRepository<ProgrammaticPageRelated, Long> {

    @Query("SELECT p.relatedSlug FROM ProgrammaticPageRelated p WHERE p.page = :page ORDER BY FUNCTION('RAND')")
    List<String> findRandomRelatedSlugs(@Param("page") ProgrammaticPage page, Pageable pageable);
}
