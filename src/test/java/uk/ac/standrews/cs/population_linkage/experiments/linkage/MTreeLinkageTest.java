package uk.ac.standrews.cs.population_linkage.experiments.linkage;

import uk.ac.standrews.cs.population_linkage.experiments.SearchStructures.MTreeSearchStructure;
import uk.ac.standrews.cs.population_linkage.experiments.SearchStructures.SearchStructure;
import uk.ac.standrews.cs.population_linkage.experiments.SearchStructures.SearchStructureFactory;
import uk.ac.standrews.cs.storr.impl.LXP;

public class MTreeLinkageTest extends SimilaritySearchLinkageTest {

    @Override
    public Linker getLinker() {

        SearchStructureFactory<LXP> factory = new SearchStructureFactory<LXP>() {
            @Override
            public SearchStructure<LXP> newSearchStructure(final Iterable<LXP> records) {
                return new MTreeSearchStructure<>(metric, records);
            }

            @Override
            public String getSearchStructureType() {
                return "MTree";
            }
        };

        return new TestLinker(factory, 2.0, metric,0);
    }
}
