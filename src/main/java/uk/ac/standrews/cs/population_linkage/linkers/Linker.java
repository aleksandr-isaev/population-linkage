package uk.ac.standrews.cs.population_linkage.linkers;

import uk.ac.standrews.cs.population_linkage.supportClasses.Link;
import uk.ac.standrews.cs.population_linkage.supportClasses.RecordPair;
import uk.ac.standrews.cs.storr.impl.LXP;
import uk.ac.standrews.cs.storr.impl.exceptions.PersistentObjectException;
import uk.ac.standrews.cs.storr.interfaces.IStoreReference;
import uk.ac.standrews.cs.utilities.PercentageProgressIndicator;
import uk.ac.standrews.cs.utilities.ProgressIndicator;
import uk.ac.standrews.cs.utilities.metrics.coreConcepts.Metric;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Function;

public abstract class Linker {

    private final Function<RecordPair, Boolean> isViableLink;
    protected double threshold;
    protected final Metric<LXP> distance_metric;
    private Iterable<LXP> records1;
    private Iterable<LXP> records2;

    private String link_type;
    private String provenance;
    private String role_type_1;
    private String role_type_2;


    protected final ProgressIndicator linkage_progress_indicator;

    public Linker(Metric<LXP> distance_metric, double threshold, int number_of_progress_updates,
                  String link_type, String provenance, String role_type_1, String role_type_2, Function<RecordPair, Boolean> isViableLink) {

        this.link_type = link_type;
        this.provenance = provenance;
        this.role_type_1 = role_type_1;
        this.role_type_2 = role_type_2;
        this.isViableLink = isViableLink;

        this.distance_metric = distance_metric;
        this.threshold = threshold;
        linkage_progress_indicator = new PercentageProgressIndicator(number_of_progress_updates);

    }

    public void addRecords(Iterable<LXP> records1, Iterable<LXP> records2) {

        this.records1 = records1;
        this.records2 = records2;
    }

    public void terminate() {}

    public Iterable<Link> getLinks() {

        final Iterator<RecordPair> matching_pairs = getMatchingRecordPairs(records1, records2).iterator();

        return new Iterable<Link>() {

            private Link next = null;

            @Override
            public Iterator<Link> iterator() {

                return new Iterator<Link>() {

                    @Override
                    public boolean hasNext() {

                        return matching_pairs.hasNext();
                    }

                    @Override
                    public Link next() {

                        getNextLink();
                        return next;
                    }
                };
            }

            private void getNextLink() {

                if (matching_pairs.hasNext()) {

                    RecordPair pair;
                    do {
                        pair = matching_pairs.next();
                    }
                    while ((pair.distance > threshold || !isViableLink.apply(pair)) && matching_pairs.hasNext());

                    if (pair.distance <= threshold && isViableLink.apply(pair)) {

                        try {
                            next = new Link(pair.record1, getRole_type_1(), pair.record2, getRole_type_2(), 1.0f,
                                    getLink_type(), getProvenance() + ", distance: " + pair.distance);
                        } catch (PersistentObjectException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    else throw new NoSuchElementException();
                }
                else throw new NoSuchElementException();
            }
        };
    }

    public void setThreshold(double threshold) {

        this.threshold = threshold;
    }

    public Metric<LXP> getMetric() {
        return distance_metric;
    }

    protected abstract Iterable<RecordPair> getMatchingRecordPairs(final Iterable<LXP> records1, final Iterable<LXP> records2);

    public String getLink_type() {
        return link_type;
    }

    public String getProvenance() {
        return provenance;
    }

    public String getRole_type_1() {
        return role_type_1;
    }

    public String getRole_type_2() {
        return role_type_2;
    }

    public IStoreReference getIdentifier1(LXP record) throws PersistentObjectException {
        return record.getThisRef();
    }

    public IStoreReference getIdentifier2(LXP record) throws PersistentObjectException {
        return record.getThisRef();
    }

    protected int count(final Iterable<LXP> records) {

        int i = 0;
        for (LXP ignored : records) {
            i++;
        }
        return i;
    }
}