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
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.types.Node;
import uk.ac.standrews.cs.neoStorr.impl.LXP;
import uk.ac.standrews.cs.neoStorr.impl.Store;
import uk.ac.standrews.cs.neoStorr.impl.exceptions.BucketException;
import uk.ac.standrews.cs.neoStorr.interfaces.IBucket;
import uk.ac.standrews.cs.neoStorr.util.NeoDbCypherBridge;
import uk.ac.standrews.cs.population_linkage.compositeMeasures.LXPMeasure;
import uk.ac.standrews.cs.population_linkage.compositeMeasures.SumOfFieldDistances;
import uk.ac.standrews.cs.population_linkage.endToEnd.builders.BirthDeathSiblingBundleBuilder;
import uk.ac.standrews.cs.population_linkage.endToEnd.builders.BirthOwnDeathBuilder;
import uk.ac.standrews.cs.population_linkage.linkageAccuracy.BirthBirthSiblingAccuracy;
import uk.ac.standrews.cs.population_linkage.linkageAccuracy.BirthDeathSiblingAccuracy;
import uk.ac.standrews.cs.population_linkage.linkageRecipes.BirthDeathIdentityLinkageRecipe;
import uk.ac.standrews.cs.population_linkage.linkageRecipes.BirthDeathSiblingLinkageRecipe;
import uk.ac.standrews.cs.population_linkage.linkageRecipes.BirthSiblingLinkageRecipe;
import uk.ac.standrews.cs.population_linkage.resolver.msed.Binomials;
import uk.ac.standrews.cs.population_linkage.resolver.msed.MSED;
import uk.ac.standrews.cs.population_linkage.resolver.msed.OrderedList;
import uk.ac.standrews.cs.population_linkage.supportClasses.Constants;
import uk.ac.standrews.cs.population_records.RecordRepository;
import uk.ac.standrews.cs.population_records.record_types.Birth;
import uk.ac.standrews.cs.population_records.record_types.Death;
import uk.ac.standrews.cs.utilities.measures.coreConcepts.StringMeasure;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static uk.ac.standrews.cs.population_linkage.linkageRecipes.LinkageRecipe.list;

public class BirthDeathOpenTriangleResolver {

    final static int NUM_OF_CHILDREN  = 12;
    final static int MAX_AGE_DIFFERENCE  = 23;
    final static double DATE_THRESHOLD = 0.8;
    final static int BIRTH_INTERVAL = 280;
    private static final String BB_SIBLING_QUERY = "MATCH (a:Birth), (b:Birth) WHERE a.STANDARDISED_ID = $standard_id_from AND b.STANDARDISED_ID = $standard_id_to MERGE (a)-[r:SIBLING { provenance: $prov, actors: \"Child-Child\" } ]-(b)";
    private static final String BB_SIBLING_QUERY_DEL = "MATCH (a:Birth)-[r:SIBLING]-(b:Death) WHERE a.STANDARDISED_ID = $standard_id_from AND b.STANDARDISED_ID = $standard_id_to DELETE r";
    private static final String BB_SIBLING_QUERY_DEL_PROV = "MATCH (a:Birth), (b:Death) WHERE a.STANDARDISED_ID = $standard_id_from AND b.STANDARDISED_ID = $standard_id_to MERGE (a)-[r:DELETED { provenance: $prov } ]-(b)";

    private static NeoDbCypherBridge bridge;

    private static String[] creationPredicates = {"match_m_date_bd"};
    private static String[] deletionPredicates = {"max_age_range", "min_b_interval", "birthplace_mode", "bad_m_date", "msed"};

