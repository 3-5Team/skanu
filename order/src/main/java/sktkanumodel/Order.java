package sktkanumodel;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
//import java.util.List;
//import java.util.Date;

@Entity
@Table(name="Order_table")
public class Order {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private Long productId;
    private Integer qty;
    private String paymentType;
    private Long cost;
    private String productName;

    @PostPersist
    public void onPostPersist(){
        Ordered ordered = new Ordered();
        BeanUtils.copyProperties(this, ordered);
        ordered.setOrderStatus("Order complete");

        sktkanumodel.external.Payment payment = new sktkanumodel.external.Payment();
        // mappings goes here
        payment.setOrderId(this.getId());
        payment.setProductId(this.getProductId());
        payment.setProductName(this.getProductName());
        payment.setPaymentStatus("Not Pay");
        payment.setQty(this.getQty());
        payment.setPaymentType(this.getPaymentType());
        payment.setCost(this.getCost());
        OrderApplication.applicationContext.getBean(sktkanumodel.external.PaymentService.class)
            .paid(payment);

        ordered.publishAfterCommit();
    }

    @PostRemove
    public void onPostRemove(){
        OrderCancelled orderCancelled = new OrderCancelled();
        BeanUtils.copyProperties(this, orderCancelled);
        orderCancelled.setOrderStatus("Order Cancelled");
        orderCancelled.publishAfterCommit();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }
    public Integer getQty() {
        return qty;
    }

    public void setQty(Integer qty) {
        this.qty = qty;
    }
    public String getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }
    public Long getCost() {
        return cost;
    }

    public void setCost(Long cost) {
        this.cost = cost;
    }
    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }




}