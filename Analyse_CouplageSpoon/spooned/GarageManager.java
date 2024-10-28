import java.util.ArrayList;
import java.util.List;
public class GarageManager {
    private List<Car> cars = new ArrayList<>();

    public void addCar(Car car) {
        cars.add(car);
    }

    public void startAllCars() {
        for (Car car : cars) {
            car.start();
        }
    }
}