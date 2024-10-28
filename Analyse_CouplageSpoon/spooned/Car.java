public class Car {
    private Engine engine;

    private Wheel[] wheels;

    public Car(Engine engine) {
        this.engine = engine;
        this.wheels = new Wheel[4];
        for (int i = 0; i < 4; i++) {
            this.wheels[i] = new Wheel();
        }
    }

    public void start() {
        engine.start();
        System.out.println("Car is starting");
    }

    public void drive() {
        if (engine.isRunning()) {
            for (Wheel wheel : wheels) {
                wheel.rotate();
            }
            System.out.println("Car is driving");
        }
    }
}