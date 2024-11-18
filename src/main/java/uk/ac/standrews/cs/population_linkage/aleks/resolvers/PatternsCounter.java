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
package uk.ac.standrews.cs.population_linkage.aleks.resolvers;

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

    public static int countOpenTrianglesID(NeoDbCypherBridge bridge, String type1, String type2) {
        String openTriangleQuery = String.format(
                "MATCH (x:%s)-[:SIBLING]-(y:%s)-[:ID]-(z:%s) " +
                        "WHERE NOT (x)-[:ID]-(z) AND id(x) < id(z) AND NOT (x)-[:DELETED]-(y) AND NOT (z)-[:DELETED]-(y)" +
                        "RETURN count(DISTINCT [x, z]) as cluster_count",
                type1, type1, type2
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

    public static int countOpenTrianglesSibFNOT(NeoDbCypherBridge bridge, String type1, String type2, double threshold, int fields) {
        long count = 0;
        String openTriangleQuery = "";
        String openTriangleQuery2 = "";

        if(Objects.equals(type1, "Birth") && Objects.equals(type2, "Birth")){
            openTriangleQuery = String.format(
                    "MATCH (x:%1$s)-[s:SIBLING]-(y:%2$s)-[r:SIBLING]-(z:%1$s) " +
                            "WHERE NOT (x)-[:SIBLING]-(z) AND id(x) < id(z) AND x.PARENTS_DAY_OF_MARRIAGE = z.PARENTS_DAY_OF_MARRIAGE AND x.PARENTS_MONTH_OF_MARRIAGE = z.PARENTS_MONTH_OF_MARRIAGE AND x.PARENTS_YEAR_OF_MARRIAGE = z.PARENTS_YEAR_OF_MARRIAGE and x.PARENTS_YEAR_OF_MARRIAGE <> \"----\" AND z.PARENTS_YEAR_OF_MARRIAGE <> \"----\"\n" +
                            "AND r.distance <= %3$s AND s.distance <= %3$s AND r.fields_populated >= %4$s AND s.fields_populated >= %4$s\n" +
                            "RETURN count(DISTINCT [x, z]) as cluster_count",
                    type1, type2, threshold, fields
            );

            openTriangleQuery2 = String.format(
                    "MATCH (x:%1$s)-[s:SIBLING]-(y:%2$s)-[r:SIBLING]-(z:%1$s), (x)-[t:SIBLING]-(z)  " +
                            "WHERE id(x) < id(z) AND x.PARENTS_DAY_OF_MARRIAGE = z.PARENTS_DAY_OF_MARRIAGE AND x.PARENTS_MONTH_OF_MARRIAGE = z.PARENTS_MONTH_OF_MARRIAGE AND x.PARENTS_YEAR_OF_MARRIAGE = z.PARENTS_YEAR_OF_MARRIAGE and x.PARENTS_YEAR_OF_MARRIAGE <> \"----\" AND z.PARENTS_YEAR_OF_MARRIAGE <> \"----\"\n" +
                            "AND r.distance <= %3$s AND s.distance <= %3$s AND r.fields_populated >= %4$s AND s.fields_populated >= %4$s\n" +
                            "AND (t.fields_populated < %4$s OR t.distance > %3$s)\n" +
                            "RETURN count(DISTINCT [x, z]) as cluster_count",
                    type1, type2, threshold, fields
            );
        }

        Result result = bridge.getNewSession().run(openTriangleQuery);
        List<Long> clusters = result.list(r -> r.get("cluster_count").asLong());

        if (!clusters.isEmpty()) {
            count = clusters.get(0);
        }

        result = bridge.getNewSession().run(openTriangleQuery2);
        clusters = result.list(r -> r.get("cluster_count").asLong());

        long tCount = 0;
        if (!clusters.isEmpty()) {
            tCount = clusters.get(0);
        }

        return (int) count + (int) tCount;
    }

    public static int countOpenTrianglesParentsMarriage(NeoDbCypherBridge bridge, double threshold, int fields, boolean isTotal){
        long count = 0;
        String openSquaresQuery = "";
        String openSquaresQuery2 = "";

        if(isTotal){
            openSquaresQuery = String.format(
                    "MATCH (x:Birth)-[:SIBLING]-(y:Birth)-[r:ID]-(z:Marriage) " +
                            "WHERE NOT (x)-[:ID]-(z) AND id(x) < id(z) AND (r.actors = \"Child-Father\" OR r.actors = \"Child-Mother\") AND r.distance <= %1$s AND r.fields_populated >= %2$s " +
                            "RETURN count(DISTINCT [x, z]) as cluster_count",
                    threshold, fields
            );

            openSquaresQuery2 = String.format(
                    "MATCH (x:Birth)-[:SIBLING]-(y:Birth)-[r:ID]-(z:Marriage), (x)-[t:ID]-(z) " +
                            "WHERE id(x) < id(z) AND (r.actors = \"Child-Father\" OR r.actors = \"Child-Mother\") AND r.distance <= %1$s AND r.fields_populated >= %2$s " +
                            "AND (t.fields_populated < %2$s OR t.distance > %1$s) " +
                            "RETURN count(DISTINCT [x, z]) as cluster_count",
                    threshold, fields
            );
        }else{
            openSquaresQuery = String.format(
                    "MATCH (x:Birth)-[:SIBLING]-(y:Birth)-[r:ID]-(z:Marriage) " +
                            "WHERE NOT (x)-[:ID]-(z) AND id(x) < id(z) AND (r.actors = \"Child-Father\" OR r.actors = \"Child-Mother\") AND r.distance <= %1$s AND r.fields_populated >= %2$s " +
                            "AND x.PARENTS_YEAR_OF_MARRIAGE = z.MARRIAGE_YEAR AND x.PARENTS_MONTH_OF_MARRIAGE = z.MARRIAGE_MONTH AND x.PARENTS_MONTH_OF_MARRIAGE = z.MARRIAGE_MONTH " +
                            "AND y.PARENTS_YEAR_OF_MARRIAGE = z.MARRIAGE_YEAR AND y.PARENTS_MONTH_OF_MARRIAGE = z.MARRIAGE_MONTH AND y.PARENTS_MONTH_OF_MARRIAGE = z.MARRIAGE_MONTH " +
                            "RETURN count(DISTINCT [x, z]) as cluster_count",
                    threshold, fields
            );

            openSquaresQuery2 = String.format(
                    "MATCH (x:Birth)-[:SIBLING]-(y:Birth)-[r:ID]-(z:Marriage), (x)-[t:ID]-(z) " +
                            "WHERE id(x) < id(z) AND (r.actors = \"Child-Father\" OR r.actors = \"Child-Mother\") AND r.distance <= %1$s AND r.fields_populated >= %2$s " +
                            "AND (t.fields_populated < %2$s OR t.distance > %1$s) " +
                            "AND x.PARENTS_YEAR_OF_MARRIAGE = z.MARRIAGE_YEAR AND x.PARENTS_MONTH_OF_MARRIAGE = z.MARRIAGE_MONTH AND x.PARENTS_MONTH_OF_MARRIAGE = z.MARRIAGE_MONTH " +
                            "AND y.PARENTS_YEAR_OF_MARRIAGE = z.MARRIAGE_YEAR AND y.PARENTS_MONTH_OF_MARRIAGE = z.MARRIAGE_MONTH AND y.PARENTS_MONTH_OF_MARRIAGE = z.MARRIAGE_MONTH " +
                            "RETURN count(DISTINCT [x, z]) as cluster_count",
                    threshold, fields
            );
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

    public static int countOpenTrianglesCumulativeAdditionalLinkage(NeoDbCypherBridge bridge, String type1, String type2, double threshold, int fields, boolean isTotal) {
        long count = 0;
        String openSquaresQuery = "";
        String openSquaresQuery2 = "";

        if(isTotal){
            openSquaresQuery = String.format(
                    "MATCH (x:%1$s)-[:SIBLING]-(y:%1$s)-[r:SIBLING]-(z:%2$s) " +
                            "WHERE NOT (x)-[:SIBLING]-(z) AND id(x) < id(z) AND r.distance <= %3$s AND r.fields_populated >= %4$s " +
                            "RETURN count(DISTINCT [x, z]) as cluster_count",
                    type1, type2, threshold, fields
            );

            openSquaresQuery2 = String.format(
                    "MATCH (x:%1$s)-[:SIBLING]-(y:%1$s)-[r:SIBLING]-(z:%2$s), (x)-[t:SIBLING]-(z)  " +
                            "WHERE id(x) < id(z) AND r.distance <= %3$s AND r.fields_populated >= %4$s " +
                            "AND (t.fields_populated < %4$s OR t.distance > %3$s) " +
                            "RETURN count(DISTINCT [x, z]) as cluster_count",
                    type1, type2, threshold, fields
            );
        }else{
            openSquaresQuery = String.format("MATCH (b1:Birth)-[:SIBLING]-(b2:Birth), (b1)-[:SIBLING]-(b:Birth), (b2)-[:SIBLING]-(b), (b2)-[r:SIBLING]-(d:Death), (d)-[:ID]-(b), (b)-[:ID]-(m:Marriage), (b1)-[:ID]-(m), (b2)-[:ID]-(m) \n" +
                    "WHERE id(b1) < id(b2) AND NOT (b1)-[:SIBLING]-(d) AND NOT (d)-[:SIBLING]-(b) AND NOT (b1)-[:ID]-(d) AND b1.BIRTH_YEAR <> right(d.DATE_OF_BIRTH, 4) AND b1.BIRTH_YEAR <> \"----\" \n" +
                    "AND r.distance <= %1$s AND r.fields_populated >= %2$s\n" +
                    "return count(DISTINCT [b1, b2]) as cluster_count", threshold, fields);

            openSquaresQuery2 = String.format("MATCH (b1:Birth)-[:SIBLING]-(b2:Birth), (b1)-[:SIBLING]-(b:Birth), (b2)-[:SIBLING]-(b), (b2)-[r:SIBLING]-(d:Death), (d)-[:ID]-(b), (b)-[:ID]-(m:Marriage), (b1)-[:ID]-(m), (b2)-[:ID]-(m), (b1)-[t:SIBLING]-(d)\n" +
                    "WHERE id(b1) < id(b2) AND NOT (d)-[:SIBLING]-(b) AND b1.BIRTH_YEAR <> right(d.DATE_OF_BIRTH, 4) AND b1.BIRTH_YEAR <> \"----\" \n" +
                    "AND r.distance <= %1$s AND r.fields_populated >= %2$s\n" +
                    "AND (t.fields_populated < %2$s OR t.distance > %1$s)\n" +
                    "return count(DISTINCT [b1, b2]) as cluster_count", threshold, fields);
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

    public static int countOpenSquaresCumulativeID(NeoDbCypherBridge bridge, String type1, String type2, double threshold, int fields, boolean isTotal, String partner) {
        long count = 0;
        String openSquaresQuery = "";
        String openSquaresQuery2 = "";

        if (Objects.equals(type1, "Birth") && Objects.equals(type2, "Death")) {
            if(isTotal){
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
            }else{
                openSquaresQuery = String.format("MATCH (b1:%1$s)-[:SIBLING]-(b2:%1$s),\n" +
                        "(d1:%2$s)-[:SIBLING]-(d2:%2$s),\n" +
                        "(b1)-[r:ID]-(d1)\n" +
                        "WHERE NOT (b2)-[:ID]-(d2) AND NOT (b2)-[:SIBLING]-(d2) AND b2.FORENAME = d2.FORENAME AND b2.SURNAME = d2.SURNAME AND b2.BIRTH_YEAR = right(d2.DATE_OF_BIRTH, 4) " +
                        "AND b1.FORENAME = d1.FORENAME AND b1.SURNAME = d1.SURNAME AND b1.BIRTH_YEAR = right(d1.DATE_OF_BIRTH, 4) AND r.distance <= %3$s AND r.fields_populated >= %4$s\n" +
                        "RETURN count(DISTINCT [b1, b2]) as cluster_count", type1, type2, threshold, fields);

                openSquaresQuery2 = String.format("MATCH (b1:%1$s)-[:SIBLING]-(b2:%1$s),\n" +
                        "(d1:%2$s)-[:SIBLING]-(d2:%2$s),\n" +
                        "(b1)-[r:ID]-(d1),\n" +
                        "(b2)-[s:ID]-(d2)\n" +
                        "WHERE NOT (b2)-[:SIBLING]-(d2) AND r.distance <= %3$s AND r.fields_populated >= %4$s\n" +
                        "AND b2.FORENAME = d2.FORENAME AND b2.SURNAME = d2.SURNAME AND b2.BIRTH_YEAR = right(d2.DATE_OF_BIRTH, 4) AND b1.FORENAME = d1.FORENAME AND b1.SURNAME = d1.SURNAME AND b1.BIRTH_YEAR = right(d1.DATE_OF_BIRTH, 4) " +
                        "AND (s.fields_populated < %4$s OR s.distance > %3$s) " +
                        "RETURN count(DISTINCT [b1, b2]) as cluster_count", type1, type2, threshold, fields);
            }
        }else if(Objects.equals(type1, "Marriage") && Objects.equals(type2, "Death")){
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
        }else if(Objects.equals(type1, "Birth") && Objects.equals(type2, "Marriage")){
            if(isTotal){
                openSquaresQuery = String.format("MATCH (b1:%1$s)-[:SIBLING]-(b2:%1$s),\n" +
                        "(d1:%2$s)-[:SIBLING]-(d2:%2$s),\n" +
                        "(b1)-[r:ID {actors: \"Child-%5$s\"}]-(d1)\n" +
                        "WHERE id(b1) < id(b2) AND NOT (b2)-[:ID]-(d2) AND r.distance <= %3$s AND r.fields_populated >= %4$s\n" +
                        "RETURN count(DISTINCT [b1, b2]) as cluster_count", type1, type2, threshold, fields, partner);

                openSquaresQuery2 = String.format("MATCH (b1:%1$s)-[:SIBLING]-(b2:%1$s),\n" +
                        "(d1:%2$s)-[:SIBLING]-(d2:%2$s),\n" +
                        "(b1)-[r:ID {actors: \"Child-%5$s\"}]-(d1),\n" +
                        "(b2)-[s:ID {actors: \"Child-%5$s\"}]-(d2)\n" +
                        "WHERE id(b1) < id(b2) AND r.distance <= %3$s AND r.fields_populated >= %4$s\n" +
                        "AND (s.fields_populated < %4$s OR s.distance > %3$s) " +
                        "RETURN count(DISTINCT [b1, b2]) as cluster_count", type1, type2, threshold, fields, partner);
            }else{
                String partnerCapitalised = partner.toUpperCase();
                openSquaresQuery = String.format("MATCH (b1:%1$s)-[:SIBLING]-(b2:%1$s),\n" +
                        "(d1:%2$s)-[:SIBLING]-(d2:%2$s),\n" +
                        "(b1)-[r:ID {actors: \"Child-%5$s\"}]-(d1)\n" +
                        "WHERE id(b1) < id(b2) AND NOT (b2)-[:ID]-(d2) AND NOT (b2)-[:SIBLING]-(d2) AND b2.FORENAME = d2.%6$s_FORENAME AND b2.SURNAME = d2.%6$s_SURNAME AND b2.BIRTH_YEAR = right(d2.%6$s_AGE_OR_DATE_OF_BIRTH, 4)\n" +
                        "AND b1.FORENAME = d1.%6$s_FORENAME AND b1.SURNAME = d1.%6$s_SURNAME AND b1.BIRTH_YEAR = right(d1.%6$s_AGE_OR_DATE_OF_BIRTH, 4)\n" +
                        "AND r.distance <= %3$s AND r.fields_populated >= %4$s\n" +
                        "RETURN count(DISTINCT [b1, b2]) as cluster_count", type1, type2, threshold, fields, partner, partnerCapitalised);

                openSquaresQuery2 = String.format("MATCH (b1:%1$s)-[:SIBLING]-(b2:%1$s),\n" +
                        "(d1:%2$s)-[:SIBLING]-(d2:%2$s),\n" +
                        "(b1)-[r:ID {actors: \"Child-%5$s\"}]-(d1),\n" +
                        "(b2)-[s:ID {actors: \"Child-%5$s\"}]-(d2)\n" +
                        "WHERE id(b1) < id(b2) AND b2.FORENAME = d2.%6$s_FORENAME AND b2.SURNAME = d2.%6$s_SURNAME AND b2.BIRTH_YEAR = right(d2.%6$s_AGE_OR_DATE_OF_BIRTH, 4)\n" +
                        "AND b1.FORENAME = d1.%6$s_FORENAME AND b1.SURNAME = d1.%6$s_SURNAME AND b1.BIRTH_YEAR = right(d1.%6$s_AGE_OR_DATE_OF_BIRTH, 4)\n" +
                        "AND r.distance <= %3$s AND r.fields_populated >= %4$s\n" +
                        "AND (s.fields_populated < %4$s OR s.distance > %3$s)\n" +
                        "RETURN count(DISTINCT [b1, b2]) as cluster_count", type1, type2, threshold, fields, partner, partnerCapitalised);
            }
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