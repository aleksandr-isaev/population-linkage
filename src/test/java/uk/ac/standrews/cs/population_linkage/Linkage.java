package uk.ac.standrews.cs.population_linkage;

import org.junit.Before;
import org.junit.Test;
import uk.ac.standrews.cs.population_linkage.model.Linker;
import uk.ac.standrews.cs.population_linkage.model.RecordPair;
import uk.ac.standrews.cs.storr.impl.LXP;
import uk.ac.standrews.cs.storr.impl.Metadata;
import uk.ac.standrews.cs.storr.impl.StaticLXP;

import java.util.Arrays;
import java.util.List;

import static junit.framework.TestCase.*;

public abstract class Linkage {

    final LXP birth1 = new DummyLXP("john", "smith");
    final LXP birth2 = new DummyLXP("janet", "smith");
    final LXP birth3 = new DummyLXP("jane", "smyth");

    final LXP death1 = new DummyLXP("janet", "smythe");
    final LXP death2 = new DummyLXP("john", "stith");

    final List<LXP> birth_records = Arrays.asList(birth1, birth2, birth3);
    final List<LXP> death_records = Arrays.asList(death1, death2);

    Linker linker;

    protected abstract Linker getLinker();

    protected abstract boolean equal(RecordPair pair1, RecordPair pair2);

    @Before
    public void init() {

        linker = getLinker();
    }

    @Test
    public void checkAllRecordPairsWithSingleDataSet() {

        linker.setThreshold(Double.MAX_VALUE);

        Iterable<RecordPair> pairs = linker.getMatchingRecordPairs(birth_records);

        // By default, assume links are asymmetric, so we want to consider record pair (a,b) as well as (b,a), but not (a,a).
        assertEquals((birth_records.size() - 1) * birth_records.size(), count(pairs));

        assertTrue(containsPair(pairs, birth1, birth2));
        assertTrue(containsPair(pairs, birth1, birth3));
        assertTrue(containsPair(pairs, birth2, birth3));
        assertTrue(containsPair(pairs, birth2, birth1));
        assertTrue(containsPair(pairs, birth3, birth1));
        assertTrue(containsPair(pairs, birth3, birth2));

        assertFalse(containsPair(pairs, birth1, birth1));
    }

    @Test
    public void checkAllRecordPairsWithTwoDataSets() {

        Iterable<RecordPair> pairs = linker.getMatchingRecordPairs(birth_records, death_records);

        assertEquals(birth_records.size() * death_records.size(), count(pairs));

        for (LXP birth_record : birth_records) {
            for (LXP death_record : death_records) {

                assertTrue(containsPair(pairs, birth_record, death_record));
            }
        }

        assertFalse(containsPair(pairs, birth1, birth1));
        assertFalse(containsPair(pairs, death1, death1));
    }

    @Test
    public void checkRecordPairsWithinDistanceZero() {

        linker.setThreshold(0.0);

        Iterable<RecordPair> pairs = linker.getMatchingRecordPairs(birth_records, death_records);

        assertEquals(0, count(pairs));
    }

    @Test
    public void checkRecordPairsWithinDistanceNoughtPointFive() {

        // "john smith" distance 0.5 from "john stith"

        linker.setThreshold(0.5);

        Iterable<RecordPair> pairs = linker.getMatchingRecordPairs(birth_records, death_records);

        assertEquals(1, count(pairs));
        assertTrue(containsPair(pairs, birth1, death2));
    }

    @Test
    public void checkRecordPairsWithinDistanceOne() {

        // "john smith" distance 0.5 from "john stith"
        // "janet smith" distance 1 from "janet smythe"
        // "jane smyth" distance 1 from "janet smythe"

        linker.setThreshold(1.0);

        Iterable<RecordPair> pairs = linker.getMatchingRecordPairs(birth_records, death_records);

        assertEquals(3, count(pairs));
        assertTrue(containsPair(pairs, birth1, death2));
        assertTrue(containsPair(pairs, birth2, death1));
        assertTrue(containsPair(pairs, birth3, death1));
    }

    boolean containsPair(Iterable<RecordPair> record_pairs, LXP record1, LXP record2) {

        RecordPair record_pair = new RecordPair(record1, record2, -1);

        for (RecordPair p : record_pairs) {
            if (equal(p, record_pair))
                return true;
        }
        return false;
    }

    int count(Iterable<RecordPair> record_pairs) {

        int count = 0;
        for (RecordPair ignored : record_pairs) count++;
        return count;
    }

    void printPairs(Iterable<RecordPair> recordPairs) {

        for (RecordPair pair : recordPairs) {
            System.out.println(pair);
        }
        System.out.println("----------");
    }

    class DummyLXP extends StaticLXP {

        String rep = "";

        DummyLXP(String... values) {

            int i = 0;
            for (String value : values) {
                put(i++, value);
                rep += value + " ";
            }
        }

        @Override
        public Metadata getMetaData() {
            return null;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof DummyLXP && ((DummyLXP) o).id == id;
        }

        public String toString() {
            return rep;
        }
    }
}