    public static void main(String[] args) throws BucketException {
        bridge = Store.getInstance().getBridge();
        RecordRepository record_repository = new RecordRepository("umea");
        final StringMeasure base_measure = Constants.LEVENSHTEIN;;
        final LXPMeasure composite_measure_name = getCompositeMeasure(base_measure);
        final LXPMeasure composite_measure_date = getCompositeMeasureDate(base_measure);
        final LXPMeasure composite_measure_bd = getCompositeMeasureBirthDeath(base_measure);
        IBucket births = record_repository.getBucket("birth_records");
        IBucket deaths = record_repository.getBucket("death_records");
        BirthDeathSiblingLinkageRecipe recipe = new BirthDeathSiblingLinkageRecipe("umea", "EVERYTHING", BirthDeathSiblingBundleBuilder.class.getName(), null);

        System.out.println("Before");
        PatternsCounter.countOpenTrianglesToString(bridge, "Birth", "Death");
        PatternsCounter.countOpenTrianglesToString(bridge, "Birth", "Birth");
        new BirthDeathSiblingAccuracy(bridge);
        new BirthBirthSiblingAccuracy(bridge);

        System.out.println("Locating triangles...");
        List<OpenTriangleCluster> triangles = findIllegalBirthDeathSiblingTriangles(bridge);
        System.out.println("Triangle clusters found: " + triangles.size());

        System.out.println("Resolving triangles with MSED...");
        for (OpenTriangleCluster triangle : triangles) {
            resolveTrianglesMSED(triangle.getTriangleChain(), triangle.x, record_repository, recipe, 2, 4);
        }

        System.out.println("Resolving triangles...");
        for (OpenTriangleCluster triangle : triangles) {
            for (List<Long> chain : triangle.getTriangleChain()){
                LXP[] tempKids = {(LXP) births.getObjectById(triangle.x), (LXP) deaths.getObjectById(chain.get(0)), (LXP) births.getObjectById(chain.get(1))};
                String std_id_x = tempKids[0].getString(Birth.STANDARDISED_ID);
                String std_id_y = tempKids[1].getString(Death.STANDARDISED_ID);
                String std_id_z = tempKids[2].getString(Birth.STANDARDISED_ID);

                triangle.getYearStatistics();
                boolean hasChanged = false;

                String toFind = "7106096";
//                    if(Objects.equals(std_id_z, toFind) || Objects.equals(std_id_y, toFind) || Objects.equals(std_id_x, toFind)){
//                        System.out.println("fsd");
//                    }

                //1. Check age of child not outside of max difference
                hasChanged = maxRangePredicate(triangle, tempKids, hasChanged, 0);

                //2. check DOB at least 9 months away from rest
                hasChanged = minBirthIntervalPredicate(triangle, tempKids, hasChanged, 1);

                //3. Get mode of birthplace
                hasChanged = mostCommonBirthPlacePredicate(triangle, hasChanged, tempKids, 2);

                //4. If same marriage date and pass other checks, create link. Match for same birthplace as well?
                if(!hasChanged && getDistance(triangle.x, chain.get(1), composite_measure_date, births) < DATE_THRESHOLD &&
                        !Objects.equals(tempKids[0].getString(Birth.PARENTS_YEAR_OF_MARRIAGE), "----") &&
                        !Objects.equals(tempKids[2].getString(Birth.PARENTS_YEAR_OF_MARRIAGE), "----")){
                    createLink(bridge, std_id_x, std_id_z, creationPredicates[0]);
                }
            }
        }

        System.out.println("After");
        PredicateEfficacy pef = new PredicateEfficacy(); //get efficacy of each predicate
        pef.countSiblingEfficacy(new String[0], deletionPredicates, "Birth", "Death");
        pef.countSiblingEfficacy(creationPredicates, new String[0], "Birth", "Birth");
        PatternsCounter.countOpenTrianglesToString(bridge, "Birth", "Death");
        PatternsCounter.countOpenTrianglesToString(bridge, "Birth", "Birth");
        new BirthDeathSiblingAccuracy(bridge);
        new BirthBirthSiblingAccuracy(bridge);
    }

    private static List<OpenTriangleCluster> findIllegalBirthDeathSiblingTriangles(NeoDbCypherBridge bridge) {
        final String BIRTH_SIBLING_TRIANGLE_QUERY = "MATCH (x:Birth)-[:SIBLING]-(y:Death)-[:SIBLING]-(z:Birth)\n"+
                "WHERE NOT (x)-[:SIBLING]-(z) AND NOT (x)-[:DELETED]-(y) AND NOT (z)-[:DELETED]-(y)\n" +
                "RETURN x, collect([y, z]) AS openTriangles";
        Result result = bridge.getNewSession().run(BIRTH_SIBLING_TRIANGLE_QUERY);
        return result.stream().map(r -> {
            long x = ((Node) r.asMap().get("x")).get("STORR_ID").asLong();
            List<List<Node>> openTrianglesNodes = (List<List<Node>>) r.asMap().get("openTriangles");

            List<List<Long>> openTrianglesList = openTrianglesNodes
                    .stream()
                    .map(innerList -> innerList.stream()
                            .map(obj -> {
                                if (obj instanceof Node) {
                                    return ((Node) obj).get("STORR_ID").asLong();
                                } else {
                                    throw new IllegalArgumentException("Expected a Node but got: " + obj.getClass());
                                }
                            })
                            .collect(Collectors.toList()))
                    .collect(Collectors.toList());

            return new OpenTriangleCluster(x, openTrianglesList);
        }).collect(Collectors.toList());
    }

