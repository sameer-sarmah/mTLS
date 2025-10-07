package northwind.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Product(
    String ProductID,
    String ProductName,
    String CategoryID,
    String QuantityPerUnit,
    String UnitPrice
) {
    
    public double getUnitPriceAsDouble() {
        return Double.parseDouble(UnitPrice);
    }
}