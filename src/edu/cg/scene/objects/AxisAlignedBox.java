package edu.cg.scene.objects;

import edu.cg.algebra.*;


public class AxisAlignedBox extends Shape{
    private final static int NDIM = 3; // Number of dimensions
    private Point a = null;
    private Point b = null;
    private double[] aAsArray;
    private double[] bAsArray;

    public AxisAlignedBox(Point a, Point b){
        this.a = a;
        this.b = b;
        // We store the points as Arrays - this could be helpful for more elegant implementation.
        aAsArray = a.asArray();
        bAsArray = b.asArray();
        assert (a.x <= b.x && a.y<=b.y && a.z<=b.z);
    }

    @Override
    public String toString() {
        String endl = System.lineSeparator();
        return "AxisAlignedBox:" + endl +
                "a: " + a + endl +
                "b: " + b + endl;
    }

    public AxisAlignedBox initA(Point a){
        this.a = a;
        aAsArray = a.asArray();
        return this;
    }

    public AxisAlignedBox initB(Point b){
        this.b = b;
        bAsArray = b.asArray();
        return this;
    }

    private int findMinDim(final double[] vals) {
        double minVal = vals[0];
        int minDim = 0;
        for (int dim = 1; dim < NDIM; ++dim) {
            if (vals[dim] < minVal) {
                minDim = dim;
                minVal = vals[dim];
            }
        }
        return minDim;
    }
    
    private int findMaxDim(final double[] vals) {
        double minVal = vals[0];
        int minDim = 0;
        for (int dim = 1; dim < NDIM; ++dim) {
            if (vals[dim] > minVal) {
                minDim = dim;
                minVal = vals[dim];
            }
        }
        return minDim;
    }
    
    @Override
    public Hit intersect(final Ray ray) {
        double[][] intervals = new double[2][3];
        boolean[] negate = new boolean[3];
        Point p = ray.source();
        Vec v = ray.direction();
        for (int dim = 0; dim < NDIM; ++dim) {
            double vDim = v.getCoordinate(dim);
            double pDim = p.getCoordinate(dim);
            if (Math.abs(vDim) <= 0.00001) {
                if (pDim <= this.aAsArray[dim] || pDim >= this.bAsArray[dim]) 
                    return null;
                
                intervals[0][dim] = Double.NEGATIVE_INFINITY;
                intervals[1][dim] = Double.POSITIVE_INFINITY;
            }
            else {
                double t1 = (this.aAsArray[dim] - pDim) / vDim;
                double t2 = (this.bAsArray[dim] - pDim) / vDim;
                if (t1 <= 0.00001 || (t2 > 0.00001 && t2 < t1)) 
                    negate[dim] = true;                
                else 
                    negate[dim] = false;
                    
                intervals[0][dim] = Math.min(t1, t2);
                intervals[1][dim] = Math.max(t1, t2);
            }
        }
        int maxDim = this.findMaxDim(intervals[0]);
        int minDim = this.findMinDim(intervals[1]);
        double minInt = intervals[0][maxDim];
        double maxInt = intervals[1][minDim];
        
        if (minInt > maxInt || maxInt <= 0.00001) 
            return null;

        boolean isWithin;
        Vec normal;
        if (minInt > 0.00001) {
            isWithin = false;
            normal = this.getDimNormal(maxDim).neg();
            if (negate[maxDim])
                normal = normal.neg();
        }
        else {
            minInt = maxInt;
            isWithin = true;
            normal = this.getDimNormal(minDim);
            if (negate[minDim]) 
                normal = normal.neg();
        }
        return new Hit(minInt, normal).setIsWithin(isWithin);
    }
    
    private Vec getDimNormal(final int dim) {
        switch (dim) {
            case 0: {
                return new Vec(1.0, 0.0, 0.0);
            }
            case 1: {
                return new Vec(0.0, 1.0, 0.0);
            }
            case 2: {
                return new Vec(0.0, 0.0, 1.0);
            }
            default: {
                return null;
            }
        }
    }
}