    private static boolean maxRangePredicate(OpenTriangleCluster triangle, LXP[] tempKids, boolean hasChanged, int predNumber) {
        String std_id_x = tempKids[0].getString(Birth.STANDARDISED_ID);
        String std_id_y = tempKids[1].getString(Death.STANDARDISED_ID);
        String std_id_z = tempKids[2].getString(Birth.STANDARDISED_ID);

        if(!Objects.equals(tempKids[0].getString(Birth.BIRTH_YEAR), "----") && !Objects.equals(tempKids[1].getString(Death.DATE_OF_BIRTH), "--/--/----") &&
                Math.abs(triangle.getYearMedian() - Integer.parseInt(tempKids[0].getString(Birth.BIRTH_YEAR))) > MAX_AGE_DIFFERENCE &&
                Math.abs(Integer.parseInt((tempKids[1].getString(Death.DATE_OF_BIRTH)).substring(6)) - Integer.parseInt(tempKids[0].getString(Birth.BIRTH_YEAR))) > MAX_AGE_DIFFERENCE){
//                        deleteLink(bridge, std_id_x, std_id_y);
            deleteLink(bridge, std_id_x, std_id_y, deletionPredicates[predNumber]);
            hasChanged = true;
        } else if (!Objects.equals(tempKids[2].getString(Birth.BIRTH_YEAR), "----") && !Objects.equals(tempKids[1].getString(Death.DATE_OF_BIRTH), "--/--/----") &&
                Math.abs(triangle.getYearMedian() - Integer.parseInt(tempKids[2].getString(Birth.BIRTH_YEAR))) > MAX_AGE_DIFFERENCE &&
                Math.abs(Integer.parseInt((tempKids[1].getString(Death.DATE_OF_BIRTH)).substring(6))- Integer.parseInt(tempKids[2].getString(Birth.BIRTH_YEAR))) > MAX_AGE_DIFFERENCE){
//                        deleteLink(bridge, std_id_z, std_id_y);
            deleteLink(bridge, std_id_z, std_id_y, deletionPredicates[predNumber]);
            hasChanged = true;
        } else if (!Objects.equals(tempKids[0].getString(Birth.BIRTH_YEAR), "----") && !Objects.equals(tempKids[1].getString(Death.DATE_OF_BIRTH), "--/--/----")  &&
                Math.abs(triangle.getYearMedian() - Integer.parseInt((tempKids[1].getString(Death.DATE_OF_BIRTH)).substring(6))) > MAX_AGE_DIFFERENCE &&
                Math.abs(Integer.parseInt((tempKids[1].getString(Death.DATE_OF_BIRTH)).substring(6)) - Integer.parseInt(tempKids[0].getString(Birth.BIRTH_YEAR))) > MAX_AGE_DIFFERENCE) {
//                        deleteLink(bridge, std_id_z, std_id_y);
//                        deleteLink(bridge, std_id_x, std_id_y);
            deleteLink(bridge, std_id_x, std_id_y, deletionPredicates[predNumber]);
            hasChanged = true;
        } else if (!Objects.equals(tempKids[2].getString(Birth.BIRTH_YEAR), "----") && !Objects.equals(tempKids[1].getString(Death.DATE_OF_BIRTH), "--/--/----")  &&
                Math.abs(triangle.getYearMedian() - Integer.parseInt((tempKids[1].getString(Death.DATE_OF_BIRTH)).substring(6))) > MAX_AGE_DIFFERENCE &&
                Math.abs(Integer.parseInt((tempKids[1].getString(Death.DATE_OF_BIRTH)).substring(6)) - Integer.parseInt(tempKids[2].getString(Birth.BIRTH_YEAR))) > MAX_AGE_DIFFERENCE){
            deleteLink(bridge, std_id_z, std_id_y, deletionPredicates[predNumber]);
            hasChanged = true;
        }

        return hasChanged;
    }

    //https://stackoverflow.com/a/67767630
    private static boolean minBirthIntervalPredicate(OpenTriangleCluster triangle, LXP[] tempKids, boolean hasChanged, int predNumber) {
        String std_id_x = tempKids[0].getString(Birth.STANDARDISED_ID);
        String std_id_y = tempKids[1].getString(Death.STANDARDISED_ID);
        String std_id_z = tempKids[2].getString(Birth.STANDARDISED_ID);

        for (int i = 0; i < tempKids.length; i+=2) {
            try{
                LocalDate childDate = getBirthdayAsDate(tempKids[i], false);
                LocalDate dateY = getBirthdayAsDate(tempKids[1], true);
                if(!hasChanged && Math.abs(ChronoUnit.DAYS.between(dateY, childDate)) < BIRTH_INTERVAL && Math.abs(ChronoUnit.DAYS.between(dateY, childDate)) > 2){
                    if(i == 0){
                        deleteLink(bridge, std_id_x, std_id_y, deletionPredicates[predNumber]);
                    }else{
                        deleteLink(bridge, std_id_z, std_id_y, deletionPredicates[predNumber]);
                    }
                    hasChanged = true;
                }
            }catch (Exception e){

            }
        }

        return hasChanged;
    }

