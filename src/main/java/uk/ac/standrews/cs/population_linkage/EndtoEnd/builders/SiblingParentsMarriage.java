/*
 * Copyright 2020 Systems Research Group, University of St Andrews:
 * <https://github.com/stacs-srg>
 */
package uk.ac.standrews.cs.population_linkage.EndtoEnd.builders;

import uk.ac.standrews.cs.storr.impl.LXP;
import uk.ac.standrews.cs.utilities.metrics.coreConcepts.DataDistance;

import java.util.List;

public class SiblingParentsMarriage implements Comparable<SiblingParentsMarriage> {

    public LXP sibling;
    public List<DataDistance<LXP>> parents_marriages;

    public SiblingParentsMarriage(LXP sibling, List<DataDistance<LXP>> parents_marriages) {
        this.sibling = sibling;
        this.parents_marriages = parents_marriages;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SiblingParentsMarriage)) return false;

        SiblingParentsMarriage that = (SiblingParentsMarriage) o;

        return (sibling != null ? !sibling.equals(that.sibling) : that.sibling != null);
    }



    @Override
    public int hashCode() {
        return sibling.hashCode();
    }


    @Override
    public int compareTo(SiblingParentsMarriage other) {
        return (int) (this.sibling.getId() - other.sibling.getId()); // here is hoping!
    }
}
