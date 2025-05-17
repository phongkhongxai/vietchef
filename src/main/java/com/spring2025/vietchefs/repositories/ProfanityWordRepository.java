package com.spring2025.vietchefs.repositories;

import com.spring2025.vietchefs.models.entity.ProfanityWord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProfanityWordRepository extends JpaRepository<ProfanityWord, Long> {
    
    Optional<ProfanityWord> findByWordIgnoreCase(String word);
    
    List<ProfanityWord> findByActiveTrue();
    
    List<ProfanityWord> findByLanguageAndActiveTrue(String language);
    
    @Query("SELECT p.word FROM ProfanityWord p WHERE p.active = true")
    List<String> findAllActiveWords();
    
    boolean existsByWordIgnoreCase(String word);
    
    @Query("SELECT COUNT(p) FROM ProfanityWord p WHERE p.active = true")
    long countActiveWords();
} 