    private static LocalDate getBirthdayAsDate(LXP child, boolean isDead){
        int day = 1;

        if(isDead){
            //if missing day, set to first of month
            if(!Objects.equals(child.getString(Death.DATE_OF_BIRTH).substring(0, 2), "--")){
                day = Integer.parseInt(child.getString(Death.DATE_OF_BIRTH).substring(0, 2));
            }

            //get date
            return LocalDate.of(Integer.parseInt(child.getString(Death.DATE_OF_BIRTH).substring(6)), Integer.parseInt(child.getString(Death.DATE_OF_BIRTH).substring(3, 5)), day);
        }else{
            //if missing day, set to first of month
            if(!Objects.equals(child.getString(Birth.BIRTH_DAY), "--")){
                day = Integer.parseInt(child.getString(Birth.BIRTH_DAY));
            }

            //get date
            return LocalDate.of(Integer.parseInt(child.getString(Birth.BIRTH_YEAR)), Integer.parseInt(child.getString(Birth.BIRTH_MONTH)), day);
        }

    }

    private static boolean mostCommonBirthPlacePredicate(OpenTriangleCluster triangle, boolean hasChanged, LXP[] tempKids, int predNumber) {
        int MIN_FAMILY_SIZE = 3;
        String std_id_x = tempKids[0].getString(Birth.STANDARDISED_ID);
        String std_id_y = tempKids[1].getString(Death.STANDARDISED_ID);
        String std_id_z = tempKids[2].getString(Birth.STANDARDISED_ID);

        if(!hasChanged && !Objects.equals(tempKids[1].getString(Death.PLACE_OF_DEATH), "----") && ((!Objects.equals(tempKids[1].getString(Death.AGE_AT_DEATH), "") && !Objects.equals(tempKids[0].getString(Birth.BIRTH_ADDRESS), "----") && Integer.parseInt(tempKids[1].getString(Death.AGE_AT_DEATH)) < triangle.getAgeRange() / 2) ||
                (!Objects.equals(tempKids[1].getString(Death.DEATH_YEAR), "----") && !Objects.equals(tempKids[1].getString(Death.DATE_OF_BIRTH), "--/--/----") &&
                        Integer.parseInt(tempKids[1].getString(Death.DEATH_YEAR)) - Integer.parseInt((tempKids[1].getString(Death.DATE_OF_BIRTH)).substring(6)) < triangle.getAgeRange() / 2)) &&
                !Objects.equals(tempKids[0].getString(Birth.BIRTH_ADDRESS), tempKids[1].getString(Death.PLACE_OF_DEATH)) && !Objects.equals(tempKids[0].getString(Birth.BIRTH_ADDRESS), triangle.getMostCommonBirthplace()) && triangle.getNumOfChildren() > MIN_FAMILY_SIZE){
//                        deleteLink(bridge, std_id_x, std_id_y);
            deleteLink(bridge, std_id_x, std_id_y, deletionPredicates[predNumber]);;
            hasChanged = true;
        } else if (!hasChanged && !Objects.equals(tempKids[1].getString(Death.PLACE_OF_DEATH), "----") && ((!Objects.equals(tempKids[1].getString(Death.AGE_AT_DEATH), "") && !Objects.equals(tempKids[2].getString(Birth.BIRTH_ADDRESS), "----") && Integer.parseInt(tempKids[1].getString(Death.AGE_AT_DEATH)) < triangle.getAgeRange() / 2) ||
                (!Objects.equals(tempKids[1].getString(Death.DEATH_YEAR), "----") && !Objects.equals(tempKids[1].getString(Death.DATE_OF_BIRTH), "--/--/----") &&
                        Integer.parseInt(tempKids[1].getString(Death.DEATH_YEAR)) - Integer.parseInt((tempKids[1].getString(Death.DATE_OF_BIRTH)).substring(6)) < triangle.getAgeRange() / 2)) &&
                !Objects.equals(tempKids[2].getString(Birth.BIRTH_ADDRESS), tempKids[1].getString(Death.PLACE_OF_DEATH)) && !Objects.equals(tempKids[2].getString(Birth.BIRTH_ADDRESS), triangle.getMostCommonBirthplace()) && triangle.getNumOfChildren() > MIN_FAMILY_SIZE) {
//                        deleteLink(bridge, std_id_z, std_id_y);
            deleteLink(bridge, std_id_z, std_id_y, deletionPredicates[predNumber]);
            hasChanged = true;
        }

        return hasChanged;
    }

