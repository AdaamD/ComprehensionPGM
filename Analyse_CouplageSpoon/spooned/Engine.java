public class Engine {
    private boolean running;

    public void start() {
        running = true;
        System.out.println("Engine started");
    }

    public boolean isRunning() {
        return running;
    }

    public void stop() {
        running = false;
        System.out.println("Engine stopped");
    }
}