package com.wexa.researchgraph.seed;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Loads realistic seed data into CognoDB. Run with:
 *   mvn spring-boot:run -Dspring-boot.run.profiles=seed
 *
 * All writes go through parameterised UNWIND queries - no string
 * concatenation, and re-running is idempotent thanks to MERGE.
 */
@Component
@Profile("seed")
public class SeedRunner implements CommandLineRunner {

    private final Driver driver;
    private final String database;

    public SeedRunner(Driver driver, @Value("${cognodb.database}") String database) {
        this.driver = driver;
        this.database = database;
    }

    @Override
    public void run(String... args) {
        try (Session session = driver.session(org.neo4j.driver.SessionConfig.forDatabase(database))) {
            System.out.println("Creating constraints...");
            createConstraints(session);

            System.out.println("Loading institutions...");
            session.run("""
                    UNWIND $rows AS row
                    MERGE (i:Institution {id: row.id}) SET i.name = row.name, i.country = row.country
                    """, Values.parameters("rows", institutions()));

            System.out.println("Loading concepts...");
            session.run("""
                    UNWIND $rows AS row
                    MERGE (c:Concept {id: row.id}) SET c.name = row.name
                    """, Values.parameters("rows", concepts()));

            System.out.println("Loading technologies...");
            session.run("""
                    UNWIND $rows AS row
                    MERGE (t:Technology {id: row.id}) SET t.name = row.name
                    """, Values.parameters("rows", technologies()));

            System.out.println("Loading technology -> concept relations (RELATED_TO)...");
            session.run("""
                    UNWIND $rows AS row
                    MATCH (t:Technology {id: row.techId}), (c:Concept {id: row.conceptId})
                    MERGE (t)-[:RELATED_TO]->(c)
                    """, Values.parameters("rows", technologyConceptLinks()));

            System.out.println("Loading authors...");
            session.run("""
                    UNWIND $rows AS row
                    MERGE (a:Author {id: row.id}) SET a.name = row.name
                    WITH a, row
                    MATCH (i:Institution {id: row.institutionId})
                    MERGE (a)-[:WORKS_AT]->(i)
                    """, Values.parameters("rows", authors()));

            System.out.println("Loading papers...");
            session.run("""
                    UNWIND $rows AS row
                    MERGE (p:Paper {id: row.id})
                    SET p.title = row.title, p.year = row.year, p.abstract = row.abstract
                    """, Values.parameters("rows", papers()));

            System.out.println("Loading authorship (WROTE)...");
            session.run("""
                    UNWIND $rows AS row
                    MATCH (a:Author {id: row.authorId}), (p:Paper {id: row.paperId})
                    MERGE (a)-[:WROTE]->(p)
                    """, Values.parameters("rows", authorships()));

            System.out.println("Loading paper -> technology usage (USES)...");
            session.run("""
                    UNWIND $rows AS row
                    MATCH (p:Paper {id: row.paperId}), (t:Technology {id: row.techId})
                    MERGE (p)-[:USES]->(t)
                    """, Values.parameters("rows", paperTechnology()));

            System.out.println("Loading citations (CITES)...");
            session.run("""
                    UNWIND $rows AS row
                    MATCH (citing:Paper {id: row.fromId}), (cited:Paper {id: row.toId})
                    MERGE (citing)-[:CITES]->(cited)
                    """, Values.parameters("rows", citations()));

            System.out.println("Seed complete.");
        }
    }

    private void createConstraints(Session session) {
        session.run("CREATE CONSTRAINT author_id IF NOT EXISTS FOR (a:Author) REQUIRE a.id IS UNIQUE");
        session.run("CREATE CONSTRAINT paper_id IF NOT EXISTS FOR (p:Paper) REQUIRE p.id IS UNIQUE");
        session.run("CREATE CONSTRAINT concept_id IF NOT EXISTS FOR (c:Concept) REQUIRE c.id IS UNIQUE");
        session.run("CREATE CONSTRAINT tech_id IF NOT EXISTS FOR (t:Technology) REQUIRE t.id IS UNIQUE");
        session.run("CREATE CONSTRAINT institution_id IF NOT EXISTS FOR (i:Institution) REQUIRE i.id IS UNIQUE");
    }

    // ---------------- seed data ----------------

