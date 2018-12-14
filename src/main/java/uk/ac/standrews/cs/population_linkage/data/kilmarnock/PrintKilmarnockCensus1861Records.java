package uk.ac.standrews.cs.population_linkage.data.kilmarnock;

import uk.ac.standrews.cs.data.kilmarnock.KilmarnockCensus1861DataSet;
import uk.ac.standrews.cs.utilities.dataset.DataSet;

public class PrintKilmarnockCensus1861Records {

    public void run() throws Exception {

        DataSet census_records = new KilmarnockCensus1861DataSet();
        census_records.print(System.out);
        System.out.println("Printed " + census_records.getRecords().size() + " census records");
    }

    public static void main(String[] args) throws Exception {

        new PrintKilmarnockCensus1861Records().run();
    }
}