    public static void resolveTrianglesMSED(List<List<Long>> triangleChain, Long x, RecordRepository record_repository, BirthDeathSiblingLinkageRecipe recipe, int cPred, int dPred) throws BucketException {
        double THRESHOLD = 0.04;
        double TUPLE_THRESHOLD = 0.02;

        List<Set<LXP>> familySets = new ArrayList<>();
        List<List<LXP>> toDelete = new ArrayList<>();
        Set<LXP> fixedChildren = new HashSet<LXP>();
        int[] fieldsB = {Birth.FATHER_FORENAME, Birth.MOTHER_FORENAME, Birth.FATHER_SURNAME, Birth.MOTHER_MAIDEN_SURNAME};
        int[] fieldsD = {Death.FATHER_FORENAME, Death.MOTHER_FORENAME, Death.FATHER_SURNAME, Death.MOTHER_MAIDEN_SURNAME};

        for (List<Long> chain : triangleChain){
            List<Long> listWithX = new ArrayList<>(Arrays.asList(x));
            listWithX.addAll(chain);
            List<LXP> bs = getRecords(listWithX, record_repository);

            for (int i = 0; i < bs.size(); i++) {
                //1. DOTTER/SON
                String dotterRegex = "D[.:ORT](?!.*D[.:RT])";
                Pattern pattern = Pattern.compile(dotterRegex);
                Matcher matcher = pattern.matcher(bs.get(i).getString(Birth.MOTHER_MAIDEN_SURNAME));
                if (matcher.find()) {
                    String newString = bs.get(i).getString(Birth.MOTHER_MAIDEN_SURNAME).substring(0, matcher.start()) + "DOTTER";
                    bs.get(i).put(Birth.MOTHER_MAIDEN_SURNAME, newString);
                }

                String sonRegex = "S[.]";
                pattern = Pattern.compile(sonRegex);
                matcher = pattern.matcher(bs.get(i).getString(Birth.FATHER_SURNAME));
                if (matcher.find()) {
                    String newString = bs.get(i).getString(Birth.FATHER_SURNAME).substring(0, matcher.start()) + "SON";
                    bs.get(i).put(Birth.FATHER_SURNAME, newString);
                }

                //2. Initials or incomplete names
                String initialRegex = "^[A-Z]*\\.$";
                pattern = Pattern.compile(initialRegex);
                for (int j = 0; j < fieldsB.length - 3; j++) {
                    if(i == 1){
                        matcher = pattern.matcher(bs.get(i).getString(fieldsD[j]));
                    }else{
                        matcher = pattern.matcher(bs.get(i).getString(fieldsB[j]));
                    }

                    if(matcher.find()){
                        String substringX = bs.get(0).getString(fieldsB[j]).length() >= matcher.end() - 1 ? bs.get(0).getString(fieldsB[j]).substring(matcher.start(), matcher.end() - 1) : bs.get(0).getString(j);
                        String substringY = bs.get(1).getString(fieldsD[j]).length() >= matcher.end() - 1 ? bs.get(1).getString(fieldsD[j]).substring(matcher.start(), matcher.end() - 1) : bs.get(1).getString(j);
                        String substringZ = bs.get(2).getString(fieldsB[j]).length() >= matcher.end() - 1 ? bs.get(2).getString(fieldsB[j]).substring(matcher.start(), matcher.end() - 1) : bs.get(2).getString(j);

                        if (i == 0 && substringX.equals(substringY) && substringX.equals(substringZ)) {
                            bs.get(0).put(fieldsB[j], bs.get(0).getString(fieldsB[j]).replace(".", ""));
                            bs.get(1).put(fieldsD[j], bs.get(0).getString(fieldsD[j]).substring(matcher.start(), matcher.end() - 1));
                            bs.get(2).put(fieldsB[j], bs.get(0).getString(fieldsB[j]).substring(matcher.start(), matcher.end() - 1));
                        } else if (i == 1 && substringY.equals(substringX) && substringY.equals(substringZ)) {
                            bs.get(1).put(fieldsB[j], bs.get(1).getString(fieldsB[j]).replace(".", ""));
                            bs.get(0).put(fieldsD[j], bs.get(1).getString(fieldsD[j]).substring(matcher.start(), matcher.end() - 1));
                            bs.get(2).put(fieldsB[j], bs.get(1).getString(fieldsB[j]).substring(matcher.start(), matcher.end() - 1));
                        } else if (i == 2 && substringZ.equals(substringX) && substringZ.equals(substringY)) {
                            bs.get(2).put(fieldsB[j], bs.get(2).getString(fieldsB[j]).replace(".", ""));
                            bs.get(0).put(fieldsD[j], bs.get(2).getString(fieldsD[j]).substring(matcher.start(), matcher.end() - 1));
                            bs.get(1).put(fieldsB[j], bs.get(2).getString(fieldsB[j]).substring(matcher.start(), matcher.end() - 1));
                        }
                    }
                }

                //3. Middle names and double barrel surnames
                for (int j = 0; j < fieldsB.length - 1; j++) {
                    if (bs.get(i).getString(fieldsB[j]).contains(" ") || bs.get(i).getString(fieldsD[j]).contains(" ")) {
                        if (i == 0 && !bs.get(2).getString(fieldsB[j]).contains(" ")) {
                            String[] names = bs.get(0).getString(fieldsB[j]).split("\\s+");
                            for (String name : names) {
                                if (name.equals(bs.get(2).getString(fieldsB[j]))) {
                                    bs.get(0).put(fieldsB[j], name);
                                    break;
                                }
                            }
                        } else if(i == 1 && (!bs.get(0).getString(fieldsB[j]).contains(" ") || !bs.get(2).getString(fieldsB[j]).contains(" "))) {
                            String[] names = bs.get(1).getString(fieldsD[j]).split("\\s+");
                            for (String name : names) {
                                if (name.equals(bs.get(0).getString(fieldsB[j]))) {
                                    bs.get(1).put(fieldsD[j], name);
                                    break;
                                }
                            }
                            for (String name : names) {
                                if (name.equals(bs.get(2).getString(fieldsB[j]))) {
                                    bs.get(1).put(fieldsD[j], name);
                                    break;
                                }
                            }
                        } else if(i == 2 && !bs.get(0).getString(fieldsB[j]).contains(" ")) {
                            String[] names = bs.get(2).getString(fieldsB[j]).split("\\s+");
                            for (String name : names) {
                                if (name.equals(bs.get(0).getString(fieldsB[j]))) {
                                    bs.get(2).put(fieldsB[j], name);
                                    break;
                                }
                            }
                        }
                    }
                }

                //4. Parentheses
                for (int j = 0; j < fieldsB.length - 1; j++) {
                    String parenthesesRegex = "\\(([^)]+)\\)";
                    pattern = Pattern.compile(parenthesesRegex);
                    if(i == 1){
                        matcher = pattern.matcher(bs.get(i).getString(fieldsD[j]));

                        if (matcher.find() && matcher.start() > 0) {
                            String newString = bs.get(i).getString(fieldsD[j]).substring(0, matcher.start()).strip();
                            bs.get(i).put(fieldsD[j], newString);
                        }
                    }else{
                        matcher = pattern.matcher(bs.get(i).getString(fieldsB[j]));

                        if (matcher.find() && matcher.start() > 0) {
                            String newString = bs.get(i).getString(fieldsB[j]).substring(0, matcher.start()).strip();
                            bs.get(i).put(fieldsB[j], newString);
                        }
                    }
                }
            }

            double distance = getMSEDForCluster(bs, recipe);
            double distanceXY = getMSEDForCluster(bs.subList(0, 2), recipe);
            double distanceZY = getMSEDForCluster(bs.subList(1, 3), recipe);

            if(distance < THRESHOLD) {
                addFamilyMSED(familySets, bs);
            }else if(distance > THRESHOLD){
                toDelete.add(bs);
                if(distanceXY < TUPLE_THRESHOLD){
                    addFamilyMSED(familySets, bs.subList(0, 2));
                }
                if (distanceZY < TUPLE_THRESHOLD){
                    addFamilyMSED(familySets, bs.subList(1, 3));
                }
            }
        }

        List<Set<LXP>> setsToRemove = new ArrayList<>();
        List<Set<LXP>> setsToAdd = new ArrayList<>();

        for (Set<LXP> fSet : familySets) {
            int k = 3;
            if (fSet.size() >= k) {
                OrderedList<List<LXP>,Double> familySetMSED = getMSEDForK(fSet, k, recipe);
                List<Double> distances = familySetMSED.getComparators();
                List<List<LXP>> records = familySetMSED.getList();
                List<Set<LXP>> newSets = new ArrayList<>();

                newSets.add(new HashSet<>(records.get(0)));

                for (int i = 1; i < distances.size(); i++) {
                    if ((distances.get(i) - distances.get(i - 1)) / distances.get(i - 1) > 0.5 || distances.get(i) > 0.01) {
                        break;
                    } else {
                        boolean familyFound = false;
                        for (Set<LXP> nSet : newSets) {
                            if (familyFound) {
                                break;
                            }
                            for (int j = 0; j < records.get(i).size(); j++) {
                                if (nSet.contains(records.get(i).get(j))) {
                                    nSet.addAll(records.get(i));
                                    familyFound = true;
                                    break;
                                }
                            }
                        }

                        if (!familyFound) {
                            newSets.add(new HashSet<>(records.get(i)));
                        }
                    }
                }

                setsToRemove.add(fSet);
                setsToAdd.addAll(newSets);
            }
        }

        familySets.removeAll(setsToRemove);
        familySets.addAll(setsToAdd);

        for (List<LXP> triangleToDelete : toDelete) {
//            String toFind = "244425";
//            String toFind2 = "235074";
//            if((Objects.equals(triangleToDelete.get(0).getString(Birth.STANDARDISED_ID), toFind) || Objects.equals(triangleToDelete.get(1).getString(Birth.STANDARDISED_ID), toFind) || Objects.equals(triangleToDelete.get(2).getString(Birth.STANDARDISED_ID), toFind)) && familySets.size() > 0 &&
//                    (Objects.equals(triangleToDelete.get(0).getString(Birth.STANDARDISED_ID), toFind2) || Objects.equals(triangleToDelete.get(1).getString(Birth.STANDARDISED_ID), toFind2) || Objects.equals(triangleToDelete.get(2).getString(Birth.STANDARDISED_ID), toFind2))) {
//                System.out.println("fsd");
//            }

            for(Set<LXP> fSet : familySets) {
                int kidsFound = 0;
                List<Integer> kidsIndex = new ArrayList<>(Arrays.asList(0, 1, 2));
                for (int i = 0; i < triangleToDelete.size(); i++) {
                    if(fSet.contains(triangleToDelete.get(i))) {
                        kidsIndex.remove((Integer.valueOf(i)));
                        kidsFound++;
                    }
                }

                if(kidsFound == 2 && kidsIndex.size() == 1) {
                    if(kidsIndex.get(0) == 0){
                        deleteLink(bridge, triangleToDelete.get(0).getString(Birth.STANDARDISED_ID), triangleToDelete.get(1).getString(Birth.STANDARDISED_ID), deletionPredicates[dPred]);
                        break;
                    } else if (kidsIndex.get(0) == 2) {
                        deleteLink(bridge, triangleToDelete.get(2).getString(Birth.STANDARDISED_ID), triangleToDelete.get(1).getString(Birth.STANDARDISED_ID), deletionPredicates[dPred]);
                        break;
                    }
                }
            }
        }
    }

