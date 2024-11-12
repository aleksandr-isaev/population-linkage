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
import uk.ac.standrews.cs.neoStorr.impl.Store;
import uk.ac.standrews.cs.neoStorr.util.NeoDbCypherBridge;

import java.util.List;
import java.util.Objects;

public class PatternsCounter {

    final static String[] RECORD_TYPES = {"Birth", "Marriage", "Death"};

    public static void main(String[] args) {
        NeoDbCypherBridge bridge = Store.getInstance().getBridge();

        for(String type1: RECORD_TYPES){
            for(String type2: RECORD_TYPES) {
                countOpenTrianglesToString(bridge, type1, type2);
            }
        }
    }

    public static void countOpenTrianglesToString(NeoDbCypherBridge bridge, String type1, String type2) {
        int count = countOpenTriangles(bridge, type1, type2);
        System.out.println(type1 + "-" + type2 + "-" + type1 + " triangle: " + count);
    }

    public static void countOpenTrianglesToStringID(NeoDbCypherBridge bridge, String type1, String type2) {
        int count = countOpenTrianglesID(bridge, type1, type2);
        System.out.println(type1 + "-" + type2 + "-" + type1 + " triangle: " + count);
    }

    public static int countOpenTriangles(NeoDbCypherBridge bridge, String type1, String type2) {
        String openTriangleQuery = String.format(
                "MATCH (x:%s)-[:SIBLING]-(y:%s)-[:SIBLING]-(z:%s) " +
                        "WHERE NOT (x)-[:SIBLING]-(z) AND id(x) < id(z) AND NOT (x)-[:DELETED]-(y) AND NOT (z)-[:DELETED]-(y)" +
                        "RETURN count(DISTINCT [x, z]) as cluster_count",
                type1, type2, type1
        );

        Result result = bridge.getNewSession().run(openTriangleQuery);
        List<Long> clusters = result.list(r -> r.get("cluster_count").asLong());

        long count = 0;
        if (!clusters.isEmpty()) {
            count = clusters.get(0);
        }

        return (int) count;
    }

