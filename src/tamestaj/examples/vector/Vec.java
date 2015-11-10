package tamestaj.examples.vector;

import tamestaj.annotations.Stage;

import java.util.Arrays;

public final class Vec {
    final double[] elements;
    Vec(double[] elements) {
        this.elements = elements;
    }

    public static Vec create(double... elements) {
        return new Vec(Arrays.copyOf(elements, elements.length));
    }

    @Stage(language = VecL.class)
    public Vec plus(Vec vec) {
        double[] r = new double[elements.length];
        for (int i = 0; i < r.length; i++) {
            r[i] = elements[i] + vec.elements[i];
        }
        return new Vec(r);
    }

    @Stage(language = VecL.class)
    public Vec times(double s) {
        double[] r = new double[elements.length];
        for (int i = 0; i < r.length; i++) {
            r[i] = elements[i] * s;
        }
        return new Vec(r);
    }

    @Stage(language = VecL.class)
    public VecE toVecE() {
        return new VecE.Lit(this);
    }

    @Override
    public String toString() {
        return Arrays.toString(elements);
    }
}
