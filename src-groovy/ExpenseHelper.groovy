import com.bonita.lr.model.ExpenseCurrency
import com.bonita.lr.model.ExpenseCurrencyDAO

public class ExpenseHelper {
	
	public static ExpenseCurrency retrieveCurrency(String currencyName, ExpenseCurrencyDAO dao) {
		def currency = dao.findByName(currencyName, 0, 100)
		if (currency.isEmpty()) {
			throw new IllegalArgumentException("The currency $currencyName doesn't exist.")
		}
		currency[0]
	}
	
}