    public static int countOpenTrianglesTwoTypes(NeoDbCypherBridge bridge, String type1, String type2, double threshold, int fields, boolean isCorrect) {
        long count = 0;

        String openTriangleQuery;

        if(isCorrect){
            openTriangleQuery = String.format(
                    "MATCH (x:%1$s)-[s:SIBLING]-(y:%2$s)-[r:SIBLING]-(z:%1$s) " +
                            "WHERE NOT (x)-[:SIBLING]-(z) AND id(x) < id(z) AND x.PARENTS_DAY_OF_MARRIAGE = z.PARENTS_DAY_OF_MARRIAGE AND x.PARENTS_MONTH_OF_MARRIAGE = z.PARENTS_MONTH_OF_MARRIAGE AND x.PARENTS_YEAR_OF_MARRIAGE = z.PARENTS_YEAR_OF_MARRIAGE and x.PARENTS_YEAR_OF_MARRIAGE <> \"----\" AND z.PARENTS_YEAR_OF_MARRIAGE <> \"----\"\n" +
                            "AND r.distance <= %3$s AND s.distance <= %3$s AND r.fields_populated >= %4$s AND s.fields_populated >= %4$s\n" +
                            "RETURN count(DISTINCT [x, z]) as cluster_count",
                    type1, type2, threshold, fields
            );
        }else{
            openTriangleQuery = String.format(
                    "MATCH (x:%1$s)-[s:SIBLING]-(y:%2$s)-[r:SIBLING]-(z:%1$s) " +
                            "WHERE NOT (x)-[:SIBLING]-(z) AND id(x) < id(z) AND (x.PARENTS_DAY_OF_MARRIAGE <> z.PARENTS_DAY_OF_MARRIAGE OR x.PARENTS_MONTH_OF_MARRIAGE <> z.PARENTS_MONTH_OF_MARRIAGE OR x.PARENTS_YEAR_OF_MARRIAGE <> z.PARENTS_YEAR_OF_MARRIAGE)\n" +
                            "AND r.distance <= %3$s AND s.distance <= %3$s AND r.fields_populated >= %4$s AND s.fields_populated >= %4$s\n" +
                            "RETURN count(DISTINCT [x, z]) as cluster_count",
                    type1, type2, threshold, fields
            );
        }


        Result result = bridge.getNewSession().run(openTriangleQuery);
        List<Long> clusters = result.list(r -> r.get("cluster_count").asLong());

        if (!clusters.isEmpty()) {
            count = clusters.get(0);
        }

        String openTriangleQuery2;

        if(isCorrect){
            openTriangleQuery2 = String.format(
                    "MATCH (x:%1$s)-[s:SIBLING]-(y:%2$s)-[r:SIBLING]-(z:%1$s), (x)-[t:SIBLING]-(z)  " +
                            "WHERE id(x) < id(z) AND x.PARENTS_DAY_OF_MARRIAGE = z.PARENTS_DAY_OF_MARRIAGE AND x.PARENTS_MONTH_OF_MARRIAGE = z.PARENTS_MONTH_OF_MARRIAGE AND x.PARENTS_YEAR_OF_MARRIAGE = z.PARENTS_YEAR_OF_MARRIAGE and x.PARENTS_YEAR_OF_MARRIAGE <> \"----\" AND z.PARENTS_YEAR_OF_MARRIAGE <> \"----\"\n" +
                            "AND r.distance <= %3$s AND s.distance <= %3$s AND r.fields_populated >= %4$s AND s.fields_populated >= %4$s\n" +
                            "AND (t.fields_populated < %4$s OR t.distance > %3$s)\n" +
                            "RETURN count(DISTINCT [x, z]) as cluster_count",
                    type1, type2, threshold, fields
            );
        }else{
            openTriangleQuery2 = String.format(
                    "MATCH (x:%1$s)-[s:SIBLING]-(y:%2$s)-[r:SIBLING]-(z:%1$s), (x)-[t:SIBLING]-(z)  " +
                            "WHERE id(x) < id(z) AND (x.PARENTS_DAY_OF_MARRIAGE <> z.PARENTS_DAY_OF_MARRIAGE OR x.PARENTS_MONTH_OF_MARRIAGE <> z.PARENTS_MONTH_OF_MARRIAGE OR x.PARENTS_YEAR_OF_MARRIAGE <> z.PARENTS_YEAR_OF_MARRIAGE)\n" +
                            "AND r.distance <= %3$s AND s.distance <= %3$s AND r.fields_populated >= %4$s AND s.fields_populated >= %4$s\n" +
                            "AND (t.fields_populated < %4$s OR t.distance > %3$s)\n" +
                            "RETURN count(DISTINCT [x, z]) as cluster_count",
                    type1, type2, threshold, fields
            );
        }

        result = bridge.getNewSession().run(openTriangleQuery2);
        clusters = result.list(r -> r.get("cluster_count").asLong());

        long tCount = 0;
        if (!clusters.isEmpty()) {
            tCount = clusters.get(0);
        }

        return (int) count + (int) tCount;
    }

    public static int countOpenTrianglesID(NeoDbCypherBridge bridge, String type1, String type2) {
        String openTriangleQuery = String.format(
                "MATCH (x:%s)-[:ID]-(y:%s)-[:ID]-(z:%s) " +
                        "WHERE NOT (x)-[:SIBLING]-(z) AND id(x) < id(z) AND NOT (x)-[:DELETED]-(y) AND NOT (z)-[:DELETED]-(y)" +
                        "RETURN count(DISTINCT [x, z]) as cluster_count",
                type1, type2, type1
        );

        Result result = bridge.getNewSession().run(openTriangleQuery);
        List<Long> clusters = result.list(r -> r.get("cluster_count").asLong());

        long count = 0;
        if (!clusters.isEmpty()) {
            count = clusters.get(0);
        }

        return (int) count;
    }