    private static void addFamilyMSED(List<Set<LXP>> familySets, List<LXP> bs) {
        if(familySets.isEmpty()) {
            familySets.add(new HashSet<>(bs));
        }else{
            boolean familyFound = false;
            for(Set<LXP> fSet : familySets) {
                if(familyFound){
                    break;
                }
                for (int i = 0; i < bs.size(); i++) {
                    if(fSet.contains(bs.get(i))) {
                        fSet.addAll(bs);
                        familyFound = true;
                        break;
                    }
                }
            }
            if(!familyFound) {
                familySets.add(new HashSet<>(bs));
            }
        }
    }

    private static void deleteLink(NeoDbCypherBridge bridge, String std_id_x, String std_id_y){
        try (Session session = bridge.getNewSession(); Transaction tx = session.beginTransaction();) {
            Map<String, Object> parameters = getCreationParameterMap(std_id_x, std_id_y);
            tx.run(BB_SIBLING_QUERY_DEL, parameters);
            tx.commit();
        }
    }

    private static void deleteLink(NeoDbCypherBridge bridge, String std_id_x, String std_id_y, String prov){
        try (Session session = bridge.getNewSession(); Transaction tx = session.beginTransaction();) {
            Map<String, Object> parameters = getCreationParameterMap(std_id_x, std_id_y, prov);
            tx.run(BB_SIBLING_QUERY_DEL_PROV, parameters);
            tx.commit();
        }
    }

