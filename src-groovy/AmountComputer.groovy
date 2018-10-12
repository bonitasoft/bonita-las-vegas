import com.bonita.lr.model.ExpenseVehicle
import com.bonita.lr.model.ExpenseVehicleDAO

public class AmountComputer {

    public static float computeAmount(String vehicle, int km, ExpenseVehicleDAO dao) {
        List<ExpenseVehicle> vehicles = dao.findByName(vehicle, 0, 100)
        if (vehicles.isEmpty()) {
            throw new IllegalArgumentException(String.format("The vehicle %s doesn't exist", vehicle))
        }
        return vehicles.get(0).rate * km;
    }
}