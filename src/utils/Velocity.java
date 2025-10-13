package utils;

public class Velocity {
    private double dx; 
    private double dy;

    public Velocity(double dx, double dy) {
        this.dx = dx;
        this.dy = dy;
    }

    public static Velocity fromAngle(double angle, double speed) {
        double radians = Math.toRadians(angle);
        double dx = speed * Math.cos(radians);
        double dy = speed * Math.sin(radians);
        return new Velocity(dx, dy);
    }

    public double getDx() {
        return this.dx;
    }

    public double getDy() {
        return this.dy;
    }


    public void setDx(double dx) {
        this.dx = dx;
    }

    public void setDy(double dy) {
        this.dy = dy;
    }

    public Velocity add(Velocity other) {
        return new Velocity(this.dx + other.dx, this.dy + other.dy);
    }


    public Velocity scale(double factor) {
        return new Velocity(this.dx * factor, this.dy * factor);
    }
    
    public double getMagnitude() {
        return Math.sqrt(dx * dx + dy * dy);
    }
    

    public Velocity normalize() {
        double magnitude = getMagnitude();
        if (magnitude == 0) return new Velocity(0, 0);
        return new Velocity(dx / magnitude, dy / magnitude);
    }
}