    private static void createLink(NeoDbCypherBridge bridge, String std_id_x, String std_id_z, String prov) {
        try (Session session = bridge.getNewSession(); Transaction tx = session.beginTransaction()) {
            Map<String, Object> parameters = getCreationParameterMap(std_id_x, std_id_z, prov);
            tx.run(BB_SIBLING_QUERY, parameters);
            tx.commit();
        }
    }

    protected static LXPMeasure getCompositeMeasure(StringMeasure base_measure) {
        final List<Integer> LINKAGE_FIELDS_NAME = list(
                Birth.MOTHER_FORENAME,
                Birth.MOTHER_MAIDEN_SURNAME,
                Birth.FATHER_FORENAME,
                Birth.FATHER_SURNAME
        );

        return new SumOfFieldDistances(base_measure, LINKAGE_FIELDS_NAME);
    }

    protected static LXPMeasure getCompositeMeasureDate(StringMeasure base_measure) {
        final List<Integer> LINKAGE_FIELDS = list(
                Birth.PARENTS_DAY_OF_MARRIAGE,
                Birth.PARENTS_MONTH_OF_MARRIAGE,
                Birth.PARENTS_YEAR_OF_MARRIAGE
        );

        return new SumOfFieldDistances(base_measure, LINKAGE_FIELDS);
    }

