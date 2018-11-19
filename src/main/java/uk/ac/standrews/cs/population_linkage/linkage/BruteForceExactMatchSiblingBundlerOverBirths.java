package uk.ac.standrews.cs.population_linkage.linkage;

import uk.ac.standrews.cs.population_records.record_types.Birth;
import uk.ac.standrews.cs.storr.impl.LXP;
import uk.ac.standrews.cs.utilities.metrics.coreConcepts.NamedMetric;

public class BruteForceExactMatchSiblingBundlerOverBirths extends BruteForceLinker {

    public BruteForceExactMatchSiblingBundlerOverBirths(NamedMetric<LXP> distance_metric, int number_of_progress_updates) {

        super(distance_metric, number_of_progress_updates);

        setSymmetricalLinks(true);
        setThreshold(0);
    }

    @Override
    protected String getLinkType() {
        return "sibling";
    }

    @Override
    protected String getProvenance() {
        return "exact match";
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
