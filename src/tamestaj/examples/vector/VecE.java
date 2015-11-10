package tamestaj.examples.vector;

import tamestaj.GlobalCarrier;
import tamestaj.annotations.Stage;
import tamestaj.annotations.Suppress;

import java.util.Arrays;

public abstract class VecE extends GlobalCarrier {
    VecE() { }

    static final class PlusN extends VecE {
        final VecE[] vecEs;

        PlusN(VecE... vecEs) {
            this.vecEs = vecEs;
        }

        @Override
        public VecE plus(VecE vecE) {
            if (vecE instanceof Plus) {
                Plus p = (Plus) vecE;
                VecE[] vecEs = Arrays.copyOf(this.vecEs, this.vecEs.length + 2);
                vecEs[vecEs.length - 2] = p.left;
                vecEs[vecEs.length - 1] = p.right;
                return new PlusN(vecEs);
            } else if (vecE instanceof PlusN) {
                PlusN p = (PlusN) vecE;
                VecE[] vecEs = Arrays.copyOf(this.vecEs, this.vecEs.length + p.vecEs.length);
                System.arraycopy(p.vecEs, 0, vecEs, this.vecEs.length, p.vecEs.length);
                return new PlusN(vecEs);
            } else {
                VecE[] vecEs = Arrays.copyOf(this.vecEs, this.vecEs.length + 1);
                vecEs[vecEs.length - 1] = vecE;
                return new PlusN(vecEs);
            }
        }

        @Override
        public VecE plus(Vec vec) {
            VecE[] vecEs = Arrays.copyOf(this.vecEs, this.vecEs.length + 1);
            vecEs[vecEs.length - 1] = vec.toVecE();

            return new PlusN(vecEs);
        }

        @Override
        @Suppress(languages = VecL.class)
        public Vec toVec() {
            Vec temp = vecEs[0].toVec();
            double[] r = Arrays.copyOf(temp.elements, temp.elements.length);
            for (int i = 1; i < vecEs.length; i++) {
                temp = vecEs[i].toVec();
                for (int j = 0; j < r.length; j++) {
                    r[j] += temp.elements[j];
                }
            }
            return new Vec(r);
        }
    }

    static final class Plus extends VecE {
        final VecE left;
        final VecE right;

        Plus(VecE left, VecE right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public VecE plus(VecE vecE) {
            if (vecE instanceof Plus) {
                Plus p = (Plus) vecE;
                return new PlusN(left, right, p.left, p.right);
            } else if (vecE instanceof PlusN) {
                PlusN p = (PlusN) vecE;
                VecE[] vecEs = new VecE[p.vecEs.length + 2];
                vecEs[0] = left;
                vecEs[1] = right;
                System.arraycopy(p.vecEs, 0, vecEs, 2, p.vecEs.length);
                return new PlusN(vecEs);
            } else {
                return new PlusN(left, right, vecE);
            }
        }

        @Override
        @Suppress(languages = VecL.class)
        public VecE plus(Vec vec) { return new PlusN(left, right, vec.toVecE()); }

        @Override
        @Suppress(languages = VecL.class)
        public Vec toVec() {
            return left.toVec().plus(right.toVec());
        }
    }

    static final class Times extends VecE {
        final VecE vec;
        final double s;

        Times(VecE vec, double s) {
            this.vec = vec;
            this.s = s;
        }

        @Override
        public VecE times(double s) {
            return new Times(vec, this.s * s);
        }

        @Override
        @Suppress(languages = VecL.class)
        public Vec toVec() {
            return vec.toVec().times(s);
        }
    }

    static final class Lit extends VecE {
        final Vec vec;

        Lit(Vec vec) {
            this.vec = vec;
        }

        @Override
        @Suppress(languages = VecL.class)
        public Vec toVec() {
            return vec;
        }
    }

    @Stage(language = VecL.class)
    public VecE plus(VecE vecE) {
        return new Plus(this, vecE);
    }

    @Stage(language = VecL.class)
    @Suppress(languages = VecL.class)
    public VecE plus(Vec vec) { return new Plus(this, vec.toVecE()); }

    @Stage(language = VecL.class)
    public VecE times(double s) {
        return new Times(this, s);
    }

    @Stage(language = VecL.class)
    public abstract Vec toVec();

    @Stage(language = VecL.class)
    public Vec eval() {
        return toVec();
    }
}