    private List<Map<String, Object>> institutions() {
        String[][] rows = {
                {"inst_orion", "Orion Institute of Technology", "USA"},
                {"inst_meridian", "Meridian University", "UK"},
                {"inst_sakura", "Sakura Research Lab", "Japan"},
                {"inst_delta", "Deltapoint AI Lab", "Canada"},
                {"inst_ashoka", "Ashoka Institute of Computing", "India"},
        };
        return toMaps(rows, "id", "name", "country");
    }

    private List<Map<String, Object>> concepts() {
        String[][] rows = {
                {"con_nlp", "Natural Language Processing"},
                {"con_vision", "Computer Vision"},
                {"con_robotics", "Robotics"},
                {"con_health", "Healthcare AI"},
                {"con_recsys", "Recommendation Systems"},
                {"con_security", "Cybersecurity"},
                {"con_drugdiscovery", "Drug Discovery"},
                {"con_autonomy", "Autonomous Systems"},
        };
        return toMaps(rows, "id", "name");
    }

    private List<Map<String, Object>> technologies() {
        String[][] rows = {
                {"tech_transformer", "Transformer Networks"},
                {"tech_gnn", "Graph Neural Networks"},
                {"tech_rl", "Reinforcement Learning"},
                {"tech_federated", "Federated Learning"},
                {"tech_diffusion", "Diffusion Models"},
                {"tech_cnn", "Convolutional Neural Networks"},
                {"tech_lstm", "LSTM Networks"},
                {"tech_attention", "Attention Mechanisms"},
                {"tech_kg", "Knowledge Graphs"},
                {"tech_contrastive", "Contrastive Learning"},
        };
        return toMaps(rows, "id", "name");
    }

    private List<Map<String, Object>> technologyConceptLinks() {
        String[][] rows = {
                {"tech_transformer", "con_nlp"}, {"tech_attention", "con_nlp"}, {"tech_lstm", "con_nlp"},
                {"tech_cnn", "con_vision"}, {"tech_diffusion", "con_vision"}, {"tech_contrastive", "con_vision"},
                {"tech_rl", "con_robotics"}, {"tech_rl", "con_autonomy"}, {"tech_gnn", "con_autonomy"},
                {"tech_gnn", "con_drugdiscovery"}, {"tech_kg", "con_drugdiscovery"}, {"tech_kg", "con_health"},
                {"tech_transformer", "con_health"}, {"tech_gnn", "con_recsys"}, {"tech_contrastive", "con_recsys"},
                {"tech_federated", "con_security"}, {"tech_federated", "con_health"}, {"tech_kg", "con_recsys"},
        };
        return toMaps(rows, "techId", "conceptId");
    }

    private List<Map<String, Object>> authors() {
        String[][] rows = {
                {"auth_maria", "Maria Chen", "inst_orion"},
                {"auth_kenji", "Kenji Watanabe", "inst_sakura"},
                {"auth_amara", "Amara Okonkwo", "inst_delta"},
                {"auth_ravi", "Ravi Iyer", "inst_ashoka"},
                {"auth_lucas", "Lucas Fontaine", "inst_meridian"},
                {"auth_nina", "Nina Petrova", "inst_orion"},
                {"auth_dev", "Dev Sharma", "inst_ashoka"},
                {"auth_hana", "Hana Kobayashi", "inst_sakura"},
                {"auth_tom", "Tom Bradley", "inst_meridian"},
                {"auth_zoe", "Zoe Marchetti", "inst_delta"},
        };
        return toMaps(rows, "id", "name", "institutionId");
    }

    private List<Map<String, Object>> papers() {
        String[][] rows = {
                {"p1", "Attention Is What You Need for Long Documents", "2019", "Extending attention mechanisms to long-context document understanding."},
                {"p2", "Graph Neural Networks for Molecular Property Prediction", "2020", "Using GNNs to predict molecular properties for drug candidates."},
                {"p3", "Federated Learning for Privacy-Preserving Healthcare Models", "2021", "Training clinical models across hospitals without sharing patient data."},
                {"p4", "Reinforcement Learning for Robotic Manipulation", "2019", "RL policies for dexterous robotic arm control."},
                {"p5", "Knowledge Graphs for Explainable Drug Discovery", "2022", "Combining KGs with GNNs to make drug discovery predictions interpretable."},
                {"p6", "Contrastive Learning for Recommendation Systems", "2021", "Self-supervised contrastive objectives for recommender embeddings."},
                {"p7", "Diffusion Models for High-Fidelity Image Synthesis", "2022", "Denoising diffusion probabilistic models applied to image generation."},
                {"p8", "Transformers Meet Knowledge Graphs in Clinical NLP", "2022", "Injecting medical knowledge graphs into transformer language models."},
                {"p9", "Scalable Reinforcement Learning for Autonomous Driving", "2020", "RL-based decision making for self-driving vehicle policies."},
                {"p10", "Federated Reinforcement Learning at the Edge", "2023", "Combining federated learning with RL for distributed edge robotics."},
                {"p11", "Graph Attention Networks for Recommendation", "2023", "Applying attention-augmented GNNs to next-item recommendation."},
                {"p12", "LSTM Baselines Revisited for Clinical Time Series", "2018", "Re-evaluating LSTM models on modern clinical time-series benchmarks."},
                {"p13", "Contrastive Pretraining for Molecular Graphs", "2023", "Contrastive self-supervision applied to molecular graph representations."},
                {"p14", "Explainable Federated Models for Cybersecurity Threat Detection", "2023", "Federated anomaly detection with explainability for network security."},
        };
        return toMaps(rows, "id", "title", "year", "abstract");
    }

