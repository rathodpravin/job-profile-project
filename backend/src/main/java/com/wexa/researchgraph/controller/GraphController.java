package com.wexa.researchgraph.controller;

import com.wexa.researchgraph.service.GraphService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class GraphController {

    private final GraphService graphService;

    public GraphController(GraphService graphService) {
        this.graphService = graphService;
    }

    @GetMapping("/authors")
    public List<Map<String, Object>> authors() {
        return graphService.listAuthors();
    }

    @GetMapping("/papers")
    public List<Map<String, Object>> papers() {
        return graphService.listPapers();
    }

    @GetMapping("/concepts")
    public List<Map<String, Object>> concepts() {
        return graphService.listConcepts();
    }

    @GetMapping("/authors/{authorId}/concept-footprint")
    public List<Map<String, Object>> conceptFootprint(@PathVariable String authorId) {
        return graphService.conceptFootprint(authorId);
    }

    @GetMapping("/authors/{authorId}/similar")
    public List<Map<String, Object>> similar(@PathVariable String authorId) {
        return graphService.similarAuthors(authorId);
    }

    @GetMapping("/authors/{authorId}/influence")
    public List<Map<String, Object>> influence(@PathVariable String authorId) {
        return graphService.influenceNetwork(authorId);
    }

    @GetMapping("/papers/citation-path")
    public Map<String, Object> citationPath(@RequestParam String from, @RequestParam String to) {
        return graphService.citationPath(from, to);
    }
}
