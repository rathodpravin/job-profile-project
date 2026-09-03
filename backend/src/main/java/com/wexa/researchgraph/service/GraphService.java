package com.wexa.researchgraph.service;

import com.wexa.researchgraph.repository.GraphRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class GraphService {

    private final GraphRepository repository;

    public GraphService(GraphRepository repository) {
        this.repository = repository;
    }

    public List<Map<String, Object>> listAuthors() {
        return repository.listAuthors();
    }

    public List<Map<String, Object>> listPapers() {
        return repository.listPapers();
    }

    public List<Map<String, Object>> listConcepts() {
        return repository.listConcepts();
    }

    public List<Map<String, Object>> conceptFootprint(String authorId) {
        return repository.conceptFootprint(authorId);
    }

    public Map<String, Object> citationPath(String fromPaperId, String toPaperId) {
        return repository.citationPath(fromPaperId, toPaperId);
    }

    public List<Map<String, Object>> similarAuthors(String authorId) {
        return repository.similarAuthors(authorId);
    }

    public List<Map<String, Object>> influenceNetwork(String authorId) {
        return repository.influenceNetwork(authorId);
    }
}
