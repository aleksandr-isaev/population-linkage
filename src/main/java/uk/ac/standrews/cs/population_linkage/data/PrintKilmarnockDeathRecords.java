package uk.ac.standrews.cs.population_linkage.data;

import uk.ac.standrews.cs.data.kilmarnock.KilmarnockDeathsDataSet;

public class PrintKilmarnockDeathRecords {

    public void run() throws Exception {

        KilmarnockDeathsDataSet death_records = new KilmarnockDeathsDataSet();
        death_records.print(System.out);
        System.out.println("Printed " + death_records.getRecords().size() + " death records");
    }

    public static void main(String[] args) throws Exception {

        new PrintKilmarnockDeathRecords().run();
    }
}