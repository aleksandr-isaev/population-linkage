package uk.ac.standrews.cs.population_linkage.linkage;

import uk.ac.standrews.cs.population_linkage.model.*;
import uk.ac.standrews.cs.population_records.record_types.Birth;
import uk.ac.standrews.cs.storr.impl.LXP;
import uk.ac.standrews.cs.utilities.metrics.coreConcepts.DataDistance;

import java.util.List;

public class SimilaritySearchSiblingBundlerOverBirths extends Linker {

    private final SearchStructure<LXP> search_structure;
    private final double threshold;
    private final int number_of_records_to_consider;

    public SimilaritySearchSiblingBundlerOverBirths(SearchStructure<LXP> search_structure, double threshold, int number_of_records_to_consider, int number_of_progress_updates) {

        super(number_of_progress_updates);

        this.search_structure = search_structure;
        this.threshold = threshold;
        this.number_of_records_to_consider = number_of_records_to_consider;
    }

    @Override
    public Links link(List<LXP> records) {

        for (LXP record : records) {
            search_structure.add(record);
        }

        Links links = new Links();

        progress_indicator.setTotalSteps(records.size());

        for (LXP record1 : records) {

            List<DataDistance<LXP>> nearest_records = search_structure.findNearest(record1, number_of_records_to_consider);

            for (DataDistance<LXP> data_distance : nearest_records) {

                LXP record2 = data_distance.value;

                if (!record1.equals(record2) && data_distance.distance <= threshold) {

                    Role role1 = new Role(getIdentifier1(record1), getRoleType1());
                    Role role2 = new Role(getIdentifier2(record2), getRoleType2());
                    links.add(new Link(role1, role2, 1.0f, "threshold match at " + threshold));
                }
            }

            if (number_of_progress_updates > 0) progress_indicator.progressStep();
        }

        return links;
    }

    @Override
    public Links link(List<LXP> records1, List<LXP> records2) {
        return null;
    }

    @Override
    protected String getLinkType() {
        return "sibling";
    }

    @Override
    protected String getProvenance() {
        return "threshold match at " + threshold;
    }

    @Override
    protected String getRoleType1() {
        return Birth.ROLE_BABY;
    }

    @Override
    protected String getRoleType2() {
        return Birth.ROLE_BABY;
    }

    @Override
    protected String getIdentifier1(LXP record) {
        return record.getString(BirthLinkageSubRecord.STANDARDISED_ID);
    }

    @Override
    protected String getIdentifier2(LXP record) {
        return record.getString(BirthLinkageSubRecord.STANDARDISED_ID);
    }
}
