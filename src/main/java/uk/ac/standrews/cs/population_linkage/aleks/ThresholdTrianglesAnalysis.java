/*
 * Copyright 2022 Systems Research Group, University of St Andrews:
 * <https://github.com/stacs-srg>
 *
 * This file is part of the module population-linkage.
 *
 * population-linkage is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * population-linkage is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with population-linkage. If not, see
 * <http://www.gnu.org/licenses/>.
 */
package uk.ac.standrews.cs.population_linkage.aleks;

import org.neo4j.driver.Result;
import uk.ac.standrews.cs.neoStorr.util.NeoDbCypherBridge;
import uk.ac.standrews.cs.utilities.ClassificationMetrics;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class ThresholdTrianglesAnalysis {

    private static final String BIRTH_BIRTH_SIBLING_TPC = "MATCH (b1:Birth)-[r:SIBLING {actors: \"Child-Child\"}]->(b2:Birth) WHERE (b1)-[:GT_SIBLING {actors: \"Child-Child\"}]-(b2) AND r.distance <= $threshold AND r.fields_populated >= $field return count(r)";
    private static final String BIRTH_BIRTH_SIBLING_FPC = "MATCH (b1:Birth)-[r:SIBLING {actors: \"Child-Child\"}]->(b2:Birth) WHERE NOT (b1)-[:GT_SIBLING {actors: \"Child-Child\"}]-(b2) AND r.distance <= $threshold AND r.fields_populated >= $field return count(r)";
    private static final String BIRTH_BIRTH_SIBLING_FNC = "MATCH (b1:Birth)-[r:GT_SIBLING { actors: \"Child-Child\"}]->(b2:Birth) WHERE NOT (b1)-[:SIBLING {actors: \"Child-Child\"}]-(b2) return count(r)";
    private static final String BIRTH_BIRTH_SIBLING_FNC_T = "MATCH (b1:Birth)-[r:GT_SIBLING { actors: \"Child-Child\"}]->(b2:Birth), (b1)-[s:SIBLING]-(b2) WHERE s.distance > $threshold OR s.fields_populated < $field return count(r)";

    private static final String DEATH_DEATH_SIBLING_TPC = "MATCH (d1:Death)-[r:SIBLING {actors: \"Deceased-Deceased\"}]->(d2:Death) WHERE (d1)-[:GT_SIBLING {actors: \"Deceased-Deceased\"}]-(d2) AND r.distance <= $threshold AND r.fields_populated >= $field return count(r)";
    private static final String DEATH_DEATH_SIBLING_FPC = "MATCH (d1:Death)-[r:SIBLING {actors: \"Deceased-Deceased\"}]->(d2:Death) WHERE NOT (d1)-[:GT_SIBLING {actors: \"Deceased-Deceased\"}]-(d2) AND r.distance <= $threshold AND r.fields_populated >= $field return count(r)";
    private static final String DEATH_DEATH_SIBLING_FNC = "MATCH (d1:Death)-[r:GT_SIBLING {actors: \"Deceased-Deceased\"}]->(d2:Death) WHERE NOT (d1)-[:SIBLING {actors: \"Deceased-Deceased\"}]-(d2) return count(r)";
    private static final String DEATH_DEATH_SIBLING_FNC_T = "MATCH (d1:Death)-[r:GT_SIBLING {actors: \"Deceased-Deceased\"}]->(d2:Death), (d1)-[s:SIBLING]-(d2) WHERE s.distance > $threshold OR s.fields_populated < $field return count(r)";

    private static final String BIRTH_DEATH_SIBLING_TPC = "MATCH (b:Birth)-[r:SIBLING {actors: \"Deceased-Child\"}]-(d:Death) WHERE (b)-[:GT_SIBLING {actors: \"Child-Deceased\"}]-(d) AND r.distance <= $threshold AND r.fields_populated >= $field return count(r)";
    private static final String BIRTH_DEATH_SIBLING_FPC = "MATCH (b:Birth)-[r:SIBLING {actors: \"Deceased-Child\"}]-(d:Death) WHERE NOT (b)-[:GT_SIBLING {actors: \"Child-Deceased\"}]-(d) AND r.distance <= $threshold AND r.fields_populated >= $field return count(r)";
    private static final String BIRTH_DEATH_SIBLING_FNC = "MATCH (b:Birth)-[r:GT_SIBLING {actors: \"Child-Deceased\"}]-(d:Death) WHERE NOT (b)-[:SIBLING {actors: \"Deceased-Child\"}]-(d) return count(r)";
    private static final String BIRTH_DEATH_SIBLING_FNC_T = "MATCH (b:Birth)-[r:GT_SIBLING {actors: \"Child-Deceased\"}]-(d:Death), (b)-[s:SIBLING]-(d) WHERE s.distance > $threshold OR s.fields_populated < $field return count(r)";


    public static void main(String[] args) {
        NeoDbCypherBridge bridge = new NeoDbCypherBridge();
        final int MAX_FIELD = 6;
        final int MIN_FIELD = 2; //1 below target
        final double MAX_THRESHOLD = 1.01; //0.01 above target
        final double MIN_THRESHOLD = 0.0;


        System.out.println("Analysing thresholds...");
        for (int fields = MAX_FIELD; fields > MIN_FIELD; fields--) {
            System.out.println("Field: " + fields);
            try (FileWriter fileWriter = new FileWriter("birthdeathIDSquare" + fields + ".csv");
                 PrintWriter printWriter = new PrintWriter(fileWriter)) {
//                printWriter.println("threshold,precision,recall,fmeasure,triangles");
                printWriter.println("threshold,squares");
                for (double i = MIN_THRESHOLD; i < MAX_THRESHOLD; i += 0.01) {
                    double threshold = Math.round(i * 100.0) / 100.0;
                    System.out.println(threshold);
//                    long fpc = doQuery(BIRTH_DEATH_SIBLING_FPC, threshold, fields, bridge);
//                    long tpc = doQuery(BIRTH_DEATH_SIBLING_TPC, threshold, fields, bridge);
//                    long fnc = doQuery(BIRTH_DEATH_SIBLING_FNC, threshold, fields, bridge) + doQuery(BIRTH_DEATH_SIBLING_FNC_T, i, fields, bridge);
//                long fpc = 1L;
//                long tpc = 1L;
//                long fnc = 1L;

//                    printWriter.printf("%.2f,%.5f,%.5f,%.5f,%d%n", threshold, ClassificationMetrics.precision(tpc, fpc), ClassificationMetrics.recall(tpc, fnc), ClassificationMetrics.F1(tpc, fpc, fnc), PatternsCounter.countOpenTrianglesCumulative(bridge, "Birth", "Birth", i, fields));
                    printWriter.printf("%.2f,%d%n",
                            threshold,
                            PatternsCounter.countOpenSquaresCumulative(bridge, "Birth", "Death", i, fields));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }


        }


        System.out.println("Done!");
    }

    protected static long doQuery(String query_string, double threshold, int fields, NeoDbCypherBridge bridge) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("threshold", threshold);
        parameters.put("field", fields);
        Result result = bridge.getNewSession().run(query_string, parameters);
        return (long) result.list(r -> r.get("count(r)").asInt()).get(0);
    }

}
