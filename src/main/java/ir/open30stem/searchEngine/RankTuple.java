package ir.open30stem.searchEngine;

/**
 * Created by erfan on 11/11/14.
 */
class RankTuple {
    public  Integer x;
    public  Double y;

    RankTuple(Integer x, Double y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public String toString() {
        return "(" + x + "," + y + ")";
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (other == this) {
            return true;
        }
        if (!(other instanceof RankTuple)){
            return false;
        }
        RankTuple other_ = (RankTuple) other;
        return other_.x.equals(this.x) && other_.y.equals(this.y);
    }
}
