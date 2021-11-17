/*
 * Copyright 2020 Systems Research Group, University of St Andrews:
 * <https://github.com/stacs-srg>
 */
package uk.ac.standrews.cs.population_linkage.linkageRunners;

import uk.ac.standrews.cs.neoStorr.impl.LXP;
import uk.ac.standrews.cs.population_linkage.helpers.MemoryLogger;
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
import java.util.List;

import static uk.ac.standrews.cs.population_linkage.helpers.RecordFiltering.filter;

public class BitBlasterLinkageRunner extends LinkageRunner{

    @Override
    public LinkageRecipe getLinkageRecipe(String links_persistent_name, String source_repository_name, String results_repository_name, RecordRepository record_repository) {
        return linkage_recipe;
    }

    public Linker getLinker(LinkageRecipe linkageRecipe, List<LXP> reference_points ) {
        Metric<LXP> compositeMetric = getCompositeMetric(linkageRecipe);
        return new SimilaritySearchLinker(getSearchFactory(compositeMetric,reference_points), compositeMetric, linkageRecipe.getThreshold(), getNumberOfProgressUpdates(),
                linkageRecipe.getLinkageType(), "threshold match at ", linkageRecipe.getStoredRole(), linkageRecipe.getQueryRole(), linkageRecipe::isViableLink, linkageRecipe);
    }

    public SearchStructureFactory<LXP> getSearchFactory(Metric<LXP> composite_metric, List<LXP> reference_points) {
        return new BitBlasterSearchStructureFactory<LXP>(composite_metric, reference_points);
    }

    @Override
    protected List<LXP> getReferencePoints() {
        ArrayList<LXP> candidates = filter(linkage_recipe.getLinkageFields().size(), LinkageRecipe.EVERYTHING, linkage_recipe.getStoredRecords(), linkage_recipe.getLinkageFields()); // all populated records
        List<LXP> result = BitBlasterSearchStructure.chooseRandomReferencePoints(candidates, LinkageConfig.numberOfROs);
        return result;
    }

    public LinkageResult link(MakePersistent make_persistent, boolean evaluate_quality, int numberOfGroundTruthTrueLinks, boolean persist_links) throws Exception {

        System.out.println("Adding records into linker @ " + LocalDateTime.now());

        ((SimilaritySearchLinker)linker).addRecords(linkage_recipe.getStoredRecords(), linkage_recipe.getQueryRecords(),getReferencePoints());

        MemoryLogger.update();
        System.out.println("Constructing link iterable @ " + LocalDateTime.now());

        Iterable<Link> links = linker.getLinks();
        LocalDateTime time_stamp = LocalDateTime.now();

        MemoryLogger.update();
        int tp = 0;
        int fp = 0;

        System.out.println("Entering persist and evaluate loop @ " + LocalDateTime.now());

        for (Link linkage_says_true_link : links) {

            if (persist_links) {
                make_persistent.makePersistent(linkage_recipe, linkage_says_true_link);
            }
            if (evaluate_quality) {
                if (doesGTSayIsTrue(linkage_says_true_link)) {
                    tp++;
                } else {
                    fp++;
                }
            }
        }

        System.out.println("Exiting persist and evaluate loop @ " + LocalDateTime.now().toString());

        MemoryLogger.update();

        if (evaluate_quality) {
            System.out.println("Evaluating ground truth @ " + LocalDateTime.now().toString());
            numberOfGroundTruthTrueLinks = linkage_recipe.getNumberOfGroundTruthTrueLinks();
            System.out.println("Number of GroundTruth true Links = " + numberOfGroundTruthTrueLinks);
            LinkageQuality lq = getLinkageQuality(evaluate_quality, numberOfGroundTruthTrueLinks, tp, fp);
            lq.print(System.out);
            return new LinkageResult(lq);
        } else {
            return new LinkageResult(null); // TODO What should this return in this case?
        }
    }
}
