/*
 * Copyright 2020 Systems Research Group, University of St Andrews:
 * <https://github.com/stacs-srg>
 */
package uk.ac.standrews.cs.population_linkage.linkageRunners;

import uk.ac.standrews.cs.neoStorr.impl.LXP;
import uk.ac.standrews.cs.neoStorr.impl.exceptions.BucketException;
import uk.ac.standrews.cs.neoStorr.impl.exceptions.RepositoryException;
import uk.ac.standrews.cs.population_linkage.linkageRecipes.LinkageRecipe;
import uk.ac.standrews.cs.population_linkage.linkers.Linker;
import uk.ac.standrews.cs.population_linkage.linkers.SimilaritySearchLinker;
import uk.ac.standrews.cs.population_linkage.searchStructures.BitBlasterSearchStructure;
import uk.ac.standrews.cs.population_linkage.searchStructures.BitBlasterSearchStructureFactory;
import uk.ac.standrews.cs.population_linkage.searchStructures.SearchStructureFactory;
import uk.ac.standrews.cs.population_linkage.supportClasses.Link;
import uk.ac.standrews.cs.population_linkage.supportClasses.LinkageConfig;
import uk.ac.standrews.cs.population_linkage.supportClasses.LinkageQuality;
import uk.ac.standrews.cs.population_linkage.supportClasses.LinkageResult;
import uk.ac.standrews.cs.population_records.RecordRepository;
import uk.ac.standrews.cs.utilities.metrics.coreConcepts.Metric;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static uk.ac.standrews.cs.population_linkage.helpers.RecordFiltering.filter;
import static uk.ac.standrews.cs.population_linkage.helpers.RecordFiltering.passesFilter;

public class BitBlasterLinkageRunner extends LinkageRunner {

    @Override
    public LinkageRecipe getLinkageRecipe(String links_persistent_name, String source_repository_name, String results_repository_name, RecordRepository record_repository) {
        return linkage_recipe;
    }

    public Linker getLinker(LinkageRecipe linkageRecipe) {
        Metric<LXP> compositeMetric = linkageRecipe.getCompositeMetric();
        return new SimilaritySearchLinker(getSearchFactory(compositeMetric), compositeMetric, linkageRecipe.getThreshold(), getNumberOfProgressUpdates(),
                linkageRecipe.getLinkageType(), "threshold match at ", linkageRecipe.getStoredRole(), linkageRecipe.getQueryRole(), linkageRecipe::isViableLink, linkageRecipe);
    }

    public SearchStructureFactory<LXP> getSearchFactory(Metric<LXP> composite_metric) {
        return new BitBlasterSearchStructureFactory<>(composite_metric);
    }

    protected List<LXP> getReferencePoints() {
        List<LXP> candidates = filter(linkage_recipe.getLinkageFields().size(), LinkageRecipe.EVERYTHING, linkage_recipe.getStoredRecords(), linkage_recipe.getLinkageFields());
        return BitBlasterSearchStructure.chooseRandomReferencePoints(candidates, LinkageConfig.numberOfROs);
    }

    public LinkageResult link(MakePersistent make_persistent, boolean evaluate_quality, long numberOfGroundTruthTrueLinks, boolean persist_links) throws Exception {

        System.out.println("Adding records into linker @ " + LocalDateTime.now());
        ((SimilaritySearchLinker) linker).addRecords(linkage_recipe.getStoredRecords(), linkage_recipe.getQueryRecords(), getReferencePoints());
        System.out.println("Constructing link iterable @ " + LocalDateTime.now());

        Iterable<Link> links = linker.getLinks();
        List<Link> links_as_list = StreamSupport.stream(links.spliterator(), false).collect(Collectors.toList());

        return processLinks(make_persistent, evaluate_quality, persist_links, links_as_list );
    }

    @Override
    public LinkageResult linkLists(MakePersistent make_persistent, boolean evaluate_quality, long numberOfGroundTruthTrueLinks, boolean persist_links, boolean isIdentityLinkage) throws Exception {
        System.out.println("Adding records into linker @ " + LocalDateTime.now());
        ((SimilaritySearchLinker) linker).addRecords(linkage_recipe.getStoredRecords(), linkage_recipe.getQueryRecords(), getReferencePoints());
        System.out.println("Constructing link iterable @ " + LocalDateTime.now());

        List<Link> linked_pairs = new ArrayList<>();

        for (List<Link> list_of_links : linker.getListsOfLinks() ) {
            if( list_of_links.size() > 0 ) {
                if( ! isIdentityLinkage ) {   // for non identity add all of then for now - TODO EXPLORE THIS.
                    linked_pairs.addAll( list_of_links );
                } else  if( list_of_links.size() == 1 ) { // No choice of links here so add it to the links.
                    linked_pairs.add( list_of_links.get(0) );
                } else {
                    // Only add the closest for now! TODO EXPLORE THIS.
                    addAllEqualToClosest(list_of_links, linked_pairs);
                    showAltDistances( list_of_links );
                }
            }

        }

        return processLinks(make_persistent, evaluate_quality, persist_links, linked_pairs);
    }