    protected static LXPMeasure getCompositeMeasureBirthDeath(StringMeasure base_measure) {
        final List<Integer> LINKAGE_FIELDS_BIRTH = list(
                Birth.MOTHER_FORENAME,
                Birth.MOTHER_MAIDEN_SURNAME,
                Birth.FATHER_FORENAME,
                Birth.FATHER_SURNAME
        );

        final List<Integer> LINKAGE_FIELDS_DEATH = list(
                Death.MOTHER_FORENAME,
                Death.MOTHER_MAIDEN_SURNAME,
                Death.FATHER_FORENAME,
                Death.FATHER_SURNAME
        );

        return new SumOfFieldDistances(base_measure, LINKAGE_FIELDS_BIRTH, LINKAGE_FIELDS_DEATH);
    }

    private static double getDistance(long id1, long id2, LXPMeasure composite_measure, IBucket births) throws BucketException {
        LXP b1 = (LXP) births.getObjectById(id1);
        LXP b2 = (LXP) births.getObjectById(id2);
        return composite_measure.distance(b1, b2);
    }

    private static Map<String, Object> getCreationParameterMap(String standard_id_from, String standard_id_to) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("standard_id_from", standard_id_from);
        parameters.put("standard_id_to", standard_id_to);
        return parameters;
    }

    private static Map<String, Object> getCreationParameterMap(String standard_id_from, String standard_id_to, String prov) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("standard_id_from", standard_id_from);
        parameters.put("standard_id_to", standard_id_to);
        parameters.put("prov", prov);
        return parameters;
    }

    public static double getMSEDForCluster(List<LXP> choices, BirthDeathSiblingLinkageRecipe recipe) {
        /* Calculate the MESD for the cluster represented by the indices choices into bs */
        List<String> fields_from_choices = new ArrayList<>(); // a list of the concatenated linkage fields from the selected choices.
        List<Integer> linkage_fields = recipe.getLinkageFields(); // the linkage field indexes to be used
        for (LXP record : choices) {
            StringBuilder sb = new StringBuilder();              // make a string of values for this record drawn from the recipe linkage fields
            for (int field_selector : linkage_fields) {
                sb.append(record.get(field_selector) + "/");
            }
            fields_from_choices.add(sb.toString()); // add the linkage fields for this choice to the list being assessed
        }
        return MSED.distance(fields_from_choices);
    }

    private static OrderedList<List<LXP>,Double> getMSEDForK(Set<LXP> family, int k, BirthDeathSiblingLinkageRecipe recipe) throws BucketException {
        OrderedList<List<LXP>,Double> all_mseds = new OrderedList<>(Integer.MAX_VALUE); // don't want a limit!
        List<LXP> bs = new ArrayList<>(family);

        List<List<Integer>> indices = Binomials.pickAll(bs.size(), k);
        for (List<Integer> choices : indices) {
            List<LXP> records = getBirthsFromChoices(bs, choices);
            double distance = getMSEDForCluster(records, recipe);
            all_mseds.add(records,distance);
        }
        return all_mseds;
    }

    private static List<LXP> getBirthsFromChoices(List<LXP> bs, List<Integer> choices) {
        List<LXP> records = new ArrayList<>();
        for (int index : choices) {
            records.add( bs.get(index) );
        }
        return records;
    }

    /**
     * Method to get birth/death objects based on storr IDs
     *
     * @param sibling_ids ids of records to find
     * @param record_repository repository of where records stored
     * @return list of birth/death objects
     * @throws BucketException
     */
    public static List<LXP> getRecords(List<Long> sibling_ids, RecordRepository record_repository) throws BucketException {
        IBucket<Birth> births = record_repository.getBucket("birth_records");
        IBucket<Death> deaths = record_repository.getBucket("death_records");
        ArrayList<LXP> bs = new ArrayList();

        for (int i = 0; i < sibling_ids.size(); i++) {
            if(i == 1){
                bs.add(deaths.getObjectById(sibling_ids.get(i)));
            }else{
                bs.add(births.getObjectById(sibling_ids.get(i)));
            }
        }

        return bs;
    }
}