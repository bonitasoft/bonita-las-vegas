import com.bonita.lr.model.ExpenseVehicleDAO

public class AmountComputer {

    public static float computeAmount(String vehicle, int km, ExpenseVehicleDAO dao) {
        def expeneVehicle = dao.findByName(vehicle, 0, 100).get(0);
        return expeneVehicle.rate * km;
    }
}