    private List<Map<String, Object>> authorships() {
        String[][] rows = {
                {"auth_maria", "p1"}, {"auth_nina", "p1"},
                {"auth_kenji", "p2"}, {"auth_hana", "p2"},
                {"auth_amara", "p3"}, {"auth_zoe", "p3"},
                {"auth_ravi", "p4"}, {"auth_dev", "p4"},
                {"auth_kenji", "p5"},
                {"auth_zoe", "p6"}, {"auth_amara", "p6"},
                {"auth_tom", "p7"}, {"auth_lucas", "p7"},
                {"auth_maria", "p8"},
                {"auth_ravi", "p9"},
                {"auth_amara", "p10"}, {"auth_dev", "p10"},
                {"auth_zoe", "p11"},
                {"auth_hana", "p12"},
                {"auth_kenji", "p13"},
                {"auth_nina", "p14"}, {"auth_amara", "p14"},
        };
        return toMaps(rows, "authorId", "paperId");
    }

    private List<Map<String, Object>> paperTechnology() {
        String[][] rows = {
                {"p1", "tech_transformer"}, {"p1", "tech_attention"},
                {"p2", "tech_gnn"},
                {"p3", "tech_federated"},
                {"p4", "tech_rl"},
                {"p5", "tech_kg"}, {"p5", "tech_gnn"},
                {"p6", "tech_contrastive"}, {"p6", "tech_gnn"},
                {"p7", "tech_diffusion"},
                {"p8", "tech_transformer"}, {"p8", "tech_kg"},
                {"p9", "tech_rl"},
                {"p10", "tech_federated"}, {"p10", "tech_rl"},
                {"p11", "tech_gnn"}, {"p11", "tech_attention"},
                {"p12", "tech_lstm"},
                {"p13", "tech_contrastive"}, {"p13", "tech_gnn"},
                {"p14", "tech_federated"},
        };
        return toMaps(rows, "paperId", "techId");
    }

    /** citing -> cited, forming genuine multi-hop chains for the shortest-path demo */
    private List<Map<String, Object>> citations() {
        String[][] rows = {
                {"p5", "p2"},   // KG drug discovery cites GNN molecular property paper
                {"p13", "p2"},  // contrastive molecular graphs cites GNN molecular property paper
                {"p13", "p6"},  // contrastive molecular graphs cites contrastive recsys paper
                {"p8", "p1"},   // transformer+KG clinical NLP cites long-doc attention paper
                {"p8", "p5"},   // cites KG drug discovery paper (shared KG idea)
                {"p11", "p6"},  // graph attention recsys cites contrastive recsys paper
                {"p11", "p1"},  // cites attention paper
                {"p10", "p3"},  // federated RL at edge cites federated healthcare paper
                {"p10", "p4"},  // cites RL robotic manipulation paper
                {"p9", "p4"},   // autonomous driving RL cites robotic manipulation RL paper
                {"p14", "p3"},  // federated cybersecurity cites federated healthcare paper
                {"p3", "p12"},  // federated healthcare cites LSTM clinical time series paper
                {"p6", "p2"},   // contrastive recsys cites GNN molecular paper (methodology)
        };
        return toMaps(rows, "fromId", "toId");
    }

    private List<Map<String, Object>> toMaps(String[][] rows, String... keys) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (String[] row : rows) {
            Map<String, Object> map = new LinkedHashMap<>();
            for (int i = 0; i < keys.length; i++) map.put(keys[i], row[i]);
            result.add(map);
        }
        return result;
    }
}
