

public class Velocity {
    private double theDx; 
    private double theDy;

    public Velocity(double dx, double dy) {
        this.theDx = dx;
        this.theDy = dy;
    }

    public static Velocity AngVelocity(double angle, double speed) {
        double radians = Math.toRadians(angle);
        double dx = speed * Math.cos(radians);
        double dy = speed * Math.sin(radians);
        return new Velocity(dx, dy);
    }

    public double getDx() {
        return this.theDx;
    }

    public double getDy() {
        return this.theDy;
    }

    public void setDx(double dx) {
        this.theDx = dx;
    }

    public void setDy(double dy) {
        this.theDy = dy;
    }

    public Velocity add(Velocity other) {
        return new Velocity(this.theDx + other.theDx, this.theDy + other.theDy);
    }

    public Velocity scale(double factor) {
        return new Velocity(this.theDx * factor, this.theDy * factor);
    }
}
