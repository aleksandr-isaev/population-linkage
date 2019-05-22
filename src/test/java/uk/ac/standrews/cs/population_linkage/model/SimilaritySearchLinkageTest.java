package uk.ac.standrews.cs.population_linkage.model;

import uk.ac.standrews.cs.population_linkage.linkage.SearchStructureFactory;
import uk.ac.standrews.cs.population_linkage.linkage.SimilaritySearchLinker;
import uk.ac.standrews.cs.storr.impl.LXP;
import uk.ac.standrews.cs.storr.impl.exceptions.PersistentObjectException;
import uk.ac.standrews.cs.storr.interfaces.IStoreReference;
import uk.ac.standrews.cs.utilities.metrics.coreConcepts.NamedMetric;

public abstract class SimilaritySearchLinkageTest extends LinkageTest {

    @Override
    protected boolean equal(final Link link, final IStoreReference id1, final IStoreReference id2) {

        // Don't care which way round the records are in the pair.
        // The order will depend on which of the record sets was the largest and hence got put into the search structure.

        IStoreReference link_id1 = link.getRole1().getRecordId();
        IStoreReference link_id2 = link.getRole2().getRecordId();

        return (link_id1.equals(id1) && link_id2.equals(id2)) || (link_id1.equals(id2) && link_id2.equals(id1));
    }

     class TestLinker extends SimilaritySearchLinker {

        TestLinker(SearchStructureFactory<LXP> search_structure_factory, double threshold, NamedMetric<LXP> metric, int number_of_progress_updates) {

            super(search_structure_factory, metric, number_of_progress_updates);
            setThreshold(threshold);
        }

        @Override
        protected String getLinkType() {
            return "link type";
        }

        @Override
        protected String getProvenance() {
            return "provenance";
        }

        @Override
        protected String getRoleType1() {
            return "role1";
        }

        @Override
        protected String getRoleType2() {
            return "role2";
        }

        @Override
        public IStoreReference getIdentifier1(LXP record) throws PersistentObjectException {
            return record.getThisRef();
        }

        @Override
        public IStoreReference getIdentifier2(LXP record) throws PersistentObjectException {
            return record.getThisRef();
        }
    }
}