    //Won't work for resolver as no deleted check
    public static int countOpenTrianglesCumulative(NeoDbCypherBridge bridge, String type1, String type2, double threshold, int fields) {
        long count = 0;
        String openTriangleQuery = String.format(
                "MATCH (x:%1$s)-[s:SIBLING]-(y:%2$s)-[r:SIBLING]-(z:%1$s) " +
                        "WHERE NOT (x)-[:SIBLING]-(z) AND id(x) < id(z) AND r.distance <= %3$s AND s.distance <= %3$s AND r.fields_populated >= %4$s AND s.fields_populated >= %4$s " +
                        "RETURN count(DISTINCT [x, z]) as cluster_count",
                type1, type2, threshold, fields
        );

        Result result = bridge.getNewSession().run(openTriangleQuery);
        List<Long> clusters = result.list(r -> r.get("cluster_count").asLong());

        if (!clusters.isEmpty()) {
            count = clusters.get(0);
        }

        String openTriangleQuery2 = String.format(
                "MATCH (x:%1$s)-[s:SIBLING]-(y:%2$s)-[r:SIBLING]-(z:%1$s), (x)-[t:SIBLING]-(z)  " +
                        "WHERE id(x) < id(z) AND r.distance <= %3$s AND s.distance <= %3$s AND r.fields_populated >= %4$s AND s.fields_populated >= %4$s " +
                        "AND (t.fields_populated < %4$s OR t.distance > %3$s) " +
                        "RETURN count(DISTINCT [x, z]) as cluster_count",
                type1, type2, threshold, fields
        );

        result = bridge.getNewSession().run(openTriangleQuery2);
        clusters = result.list(r -> r.get("cluster_count").asLong());

        long tCount = 0;
        if (!clusters.isEmpty()) {
            tCount = clusters.get(0);
        }

        return (int) count + (int) tCount;
    }

    public static int countOpenTrianglesCumulativeDouble(NeoDbCypherBridge bridge, String type1, String type2, double threshold, int fields) {
        long count = 0;
        String openTriangleQuery = String.format(
                "MATCH (x:%1$s)-[s:SIBLING]-(y:%1$s)-[r:SIBLING]-(z:%2$s) " +
                        "WHERE NOT (x)-[:SIBLING]-(z) AND id(x) < id(z) AND x.FORENAME <> z.FORENAME AND x.BIRTH_YEAR <> right(z.DATE_OF_BIRTH, 4) AND r.distance <= %3$s AND r.fields_populated >= %4$s\n" +
                        "RETURN count(DISTINCT [x, z]) as cluster_count",
                type1, type2, threshold, fields
        );

        Result result = bridge.getNewSession().run(openTriangleQuery);
        List<Long> clusters = result.list(r -> r.get("cluster_count").asLong());

        if (!clusters.isEmpty()) {
            count = clusters.get(0);
        }

        String openTriangleQuery2 = String.format(
                "MATCH (x:%1$s)-[s:SIBLING]-(y:%1$s)-[r:SIBLING]-(z:%2$s), (x)-[t:SIBLING]-(z) " +
                        "WHERE id(x) < id(z) AND x.FORENAME <> z.FORENAME AND x.BIRTH_YEAR <> right(z.DATE_OF_BIRTH, 4) AND r.distance <= %3$s AND r.fields_populated >= %4$s\n" +
                        "AND (t.fields_populated < %4$s OR t.distance > %3$s ) " +
                        "RETURN count(DISTINCT [x, z]) as cluster_count",
                type1, type2, threshold, fields
        );

        result = bridge.getNewSession().run(openTriangleQuery2);
        clusters = result.list(r -> r.get("cluster_count").asLong());

        long tCount = 0;
        if (!clusters.isEmpty()) {
            tCount = clusters.get(0);
        }

        return (int) count + (int) tCount;
    }

