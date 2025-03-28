package com.genoutfit.api.service;


import com.genoutfit.api.model.ProgrammaticPage;
import com.genoutfit.api.repository.ProgrammaticPageRelatedRepository;
import com.genoutfit.api.repository.ProgrammaticPageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProgrammaticPageService {
    @Autowired
    private  ProgrammaticPageRepository programmaticPageRepository;

    @Autowired
    private ProgrammaticPageRelatedRepository programmaticPageRelatedRepository;


    public Optional<ProgrammaticPage> getPageBySlug(String slug) {
        return programmaticPageRepository.findBySlugAndIsActiveTrue(slug);
    }

    public List<ProgrammaticPage> getPagesByType(String pageType) {
        return programmaticPageRepository.findByPageTypeAndIsActiveTrue(pageType);
    }

    public List<ProgrammaticPage> getRelatedPages(ProgrammaticPage page) {
        Pageable pageable = PageRequest.of(0, 6); // Limit to 6 pages
        List<String> relatedSlugs = programmaticPageRelatedRepository.findRandomRelatedSlugs(page, pageable);

        if (relatedSlugs.isEmpty()) {
            return getRandomFallbackPages(page);
        }

        return programmaticPageRepository.findBySlugInAndIsActiveTrue(relatedSlugs);
    }
    public List<ProgrammaticPage> getRandomFallbackPages(ProgrammaticPage page) {
        Pageable pageable = PageRequest.of(0, 6);
        return programmaticPageRepository.findRandomByPageType(page.getPageType(), page.getId(), pageable);
    }

}