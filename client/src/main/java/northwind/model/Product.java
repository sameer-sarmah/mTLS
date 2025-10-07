package northwind.model;

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