    public static int countOpenTrianglesCumulativeMarriage(NeoDbCypherBridge bridge, String type1, String type2, double threshold, int fields) {
        long count = 0;
        String openTriangleQuery = String.format(
                "MATCH (x:%1$s)-[s:SIBLING {actors: \"Groom-Groom\"}]-(y:%1$s)-[r:ID]-(z:%2$s) " +
                        "WHERE NOT (x)-[:ID]-(z) AND id(x) < id(z) AND r.actors = \"Groom-Couple\"AND r.distance <= %3$s AND r.fields_populated >= %4$s\n" +
                        "RETURN count(DISTINCT [x, z]) as cluster_count",
                type1, type2, threshold, fields
        );

        Result result = bridge.getNewSession().run(openTriangleQuery);
        List<Long> clusters = result.list(r -> r.get("cluster_count").asLong());

        if (!clusters.isEmpty()) {
            count = clusters.get(0);
        }

        String openTriangleQuery2 = String.format(
                "MATCH (x:%1$s)-[s:SIBLING {actors: \"Groom-Groom\"}]-(y:%1$s)-[r:ID]-(z:%2$s), (x)-[t:ID]-(z) " +
                        "WHERE id(x) < id(z) AND r.actors = \"Groom-Couple\" AND r.distance <= %3$s AND r.fields_populated >= %4$s\n" +
                        "AND (t.fields_populated < %4$s OR t.distance > %3$s ) AND t.actors = \"Groom-Couple\" " +
                        "RETURN count(DISTINCT [x, z]) as cluster_count",
                type1, type2, threshold, fields
        );

        result = bridge.getNewSession().run(openTriangleQuery2);
        clusters = result.list(r -> r.get("cluster_count").asLong());

        long tCount = 0;
        if (!clusters.isEmpty()) {
            tCount = clusters.get(0);
        }

        return (int) count + (int) tCount;
    }

    public static int countOpenTrianglesCumulativeBD(NeoDbCypherBridge bridge, String type1, String type2, double threshold, int fields, boolean isCorrect) {
        long count = 0;
        String openTriangleQuery = String.format(
                "MATCH (b1:%1$s)-[:SIBLING]-(b2:%1$s)-[:ID]-(d:%2$s), (b1)-[t:ID]-(d) " +
                        "WHERE NOT (b1)-[:SIBLING]-(d) AND b1.FORENAME = d.FORENAME AND b1.SURNAME = d.SURNAME AND b1.BIRTH_YEAR = right(d.DATE_OF_BIRTH, 4)\n" +
                        "AND (t.fields_populated < %4$s OR t.distance > %3$s ) " +
                        "RETURN count(DISTINCT [b1, d]) as cluster_count",
                type1, type2, threshold, fields
        );

        if(!isCorrect) {
            openTriangleQuery = String.format(
                    "MATCH (b1:%1$s)-[:SIBLING]-(b2:%1$s)-[:ID]-(d:%2$s), (b1)-[t:ID]-(d) " +
                            "WHERE NOT (b1)-[:SIBLING]-(d) AND b1.FORENAME <> d.FORENAME AND b1.SURNAME <> d.SURNAME AND b1.BIRTH_YEAR <> right(d.DATE_OF_BIRTH, 4)\n" +
                            "AND (t.fields_populated < %4$s OR t.distance > %3$s ) " +
                            "RETURN count(DISTINCT [b1, d]) as cluster_count",
                    type1, type2, threshold, fields
            );
        }

        Result result = bridge.getNewSession().run(openTriangleQuery);
        List<Long> clusters = result.list(r -> r.get("cluster_count").asLong());

        if (!clusters.isEmpty()) {
            count = clusters.get(0);
        }

        return (int) count;
    }

