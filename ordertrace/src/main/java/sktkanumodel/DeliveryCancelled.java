package sktkanumodel;

public class DeliveryCancelled extends AbstractEvent {

    private Long Id;
    private Long ProductId;
    private Integer qty;
    private String DeliveryStatus;
    private String Destination;
    private Long DeliveryId;
    private String ProductName;
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
    public String getDeliveryStatus() {
        return DeliveryStatus;
    }

    public void setDeliveryStatus(String DeliveryStatus) {
        this.DeliveryStatus = DeliveryStatus;
    }
    public String getDestination() {
        return Destination;
    }

    public void setDestination(String Destination) {
        this.Destination = Destination;
    }
    public Long getDeliveryId() {
        return DeliveryId;
    }

    public void setDeliveryId(Long DeliveryId) {
        this.DeliveryId = DeliveryId;
    }
    public String getProductName() {
        return ProductName;
    }

    public void setProductName(String ProductName) {
        this.ProductName = ProductName;
    }
    public Long getOrderId() {
        return OrderId;
    }

    public void setOrderId(Long OrderId) {
        this.OrderId = OrderId;
    }
}