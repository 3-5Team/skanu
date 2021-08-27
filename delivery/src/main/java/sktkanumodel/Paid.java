package sktkanumodel;

public class Paid extends AbstractEvent {

    private Long Id;
    private Long OrderId;
    private Long ProductId;
    private Integer qty;
    private String PaymentStatus;
    private Long PaymentId;
    private String PaymentType;
    private Long Cost;
    private String ProductName;

    public Long getId() {
        return Id;
    }

    public void setId(Long Id) {
        this.Id = Id;
    }
    public Long getOrderId() {
        return OrderId;
    }

    public void setOrderId(Long OrderId) {
        this.OrderId = OrderId;
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
    public String getPaymentStatus() {
        return PaymentStatus;
    }

    public void setPaymentStatus(String PaymentStatus) {
        this.PaymentStatus = PaymentStatus;
    }
    public Long getPaymentIId() {
        return PaymentId;
    }

    public void setPaymentIId(Long PaymentIId) {
        this.PaymentId = PaymentIId;
    }
    public String getPaymentType() {
        return PaymentType;
    }

    public void setPaymentType(String PaymentType) {
        this.PaymentType = PaymentType;
    }
    public Long getCost() {
        return Cost;
    }

    public void setCost(Long Cost) {
        this.Cost = Cost;
    }
    public String getProductName() {
        return ProductName;
    }

    public void setProductName(String ProductName) {
        this.ProductName = ProductName;
    }
}