    @Override
    protected LinkageResult linkLists2(MakePersistent make_persistent, boolean evaluateQuality, int numberOGroundTruthLinks, boolean persistLinks, boolean isIdentityLinkage) throws Exception {
        System.out.println("Adding records into linker @ " + LocalDateTime.now());
        ((SimilaritySearchLinker) linker).addRecords(linkage_recipe.getStoredRecords(), linkage_recipe.getQueryRecords(), getReferencePoints());
        System.out.println("Constructing lists of lists @ " + LocalDateTime.now());
        List<Link> linked_pairs = processListsOfLists( linker.getListsOfLinks(),isIdentityLinkage );

        return processLinks(make_persistent, true, false, linked_pairs); // params hacked TODO
    }

    /**
     * Adds all same distance as closest to the result set - some will be wrong but cannot differentiate.
     * @param list_of_links - the candidates for potential addition to the results
     * @param results - the result set being returned by the query
     */
    private void addAllEqualToClosest(List<Link> list_of_links, List<Link> results) {
        double closest_dist = list_of_links.get(0).getDistance();
        for( Link link : list_of_links ) {
            if( link.getDistance() == closest_dist ) {
                results.add( link );
            } else {
                return;
            }
        }
    }

    private List<Link> processListsOfLists(Iterable<List<Link>> lists_of_list_of_links, boolean isIdentityLinkage) throws BucketException, RepositoryException {

        // TODO fix isIdentityLinkage if this works! - some code in other linkage linkLists

        List<Link> linked_pairs = new ArrayList<>();
        List<LXP> stored_matched = new ArrayList<>();

        // Link.getRecord1 is the stored record - Birth in test
        // Link.getRecord2 is the query record - Marriage in test

        Map<Long,List<Link>> map_of_links = new HashMap<>();
        for( List<Link> list_of_links : lists_of_list_of_links ) {
            if( list_of_links.size() != 0 ) {
                long query_id = list_of_links.get(0).getRecord2().getReferend().getId(); // Marriage qid in example - checked
                map_of_links.put(query_id, list_of_links);
            }
        }

        System.out.println( "Map size = " + map_of_links.keySet().size() );

        double max_t = linkage_recipe.getThreshold();
        int all_fields = linkage_recipe.getLinkageFields().size();
        final int half_fields = all_fields - (all_fields / 2) + 1;

        for (int required_fields = all_fields; required_fields > half_fields; required_fields--) {
            System.out.println( "Fields = " + required_fields );
            for (double threshold = 0.0; threshold <= max_t; threshold += (max_t / 10)) {
                System.out.println( "Thresh = " + threshold );
                for (Long key : map_of_links.keySet()) {
                    List<Link> list_of_links = map_of_links.get(key);
                    System.out.println( "Find closest in list of size " + list_of_links.size() );
                    int index = getClosestAcceptable(list_of_links, threshold, required_fields, map_of_links, stored_matched);
                    System.out.println( " index = " + index );
                    if (index != -1) {
                        addAllEqualToClosest(list_of_links, index, linked_pairs, threshold, required_fields, map_of_links, stored_matched);
                    }
                }
            }
        }
        return linked_pairs;
    }

    private int getClosestAcceptable(List<Link> list_of_links, double threshold, int fields, Map<Long, List<Link>> search_matched, List<LXP> stored_matched) throws BucketException, RepositoryException {
        int index = 0;
        for( Link link : list_of_links ) {

            if( acceptable( link,threshold,fields,stored_matched ) ) {
                return index;
            }
            index++;
        }
        return -1;
    }

    private boolean acceptable( Link link, double threshold, int required_fields, List<LXP> stored_matched) throws BucketException, RepositoryException {
        LXP rec1 = link.getRecord1().getReferend();
        LXP rec2 = link.getRecord2().getReferend();

        boolean ltt = link.getDistance() <= threshold;
        boolean f1 = passesFilter(rec1, linkage_recipe.getLinkageFields(), required_fields);
        boolean f2 = passesFilter(rec2, linkage_recipe.getQueryMappingFields(), required_fields);  // Not really good enough - fields must match - see samePopulated below
        boolean smc = !stored_matched.contains(rec1);

        boolean result = link.getDistance() <= threshold &&
                passesFilter(rec1, linkage_recipe.getLinkageFields(), required_fields) &&
                passesFilter(rec2, linkage_recipe.getQueryMappingFields(), required_fields) &&
                !stored_matched.contains(rec1);

        String status = result ? "accept" : "reject";
        if( doesGTSayIsTrue(link) ) {
            System.out.println( "accept= " + result + " **GT True**  " + "ltt=" + ltt + "  " + " filt_res=" + f1 + " " + " filt_q=" + f2 + " " + " not before=" + smc );
        } else {
            System.out.println( "accept= " + result + "   GT False    " + "ltt=" + ltt + "  " + " filt_res=" + f1 + " " + " filt_q=" + f2 + " " + " not before=" + smc );
        }
        return result;
    }

