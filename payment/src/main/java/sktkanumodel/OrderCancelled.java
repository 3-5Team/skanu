package sktkanumodel;

public class OrderCancelled extends AbstractEvent {

    private Long Id;
    private Long ProductId;
    private Integer qty;
    private String OrderStatus;
    private String ProductName;
    private Long Cost;
    private String PaymentType;

    public Long getId() {
        return Id;
    }

    public void setId(Long Id) {
        this.Id = Id;
    }
    public Long getProductId() {
        return ProductId;
    }

    public void setProductId(Long ProductId) {
        this.ProductId = ProductId;
    }
    public Integer getQty() {
        return qty;
    }

    public void setQty(Integer qty) {
        this.qty = qty;
    }
    public String getOrderStatus() {
        return OrderStatus;
    }

    public void setOrderStatus(String OrderStatus) {
        this.OrderStatus = OrderStatus;
    }
    public String getProductName() {
        return ProductName;
    }

    public void setProductName(String ProductName) {
        this.ProductName = ProductName;
    }
    public Long getCost() {
        return Cost;
    }

    public void setCost(Long Cost) {
        this.Cost = Cost;
    }
    public String getPaymentType() {
        return PaymentType;
    }

    public void setPaymentType(String PaymentType) {
        this.PaymentType = PaymentType;
    }
}