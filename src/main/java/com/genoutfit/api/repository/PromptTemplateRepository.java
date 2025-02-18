package com.genoutfit.api.repository;

import com.genoutfit.api.model.Occasion;
import com.genoutfit.api.model.PromptTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PromptTemplateRepository extends JpaRepository<PromptTemplate, Long> {
    List<PromptTemplate> findByOccasion(Occasion occasion);
}
