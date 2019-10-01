package uk.ac.standrews.cs.population_linkage.linkers;

import uk.ac.standrews.cs.population_linkage.supportClasses.RecordPair;
import uk.ac.standrews.cs.population_linkage.searchStructures.SearchStructure;
import uk.ac.standrews.cs.population_linkage.searchStructures.SearchStructureFactory;
import uk.ac.standrews.cs.storr.impl.LXP;
import uk.ac.standrews.cs.utilities.ProgressIndicator;
import uk.ac.standrews.cs.utilities.metrics.coreConcepts.DataDistance;
import uk.ac.standrews.cs.utilities.metrics.coreConcepts.Metric;

import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

public class SimilaritySearchLinker extends Linker {

    private SearchStructureFactory<LXP> search_structure_factory;
    private SearchStructure<LXP> search_structure;
    private Iterable<LXP> smaller_set;
    private int smaller_set_size;
    private boolean recordOrderFlipped = false;

    public SimilaritySearchLinker(SearchStructureFactory<LXP> search_structure_factory, Metric<LXP> distance_metric, double threshold, int number_of_progress_updates,
                                     String link_type, String provenace, String role_type_1, String role_type_2, Function<RecordPair, Boolean> isViableLink) {

        super(distance_metric, threshold, number_of_progress_updates, link_type, provenace, role_type_1, role_type_2, isViableLink);

        this.search_structure_factory = search_structure_factory;
    }

    public void addRecords(Iterable<LXP> records1, Iterable<LXP> records2) {

        super.addRecords(records1, records2);

        int records1_size = count(records1);
        int records2_size = records1 == records2 ? records1_size : count(records2);

        Iterable<LXP> larger_set;

        if (records1_size < records2_size) {
            smaller_set = records1;
            smaller_set_size = records1_size;
            larger_set = records2;
            recordOrderFlipped = false;
        } else {
            smaller_set = records2;
            smaller_set_size = records2_size;
            larger_set = records1;
            recordOrderFlipped = true;
        }

        search_structure = search_structure_factory.newSearchStructure(larger_set);
    }

    public void terminate() {
        search_structure.terminate();
    }

    @Override
    public Iterable<RecordPair> getMatchingRecordPairs(final Iterable<LXP> records1, final Iterable<LXP> records2) {

        return new Iterable<RecordPair>() {

            class RecordPairIterator extends AbstractRecordPairIterator {

                private int neighbours_index;
                private List<DataDistance<LXP>> nearest_records;
                private LXP next_record_from_smaller_set;
                private Iterator<LXP> smaller_set_iterator;

                RecordPairIterator(final Iterable<LXP> records1, final Iterable<LXP> records2, ProgressIndicator progress_indicator) {

                    super(records1, records2, progress_indicator);

                    smaller_set_iterator = smaller_set.iterator();
                    next_record_from_smaller_set = smaller_set_iterator.next();

                    neighbours_index = 0;

                    progress_indicator.setTotalSteps(smaller_set_size);

                    getNextRecordBatch();
                    getNextPair();
                }

                @Override
                boolean match(final RecordPair pair) {
                    return true;
                }

                void getNextPair() {

                    while (smaller_set_iterator.hasNext() && !moreLinksAvailableFromCurrentRecordFromSmallerSet()) {
                        getNextRecordFromSmallerSet();
                    }

                    loadPair();

                    if (pairShouldBeSkipped()) {
                        next_pair = null;
                    }
                }

                private void loadPair() {

                    do {
                        if (moreLinksAvailable()) {

                            DataDistance<LXP> data_distance = nearest_records.get(neighbours_index++);
                            // this can potentionally flip the records round what what was specified - okay for symetric linkage but messes up asymetric linkage...
                            if(recordOrderFlipped)
                                next_pair = new RecordPair(data_distance.value, next_record_from_smaller_set, data_distance.distance);
                            else
                                next_pair = new RecordPair(next_record_from_smaller_set, data_distance.value, data_distance.distance);

                            if (!moreLinksAvailableFromCurrentRecordFromSmallerSet()) getNextRecordFromSmallerSet();

                        } else {
                            next_pair = null;
                        }
                    }
                    while (moreLinksAvailable() && pairShouldBeSkipped());
                }

                private boolean moreLinksAvailable() {

                    return smaller_set_iterator.hasNext() || moreLinksAvailableFromCurrentRecordFromSmallerSet();
                }

                private boolean moreLinksAvailableFromCurrentRecordFromSmallerSet() {

                    return neighbours_index < nearest_records.size();
                }

                private boolean pairShouldBeSkipped() {

                    return next_pair == null || (datasets_same && next_pair.record1.getId() == next_pair.record2.getId());
                }

                private void getNextRecordFromSmallerSet() {

                    if (smaller_set_iterator.hasNext()) {

                        progress_indicator.progressStep();
                        next_record_from_smaller_set = smaller_set_iterator.next();
                        neighbours_index = 0;

                        getNextRecordBatch();
                    }
                }

                private void getNextRecordBatch() {

                    nearest_records = search_structure.findWithinThreshold(next_record_from_smaller_set, threshold);
                }
            }

            @Override
            public Iterator<RecordPair> iterator() {
                return new RecordPairIterator(records1, records2, linkage_progress_indicator);
            }
        };
    }
}