package sktkanumodel;

public class PayCancelled extends AbstractEvent {

    private Long Id;
    private Long ProductId;
    private Integer qty;
    private String PaymentStatus;
    private Long PaymentId;
    private String PaymentType;
    private Long Cost;
    private Long OrderId;

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
    public String getPaymentStatus() {
        return PaymentStatus;
    }

    public void setPaymentStatus(String PaymentStatus) {
        this.PaymentStatus = PaymentStatus;
    }
    public Long getPaymentId() {
        return PaymentId;
    }

    public void setPaymentId(Long PaymentId) {
        this.PaymentId = PaymentId;
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
    public Long getOrderId() {
        return OrderId;
    }

    public void setOrderId(Long OrderId) {
        this.OrderId = OrderId;
    }
}