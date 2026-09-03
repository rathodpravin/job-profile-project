package com.wexa.researchgraph.repository;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;
import org.neo4j.driver.types.Node;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

/**
 * All Cypher for the app lives here. Every query is parameterised through
 * the driver's Values.parameters(...) - there is no string concatenation
 * of user input into Cypher anywhere in this class.
 *
 * Graph shape:
 *   (Author)-[:WROTE]->(Paper)
 *   (Paper)-[:USES]->(Technology)
 *   (Technology)-[:RELATED_TO]->(Concept)
 *   (Author)-[:WORKS_AT]->(Institution)
 *   (Paper)-[:CITES]->(Paper)
 */
@Repository
public class GraphRepository {

    private final Driver driver;
    private final String database;

    public GraphRepository(Driver driver, @Value("${cognodb.database}") String database) {
        this.driver = driver;
        this.database = database;
    }

    private Session session() {
        return driver.session(org.neo4j.driver.SessionConfig.forDatabase(database));
    }

    // ---------------------------------------------------------------
    // Simple listings
    // ---------------------------------------------------------------

    public List<Map<String, Object>> listAuthors() {
        String cypher = """
                MATCH (a:Author)
                OPTIONAL MATCH (a)-[:WROTE]->(p:Paper)
                OPTIONAL MATCH (a)-[:WORKS_AT]->(i:Institution)
                RETURN a.id AS id, a.name AS name, i.name AS institution, count(DISTINCT p) AS paperCount
                ORDER BY a.name
                """;
        try (Session s = session()) {
            return s.run(cypher).list(this::recordToMap);
        }
    }

    public List<Map<String, Object>> listPapers() {
        String cypher = """
                MATCH (p:Paper)<-[:WROTE]-(a:Author)
                WITH p, collect(a.name) AS authors
                RETURN p.id AS id, p.title AS title, p.year AS year, authors
                ORDER BY p.year DESC, p.title
                """;
        try (Session s = session()) {
            return s.run(cypher).list(this::recordToMap);
        }
    }

    public List<Map<String, Object>> listConcepts() {
        String cypher = "MATCH (c:Concept) RETURN c.id AS id, c.name AS name ORDER BY c.name";
        try (Session s = session()) {
            return s.run(cypher).list(this::recordToMap);
        }
    }

    // ---------------------------------------------------------------
    // Query 1: multi-hop concept footprint
    // Author -[:WROTE]-> Paper -[:USES]-> Technology -[:RELATED_TO]-> Concept
    // "What research fields has this author touched, via the technologies
    // their papers actually use?" - a genuine 3-hop traversal.
    // ---------------------------------------------------------------

    public List<Map<String, Object>> conceptFootprint(String authorId) {
        String cypher = """
                MATCH (a:Author {id: $authorId})-[:WROTE]->(p:Paper)-[:USES]->(t:Technology)-[:RELATED_TO]->(c:Concept)
                WITH c, collect(DISTINCT t.name) AS viaTechnologies, count(DISTINCT p) AS paperCount
                RETURN c.id AS conceptId, c.name AS conceptName, paperCount, viaTechnologies
                ORDER BY paperCount DESC, c.name
                """;
        try (Session s = session()) {
            return s.run(cypher, Values.parameters("authorId", authorId)).list(this::recordToMap);
        }
    }

    // ---------------------------------------------------------------
    // Query 2: shortest citation path between two papers.
    // Variable-length CITES traversal - the classic case a relational
    // schema needs a recursive CTE (or app-side BFS) to express.
    // ---------------------------------------------------------------

    public Map<String, Object> citationPath(String fromPaperId, String toPaperId) {
        String cypher = """
                MATCH (a:Paper {id: $fromId}), (b:Paper {id: $toId})
                MATCH path = shortestPath((a)-[:CITES*1..6]-(b))
                RETURN [n IN nodes(path) | n.title] AS chain, length(path) AS hops
                """;
        try (Session s = session()) {
            var result = s.run(cypher, Values.parameters("fromId", fromPaperId, "toId", toPaperId));
            if (result.hasNext()) {
                return recordToMap(result.next());
            }
            return Map.of("chain", List.of(), "hops", -1);
        }
    }

    // ---------------------------------------------------------------
    // Query 3: similar authors - authors whose papers use the most of
    // the same technologies (2-hop-through-a-hop shared-neighbor pattern).
    // ---------------------------------------------------------------

    public List<Map<String, Object>> similarAuthors(String authorId) {
        String cypher = """
                MATCH (a:Author {id: $authorId})-[:WROTE]->(:Paper)-[:USES]->(t:Technology)
                       <-[:USES]-(:Paper)<-[:WROTE]-(other:Author)
                WHERE other.id <> $authorId
                WITH other, collect(DISTINCT t.name) AS sharedTechnologies
                RETURN other.id AS authorId, other.name AS name,
                       size(sharedTechnologies) AS sharedCount, sharedTechnologies
                ORDER BY sharedCount DESC
                LIMIT 10
                """;
        try (Session s = session()) {
            return s.run(cypher, Values.parameters("authorId", authorId)).list(this::recordToMap);
        }
    }

    // ---------------------------------------------------------------
    // Query 4: research influence - authors whose papers cite this
    // author's work, transitively up to 3 citation hops out, with the
    // hop distance ("how many citation links away") for each.
    // ---------------------------------------------------------------

    public List<Map<String, Object>> influenceNetwork(String authorId) {
        String cypher = """
                MATCH (a:Author {id: $authorId})-[:WROTE]->(origin:Paper)
                MATCH path = (origin)<-[:CITES*1..3]-(citingPaper:Paper)
                MATCH (citingPaper)<-[:WROTE]-(citingAuthor:Author)
                WHERE citingAuthor.id <> $authorId
                WITH citingAuthor, citingPaper, min(length(path)) AS hops
                RETURN DISTINCT citingAuthor.id AS authorId, citingAuthor.name AS name,
                       citingPaper.title AS viaPaper, hops
                ORDER BY hops ASC, citingAuthor.name
                LIMIT 15
                """;
        try (Session s = session()) {
            return s.run(cypher, Values.parameters("authorId", authorId)).list(this::recordToMap);
        }
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private Map<String, Object> recordToMap(Record record) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (String key : record.keys()) {
            map.put(key, unwrap(record.get(key).asObject()));
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    private Object unwrap(Object value) {
        if (value instanceof Node node) {
            Map<String, Object> nodeMap = new LinkedHashMap<>(node.asMap());
            nodeMap.put("_labels", node.labels());
            return nodeMap;
        }
        if (value instanceof List<?> list) {
            return list.stream().map(this::unwrap).collect(Collectors.toList());
        }
        return value;
    }
}
