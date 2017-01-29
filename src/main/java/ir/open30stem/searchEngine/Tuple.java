package ir.open30stem.searchEngine;


/**
 * Created by erfan on 9/25/14.
 */
class Tuple<X, Y, Z> {
    public  X x;
    public  Y y;
    public  Z z;
    Tuple(X x, Y y, Z z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public String toString() {
        return "(" + x + "," + y + "," + z + ")";
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (other == this) {
            return true;
        }
        if (!(other instanceof Tuple)){
            return false;
        }
        Tuple<X,Y,Z> other_ = (Tuple<X,Y,Z>) other;
        return other_.x == this.x && other_.y == this.y && other_.z == this.z;
    }
}