    public static int countOpenTrianglesIsomorphicSiblings(NeoDbCypherBridge bridge, String type1, String type2, double threshold, int fields) {
        long count = 0;
        String openTriangleQuery = String.format(
                "MATCH (b1:%1$s)-[:SIBLING]-(b2:%1$s)-[:SIBLING]-(b3:%1$s),\n" +
                        "(d1:%2$s)-[r:SIBLING]-(d2:%2$s)-[s:SIBLING]-(d3:%2$s),\n" +
                        "(b1)-[:ID]-(d1),\n" +
                        "(b2)-[:ID]-(d2),\n" +
                        "(b3)-[:ID]-(d3),\n" +
                        "(b1)-[:SIBLING]-(b3)\n" +
                        "WHERE NOT (d1)-[:SIBLING]-(d3) AND b1.BIRTH_YEAR = right(d1.DATE_OF_BIRTH, 4) AND b2.BIRTH_YEAR = right(d2.DATE_OF_BIRTH, 4) AND b3.BIRTH_YEAR = right(d3.DATE_OF_BIRTH, 4)\n" +
                        "AND id(d1) < id(d3) AND r.distance <= %3$s AND r.fields_populated >= %4$s AND s.distance <= %3$s AND s.fields_populated >= %4$s\n" +
                        "RETURN count(DISTINCT [d1, d2]) as cluster_count",
                type1, type2, threshold, fields
        );

        Result result = bridge.getNewSession().run(openTriangleQuery);
        List<Long> clusters = result.list(r -> r.get("cluster_count").asLong());

        if (!clusters.isEmpty()) {
            count = clusters.get(0);
        }

        String openTriangleQuery2 = String.format(
                "MATCH (b1:%1$s)-[:SIBLING]-(b2:%1$s)-[:SIBLING]-(b3:%1$s),\n" +
                        "(d1:%2$s)-[r:SIBLING]-(d2:%2$s)-[s:SIBLING]-(d3:%2$s),\n" +
                        "(b1)-[:ID]-(d1),\n" +
                        "(b2)-[:ID]-(d2),\n" +
                        "(b3)-[:ID]-(d3),\n" +
                        "(b1)-[:SIBLING]-(b3),\n" +
                        "(d1)-[t:SIBLING]-(d3)\n" +
                        "WHERE b1.BIRTH_YEAR = right(d1.DATE_OF_BIRTH, 4) AND b2.BIRTH_YEAR = right(d2.DATE_OF_BIRTH, 4) AND b3.BIRTH_YEAR = right(d3.DATE_OF_BIRTH, 4)\n" +
                        "AND id(d1) < id(d3) AND r.distance <= %3$s AND r.fields_populated >= %4$s AND s.distance <= %3$s AND s.fields_populated >= %4$s\n" +
                        "AND (t.fields_populated < %4$s OR t.distance > %3$s)\n" +
                        "RETURN count(DISTINCT [d1, d3]) as cluster_count",
                type1, type2, threshold, fields
        );

        result = bridge.getNewSession().run(openTriangleQuery2);
        clusters = result.list(r -> r.get("cluster_count").asLong());

        long tCount = 0;
        if (!clusters.isEmpty()) {
            tCount = clusters.get(0);
        }

        return (int) count + (int) tCount;
    }

