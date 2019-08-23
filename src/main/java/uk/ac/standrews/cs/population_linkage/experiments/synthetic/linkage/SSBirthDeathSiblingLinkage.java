package uk.ac.standrews.cs.population_linkage.experiments.synthetic.linkage;

import uk.ac.standrews.cs.population_linkage.experiments.characterisation.LinkStatus;
import uk.ac.standrews.cs.population_linkage.experiments.linkage.*;
import uk.ac.standrews.cs.population_records.RecordRepository;
import uk.ac.standrews.cs.population_records.record_types.Birth;
import uk.ac.standrews.cs.population_records.record_types.Death;
import uk.ac.standrews.cs.storr.impl.LXP;
import uk.ac.standrews.cs.storr.impl.exceptions.BucketException;
import uk.ac.standrews.cs.storr.impl.exceptions.PersistentObjectException;

import java.util.*;

import static uk.ac.standrews.cs.population_linkage.experiments.characterisation.LinkStatus.TRUE_MATCH;

public class SSBirthDeathSiblingLinkage extends Linkage {


    private final Iterable<LXP> birth_records;
    private final Iterable<LXP> death_records;

    public SSBirthDeathSiblingLinkage(String results_repository_name, String links_persistent_name, String ground_truth_persistent_name, String source_repository_name, RecordRepository record_repository) {
        super(results_repository_name, links_persistent_name, ground_truth_persistent_name, source_repository_name, record_repository);
        birth_records = Utilities.getBirthRecords(record_repository);
        death_records = Utilities.getDeathRecords(record_repository);
    }

    @Override
    public Iterable<LXP> getSourceRecords1() {
        return birth_records;
    }

    @Override
    public Iterable<LXP> getSourceRecords2() {
        return death_records;
    }

    @Override
    public LinkStatus isTrueMatch(LXP record1, LXP record2) {

        String childFatherID = record1.getString(Birth.FATHER_IDENTITY);
        String childMotherID = record1.getString(Birth.MOTHER_IDENTITY);

        String decFatherID = record2.getString(Death.FATHER_IDENTITY);
        String decMotherID = record2.getString(Death.MOTHER_IDENTITY);

        if(childFatherID.trim().equals("") || childMotherID.trim().equals("") || decFatherID.trim().equals("") || decMotherID.trim().equals(""))
            return LinkStatus.UNKNOWN;

        if(childFatherID.equals(decFatherID) && childMotherID.equals(decMotherID))
            return LinkStatus.TRUE_MATCH;

        return LinkStatus.NOT_TRUE_MATCH;
    }

    @Override
    public String getDatasetName() {
        return "synthetic-scotland";
    }

    @Override
    public String getLinkageType() {
        return "birth-death-sibling";
    }

    @Override
    public String getSourceType1() {
        return "births";
    }

    @Override
    public String getSourceType2() {
        return "deaths";
    }

    @Override
    public List<Integer> getLinkageFields1() {
        return Constants.SIBLING_BUNDLING_BIRTH_TO_DEATH_LINKAGE_FIELDS;
    }

    @Override
    public List<Integer> getLinkageFields2() {
        return Constants.SIBLING_BUNDLING_DEATH_TO_BIRTH_LINKAGE_FIELDS;
    }

    @Override
    public Role makeRole1(LXP lxp) throws PersistentObjectException {
        return new Role(lxp.getThisRef(), Birth.ROLE_BABY);
    }

    @Override
    public Role makeRole2(LXP lxp) throws PersistentObjectException {
        return new Role(lxp.getThisRef(), Death.ROLE_DECEASED);
    }

    @Override
    public Map<String, Link> getGroundTruthLinks() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void makeLinksPersistent(Iterable<Link> links) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void makeGroundTruthPersistent(Iterable<Link> links) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int numberOfGroundTruthTrueLinks() {

        int c = 0;

        for(LXP birth : record_repository.getBirths()) {
            for(LXP death : record_repository.getDeaths()) {
                if(isTrueMatch(birth, death).equals(TRUE_MATCH))
                    c++;
            }
        }

        return c;
    }

    @Override
    public LinkageQuality evaluateWithoutPersisting(int numberOfGroundTruthTrueLinks, Iterable<Link> links) {
        int tp = 0;
        int fp = 0;

        try {
            for (Link link : links) {
                try {

                    if (isTrueMatch(link.getRole1().getRecordId().getReferend(),
                                    link.getRole2().getRecordId().getReferend())
                            .equals(TRUE_MATCH)) {
                        tp++;
                    } else {
                        fp++;
                    }

                } catch (BucketException ignored) {
                }
            }
        } catch (NoSuchElementException ignored) {}

        // divisions by two as links are symetrical
        int fn = numberOfGroundTruthTrueLinks - tp;

        return new LinkageQuality(tp, fp, fn);
    }

    @Override
    public Iterable<LXP> getPreFilteredSourceRecords1() {
        HashSet<LXP> filteredBirthRecords = new HashSet<>();

        for(LXP record : birth_records) {

            String fathersForename = record.getString(Birth.FATHER_FORENAME).trim();
            String fathersSurname = record.getString(Birth.FATHER_SURNAME).trim();
            String mothersForename = record.getString(Birth.MOTHER_FORENAME).trim();
//                String mothersSurname = record.getString(Birth.MOTHER_SURNAME).trim();

            if(!(fathersForename.equals("") || fathersForename.equals("missing") ||
                    fathersSurname.equals("") || fathersSurname.equals("missing") ||
                    mothersForename.equals("") || mothersForename.equals("missing"))) {
//                        mothersSurname.equals("") || mothersSurname.equals("missing"))) {
                // no key info is missing - so we'll consider this record

                filteredBirthRecords.add(record);
            } // else reject record for linkage - not enough info
        }

        return filteredBirthRecords;
    }

    @Override
    public Iterable<LXP> getPreFilteredSourceRecords2() {
        HashSet<LXP> filteredDeathRecords = new HashSet<>();

        for(LXP record : death_records) {

            String fathersForename = record.getString(Death.FATHER_FORENAME).trim();
            String fathersSurname = record.getString(Death.FATHER_SURNAME).trim();
            String mothersForename = record.getString(Death.MOTHER_FORENAME).trim();
//                String mothersSurname = record.getString(Birth.MOTHER_SURNAME).trim();

            if(!(fathersForename.equals("") || fathersForename.equals("missing") ||
                    fathersSurname.equals("") || fathersSurname.equals("missing") ||
                    mothersForename.equals("") || mothersForename.equals("missing"))) {
//                        mothersSurname.equals("") || mothersSurname.equals("missing"))) {
                // no key info is missing - so we'll consider this record

                filteredDeathRecords.add(record);
            } // else reject record for linkage - not enough info
        }
        return filteredDeathRecords;
    }
}
