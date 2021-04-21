package edu.cg.scene.objects;

import edu.cg.UnimplementedMethodException;
import edu.cg.algebra.*;


// TODO Implement this class which represents an axis aligned box
public class AxisAlignedBox extends Shape{
    private final static int NDIM=3; // Number of dimensions
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
        for (int dim = 1; dim < 3; ++dim) {
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
        for (int dim = 1; dim < 3; ++dim) {
            if (vals[dim] > minVal) {
                minDim = dim;
                minVal = vals[dim];
            }
        }
        return minDim;
    }
    
    @Override
    public Hit intersect(final Ray ray) {
        final double[][] intervals = new double[2][3];
        final boolean[] shouldNegateNormal = new boolean[3];
        final Point p = ray.source();
        final Vec v = ray.direction();
        for (int dim = 0; dim < 3; ++dim) {
            final double vdim = v.getCoordinate(dim);
            final double pdim = p.getCoordinate(dim);
            if (Math.abs(vdim) <= 1.0E-5) {
                if (pdim <= this.aAsArray[dim] || pdim >= this.bAsArray[dim]) {
                    return null;
                }
                intervals[0][dim] = Double.NEGATIVE_INFINITY;
                intervals[1][dim] = Double.POSITIVE_INFINITY;
            }
            else {
                final double t1 = (this.aAsArray[dim] - pdim) / vdim;
                final double t2 = (this.bAsArray[dim] - pdim) / vdim;
                if (t1 <= 1.0E-5 || (t2 > 1.0E-5 && t2 < t1)) {
                    shouldNegateNormal[dim] = true;
                }
                else {
                    shouldNegateNormal[dim] = false;
                }
                intervals[0][dim] = Math.min(t1, t2);
                intervals[1][dim] = Math.max(t1, t2);
            }
        }
        final int dim2 = this.findMaxDim(intervals[0]);
        final int dim3 = this.findMinDim(intervals[1]);
        double minT = intervals[0][dim2];
        final double maxT = intervals[1][dim3];
        if (minT > maxT || maxT <= 1.0E-5) {
            return null;
        }
        boolean isWithin;
        Vec normal;
        if (minT > 1.0E-5) {
            isWithin = false;
            normal = this.getDimNormal(dim2).neg();
            if (shouldNegateNormal[dim2]) {
                normal = normal.neg();
            }
        }
        else {
            minT = maxT;
            isWithin = true;
            normal = this.getDimNormal(dim3);
            if (shouldNegateNormal[dim3]) {
                normal = normal.neg();
            }
        }
        return new Hit(minT, normal).setIsWithin(isWithin);
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

