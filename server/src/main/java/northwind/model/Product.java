package northwind.model;

public class Product {

	private String ProductID;
	private String ProductName;
	private String CategoryID;
	private String QuantityPerUnit;
	private String UnitPrice;

	public Product(String productID, String productName, String categoryID, String quantityPerUnit, String unitPrice) {
		super();
		this.ProductID = productID;
		this.ProductName = productName;
		this.CategoryID = categoryID;
		this.QuantityPerUnit = quantityPerUnit;
		this.UnitPrice = unitPrice;
	}

	
	
	public Product() {
		super();
	}



	public String getProductID() {
		return ProductID;
	}

	public String getProductName() {
		return ProductName;
	}

	public String getCategoryID() {
		return CategoryID;
	}

	public String getQuantityPerUnit() {
		return QuantityPerUnit;
	}

	public String getUnitPrice() {
		return UnitPrice;
	}
	
	public double getUnitPriceAsDouble() {
		return Double.parseDouble(UnitPrice);
	}
	
	
}
