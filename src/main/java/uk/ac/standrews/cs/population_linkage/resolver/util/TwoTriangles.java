/*
 * Copyright 2022 Systems Research Group, University of St Andrews:
 * <https://github.com/stacs-srg>
 */
package uk.ac.standrews.cs.population_linkage.resolver.util;

/**
 * Used to encode twp unmatched triangles with nodes t1,t2,t3 and s1,s2,s3
 * t1 and s1 are t1s1_distance apart, t2 and s2 are t2s2_distance apart
 * but t3 and s3 is not connected.
 * All the ids are storr ids of Nodes.
 */
public class TwoTriangles {
    public final long t1;
    public final long t2;
    public final long t3;
    public final long s1;
    public final long s2;
    public final long s3;


    public TwoTriangles(long t1, long t2, long t3, long s1, long s2, long s3 ) {
        this.t1 = t1;
        this.t2 = t2;
        this.t3 = t3;
        this.s1 = s1;
        this.s2 = s2;
        this.s3 = s3;
    }

    public String toString() {
        return "t1 = " + t1 + " t2 = " + t2 + " t3 = " + t3 + "\n" +
                "s1 = " + s1 + " s2 = " + s2 + " s3 = " + s3 + "\n";
    }
}