    public static int countOpenSquaresCumulative(NeoDbCypherBridge bridge, String type1, String type2, double threshold, int fields) {
        long count = 0;
        String openSquaresQuery;
        String openSquaresQuery2;

        if (Objects.equals(type2, "Death") && Objects.equals(type1, "Birth")) {
            openSquaresQuery = String.format("MATCH (b1:%1$s)-[:SIBLING]-(b2:%1$s),\n" +
                    "(d1:%2$s)-[:SIBLING]-(d2:%2$s),\n" +
                    "(b1)-[r:ID]-(d1)\n" +
                    "WHERE id(b1) < id(b2) AND NOT (b2)-[:ID]-(d2) AND r.distance <= %3$s AND r.fields_populated >= %4$s " +
                    "RETURN count(DISTINCT [b1, b2]) as cluster_count", type1, type2, threshold, fields);
            openSquaresQuery2 = String.format("MATCH (b1:%1$s)-[:SIBLING]-(b2:%1$s),\n" +
                    "(d1:%2$s)-[:SIBLING]-(d2:%2$s),\n" +
                    "(b1)-[r:ID]-(d1),\n" +
                    "(b2)-[s:ID]-(d2)\n" +
                    "WHERE id(b1) < id(b2) AND r.distance <= %3$s AND r.fields_populated >= %4$s\n" +
                    "AND (s.fields_populated < %4$s OR s.distance > %3$s) " +
                    "RETURN count(DISTINCT [b1, b2]) as cluster_count", type1, type2, threshold, fields);
        }else if(Objects.equals(type2, "Death") && Objects.equals(type1, "Marriage")){
            openSquaresQuery = String.format("MATCH (b1:%1$s)-[:SIBLING]-(b2:%1$s),\n" +
                    "(d1:%2$s)-[:SIBLING]-(d2:%2$s),\n" +
                    "(b1)-[r:ID]-(d1)\n" +
                    "WHERE NOT (b2)-[:ID]-(d2) AND NOT (b2)-[:SIBLING]-(d2) AND b2.GROOM_FORENAME = d2.FORENAME AND right(b2.GROOM_AGE_OR_DATE_OF_BIRTH, 4) = right(d2.DATE_OF_BIRTH, 4) " +
                    "AND b1.GROOM_FORENAME = d1.FORENAME AND right(b1.GROOM_AGE_OR_DATE_OF_BIRTH, 4) = right(d1.DATE_OF_BIRTH, 4) AND r.distance <= %3$s AND r.fields_populated >= %4$s\n" +
                    "RETURN count(DISTINCT [b1, b2]) as cluster_count", type1, type2, threshold, fields);

            openSquaresQuery2 = String.format("MATCH (b1:%1$s)-[:SIBLING]-(b2:%1$s),\n" +
                    "(d1:%2$s)-[:SIBLING]-(d2:%2$s),\n" +
                    "(b1)-[r:ID]-(d1),\n" +
                    "(b2)-[s:ID]-(d2)\n" +
                    "WHERE NOT (b2)-[:SIBLING]-(d2) AND r.distance <= %3$s AND r.fields_populated >= %4$s\n" +
                    "AND b2.GROOM_FORENAME = d2.FORENAME AND right(b2.GROOM_AGE_OR_DATE_OF_BIRTH, 4)= right(d2.DATE_OF_BIRTH, 4) AND b1.GROOM_FORENAME = d1.FORENAME AND right(b1.GROOM_AGE_OR_DATE_OF_BIRTH, 4) = right(d1.DATE_OF_BIRTH, 4) " +
                    "AND (s.fields_populated < %4$s OR s.distance > %3$s) " +
                    "RETURN count(DISTINCT [b1, b2]) as cluster_count", type1, type2, threshold, fields);
        }else{
            openSquaresQuery = String.format("MATCH (b1:%1$s)-[:SIBLING]-(b2:%1$s),\n" +
                    "(d1:%2$s)-[:SIBLING]-(d2:%2$s),\n" +
                    "(b1)-[r:ID {actors: \"Child-Groom\"}]-(d1)\n" +
                    "WHERE id(b1) < id(b2) AND NOT (b2)-[:ID]-(d2) AND r.distance <= %3$s AND r.fields_populated >= %4$s\n" +
                    "RETURN count(DISTINCT [b1, b2]) as cluster_count", type1, type2, threshold, fields);

            openSquaresQuery2 = String.format("MATCH (b1:%1$s)-[:SIBLING]-(b2:%1$s),\n" +
                    "(d1:%2$s)-[:SIBLING]-(d2:%2$s),\n" +
                    "(b1)-[r:ID {actors: \"Child-Groom\"}]-(d1),\n" +
                    "(b2)-[s:ID {actors: \"Child-Groom\"}]-(d2)\n" +
                    "WHERE id(b1) < id(b2) AND r.distance <= %3$s AND r.fields_populated >= %4$s\n" +
                    "AND (s.fields_populated < %4$s OR s.distance > %3$s) " +
                    "RETURN count(DISTINCT [b1, b2]) as cluster_count", type1, type2, threshold, fields);

        }


        Result result = bridge.getNewSession().run(openSquaresQuery);
        List<Long> clusters = result.list(r -> r.get("cluster_count").asLong());


        if (!clusters.isEmpty()) {
            count = clusters.get(0);
        }


        result = bridge.getNewSession().run(openSquaresQuery2);
        clusters = result.list(r -> r.get("cluster_count").asLong());

        long tCount = 0;
        if (!clusters.isEmpty()) {
            tCount = clusters.get(0);
        }

        return (int) count + (int) tCount;
    }
}
