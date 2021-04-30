package edu.cg.scene.objects;

import edu.cg.algebra.*;

import java.util.HashSet;
import java.util.Set;


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

    private Plain[] getFaces() {
        Plain[] faces = new Plain[6];
        Vec[] unitVectors  = new Vec[] {
            new Vec(1, 0 ,0), new Vec(0, 1, 0), new Vec(0 ,0 ,1)
        };
        int idx = 0;
        for (Vec normal : unitVectors) {
            faces[idx++] = new Plain(normal, a);
            faces[idx++] = new Plain(normal, b);
        }
        return faces;
    }

    private boolean isInBox(Ray ray, Hit hit) {
        Point point = ray.getHittingPoint(hit);
        double x = point.x ;
        double y = point.y ;
        double z = point.z ;
        boolean isInX = a.x <= x + Ops.epsilon && b.x >= x - Ops.epsilon;
        boolean isInY = a.y <= y + Ops.epsilon && b.y >= y - Ops.epsilon;
        boolean isInZ = a.z <= z + Ops.epsilon && b.z >= z - Ops.epsilon;
        return isInX && isInY && isInZ;
    }

    @Override
    public Hit intersect(Ray ray) {
        Plain[] faces = getFaces();
        Hit min = null;
        for (Plain face : faces) {
            Hit hit = face.intersect(ray);
            if (hit != null && isInBox(ray, hit)) {
                if (min == null) {
                    min = hit;
                } else if (hit.compareTo(min) < 0) {
                    min = hit;
                }
            }
        }
        return min;
    }
}