    public static boolean samePopulated(LXP record1, List<Integer> filterOn1, LXP record2, List<Integer> filterOn2, long reqPopulatedFields) {

        int same_populated = 0;

        for ( int i = 0; i < filterOn1.size(); i++ ) {

            String value1 = record1.getString(filterOn1.get(i)).toLowerCase().trim();
            String value2 = record2.getString(filterOn2.get(i)).toLowerCase().trim();

            boolean value1_present = ! ( value1.equals("") || value1.contains("missing") || value1.equals("--") || value1.equals("----") );
            boolean value2_present = ! ( value2.equals("") || value2.contains("missing") || value2.equals("--") || value2.equals("----") );

            if( value1_present && value2_present ) {
                same_populated++;
            }
        }

        return same_populated >= reqPopulatedFields;
    }



    private void addResult(Link match, List<Link> linked_pairs, Map<Long, List<Link>> map, List<LXP> stored_matched) throws BucketException, RepositoryException {
        linked_pairs.add( match );
        System.out.println( ( doesGTSayIsTrue(match) ? "TP:" : "FP:" ) + match.getDistance() );
        stored_matched.add( match.getRecord1().getReferend() );
        map.remove( match.getRecord1().getReferend().getId() );
    }

    /**
     * Adds all same distance as closest to the result set - some will be wrong but cannot differentiate.
     * @param list_of_links - the candidates for potential addition to the results
     * @param results - the result set being returned by the query
     * @param stored_matched
     */
    private void addAllEqualToClosest(List<Link> list_of_links, int index, List<Link> results, double threshold, int required_fields, Map<Long, List<Link>> map, List<LXP> stored_matched) throws BucketException, RepositoryException {
        double closest_dist = list_of_links.get(index).getDistance();
        System.out.print( "** " + list_of_links.size() + " ** " );
        for( Link link : list_of_links.subList(index,list_of_links.size()) ) {
            if( link.getDistance() == closest_dist ) {
                if( acceptable( link,threshold,required_fields,stored_matched ) ) {
                    addResult(link, results, map, stored_matched);
               }
            } else {
                return;
            }
        }
    }

    private void showAltDistances(List<Link> list_of_links) {
        StringBuilder sb = new StringBuilder();
        sb.append( "Dists: " );
        for( Link l : list_of_links) {
            sb.append( doesGTSayIsTrue(l) ? "TP:" : "FP:" );
            sb.append( l.getDistance() + "," );
        }
        System.out.println( sb );
    }


    private LinkageResult processLinks(MakePersistent make_persistent, boolean evaluate_quality, boolean persist_links, List<Link> links) {

        System.out.println("Entering persist and evaluate loop @ " + LocalDateTime.now());

        if (persist_links) {
            for (Link linkage_says_true_link : links) {
                make_persistent.makePersistent(linkage_recipe, linkage_says_true_link);
            }
        }

        long tp = 0;
        long fp = 0;

        if (evaluate_quality) {
//            for (Link linkage_says_true_link : links) {
//                if (doesGTSayIsTrue(linkage_says_true_link)) {
//                    tp++;
//                } else {
//                    fp++;
//                }
//            }

            tp = links.parallelStream().filter(l -> doesGTSayIsTrue(l)).count(); // This should be much faster since can run in parallel.
            fp = links.size() - tp;
        }

        System.out.println("Exiting persist and evaluate loop @ " + LocalDateTime.now());

        if (evaluate_quality) {
            LinkageQuality lq = getLinkageQuality(evaluate_quality, tp, fp);
            return new LinkageResult(lq,links);
        } else {
            return new LinkageResult(null, null); // TODO What should this return in this case?
        }
    }

    private LinkageQuality getLinkageQuality(boolean evaluate_quality, long tp, long fp) {
        long numberOfGroundTruthTrueLinks;
        System.out.println("Evaluating ground truth @ " + LocalDateTime.now());
        numberOfGroundTruthTrueLinks = linkage_recipe.getNumberOfGroundTruthTrueLinks();
        System.out.println("Number of GroundTruth true Links = " + numberOfGroundTruthTrueLinks);
        LinkageQuality lq = getLinkageQuality(evaluate_quality, numberOfGroundTruthTrueLinks, tp, fp);
        lq.print(System.out);
        return lq;
    }
}
