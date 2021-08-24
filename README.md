# sktkanumodel
 
 # 서비스 시나리오

기능적 요구사항
1. 고객이 커피(음료)를 주문(Order)한다.
2. 고객이 지불(Pay)하고, 포인트(Point)가 적립된다.
3. 결제모듈(payment)에 결제를 진행하게 되고 '지불'처리 된다.
4. 결제 '승인' 처리가 되면 주방에서 음료를 제조한다.
5. 고객과 매니저는 마이페이지를 통해 진행상태(OrderTrace)를 확인할 수 있다.
6. 음료가 준비되면 배달(Delivery)을 한다.
7. 고객이 취소(Cancel)하는 경우 지불 및 제조, 배달이 취소가 된다.

비기능적 요구사항
1. 트랜잭션
    1. 결제가 되지 않은 주문건은 등록이 성립되지 않는다. - Sync 호출
2. 장애격리
    1. 지불이 수행되지 않더라도 주문과 결제는 365일 24시간 받을 수 있어야 한다  - Async(event-driven), Eventual Consistency
    2. 결제 시스템이 과중되면 주문(Order)을 잠시 후 처리하도록 유도한다  - Circuit breaker, fallback
3. 성능
    1. 마이페이지에서 주문상태(OrderTrace) 확인  - CQRS

# 분석/설계


## Event Storming 결과
* MSAEz 로 모델링한 이벤트스토밍 결과: http://labs.msaez.io/#/storming/P3HDhaDCvERl1kR9ZDeRJxSKcBj1/9de0090e507d50647baadc0be4472c77

### 이벤트 도출
![image](https://user-images.githubusercontent.com/79756040/129881425-3b9d3209-16b3-4d8a-a565-c82a85056980.png)

### 부적격 이벤트 탈락
![image](https://user-images.githubusercontent.com/79756040/129881872-bfa9ddb8-1e01-4885-b688-8a68d9770db4.png)

### 완성된 1차 모형
![image](https://user-images.githubusercontent.com/79756040/129881929-c6d1f38e-4115-4b5c-b650-4573852f9dd6.png)

### 완성된 최종 모형 ( 시나리오 점검 후 )
![image](https://user-images.githubusercontent.com/79756040/130600252-5362017c-ef4e-44e3-8786-44394eafc117.png)

## 헥사고날 아키텍처 다이어그램 도출 
![image](https://user-images.githubusercontent.com/20077391/121859335-88ac8a80-cd32-11eb-9159-9599abcf67cf.png)
 
 # 구현
 
 분석/설계 단계에서 도출된 헥사고날 아키텍처에 따라, 각 BC별로 대변되는 마이크로 서비스들을 스프링부트로 구현하였다. 구현한 각 서비스를 로컬에서 실행하는 방법은 아래와 같다 (각자의 포트넘버는 8081 ~ 8084, 8088 이다)

```
   cd order
   mvn spring-boot:run

   cd payment
   mvn spring-boot:run

   cd delivery
   mvn spring-boot:run

   cd ordertrace
   mvn spring-boot:run

   cd gateway
   mvn spring-boot:run
  ```
  
# DDD 의 적용

- msaez.io 를 통해 구현한 Aggregate 단위로 Entity 를 선언 후, 구현을 진행하였다.
 Entity Pattern 과 Repository Pattern 을 적용하기 위해 Spring Data REST 의 RestRepository 를 적용하였다.

 ### Order 서비스의 Order.java 

```java
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

```

### Payment 시스템의 PolicyHandler.java

```java
package sktkanumodel;

import sktkanumodel.config.kafka.KafkaProcessor;
// import com.fasterxml.jackson.databind.DeserializationFeature;
// import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PolicyHandler{
    @Autowired PaymentRepository paymentRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverOrderCancelled_PayCancel(@Payload OrderCancelled orderCancelled)
    {
        if(orderCancelled.validate())
        {
            System.out.println("\n\n##### listener PayCancel : " + orderCancelled.toJson() + "\n\n");
            List<Payment> paymanetsList = paymentRepository.findByOrderId(orderCancelled.getId());
            if(paymanetsList.size()>0) {
                for(Payment payment : paymanetsList) {
                    if(payment.getOrderId().equals(orderCancelled.getId())){
                        System.out.println("##### OrderId :: "+ payment.getId() 
                                                      +" ... "+ payment.getProductName()+" is Cancelled");
                        payment.setPaymentType("ORDER CANCEL");
                        paymentRepository.save(payment);
                    }
                }
            }
        }
    }

    
    @StreamListener(KafkaProcessor.INPUT)
    public void onStringEventListener(@Payload String eventString){}
}

```

적용 후 REST API 테스트를 통해 정상 동작 확인할 수 있다.
- 주문(Ordered) 수행의 결과

![image](https://user-images.githubusercontent.com/86760678/130349926-eff16870-1b96-465b-af2c-399eabbabd01.png)
![image](https://user-images.githubusercontent.com/86760678/130349952-376385d7-6ef2-42e6-ae80-6b0dd4d53d79.png)

# Gateway 적용
API Gateway를 통하여 마이크로 서비스들의 진입점을 통일하였다.

```yaml
server:
  port: 8088

---

spring:
  profiles: default
  cloud:
    gateway:
      routes:
        - id: order
          uri: http://localhost:8081
          predicates:
            - Path=/orders/** 
        - id: payment
          uri: http://localhost:8082
          predicates:
            - Path=/payments/** 
        - id: delivery
          uri: http://localhost:8083
          predicates:
            - Path=/deliveries/** 
        - id: ordertrace
          uri: http://localhost:8084
          predicates:
            - Path= /orderTraces/**
      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOrigins:
              - "*"
            allowedMethods:
              - "*"
            allowedHeaders:
              - "*"
            allowCredentials: true


---

spring:
  profiles: docker
  cloud:
    gateway:
      routes:
        - id: order
          uri: http://order:8080
          predicates:
            - Path=/orders/** 
        - id: payment
          uri: http://payment:8080
          predicates:
            - Path=/payments/** 
        - id: delivery
          uri: http://delivery:8080
          predicates:
            - Path=/deliveries/** 
        - id: ordertrace
          uri: http://ordertrace:8080
          predicates:
            - Path= /orderTraces/**
      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOrigins:
              - "*"
            allowedMethods:
              - "*"
            allowedHeaders:
              - "*"
            allowCredentials: true

server:
  port: 8080

```

# 폴리그랏 퍼시스턴스
- delivery 서비스의 경우, 다른 마이크로 서비스와 달리 hsql을 구현하였다.
- 이를 통해 서비스 간 다른 종류의 데이터베이스를 사용하여도 문제 없이 동작하여 폴리그랏 퍼시스턴스를 충족함.
### delivery 서비스의 pom.xml
![image](https://user-images.githubusercontent.com/86760678/130350197-5d6071e2-1fb4-42fc-95ca-c44e21619ed5.png)

#동기식 호출(Req/Res 방식)과 Fallback 처리
- order 서비스의 external/PaymentService.java 내에 결제(paid) 서비스를 호출하기 위하여 FeignClient를 이용하여 Service 대행 인터페이스(Proxy)를 구현

### order/external/PaymentService.java
```java
package sktkanumodel.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
// import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

// import java.util.Date;

@FeignClient(name="payment", url="http://localhost:8082")
public interface PaymentService {
    @RequestMapping(method= RequestMethod.POST, path="/payments")
    public void paid(@RequestBody Payment payment);

}

```

### Order 서비스의 Order.java
```java
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

### 이하 생략
```

- payment서비스를 내림

![image](https://user-images.githubusercontent.com/86760678/130350522-9f175e77-47f6-43c9-84bb-2c08fadd8525.png)

- 주문(order) 요청 및 에러 난 화면 표시

![image](https://user-images.githubusercontent.com/86760678/130350564-86e59bda-2b77-44ee-b872-b5939634ba8b.png)

- payment 서비스 재기동 후 다시 주문 요청

![image](https://user-images.githubusercontent.com/86760678/130350697-2ca7b817-36d6-4f33-80b7-be19a1f4ce7a.png)

- payment 서비스에 주문 대기 상태로 저장 확인

![image](https://user-images.githubusercontent.com/86760678/130350742-87a88566-aad3-41e8-a72e-96cce39fa9c5.png)


# 비동기식 호출(Pub/Sub 방식)
- order 서버스 내 Order.java에서 아래와 같이 서비스 Pub 구현

```java

///

@Entity
@Table(name="Order_table")
public class Order {

///
    @PostRemove
    public void onPostRemove(){
        OrderCancelled orderCancelled = new OrderCancelled();
        BeanUtils.copyProperties(this, orderCancelled);
        orderCancelled.setOrderStatus("Order Cancelled");
        orderCancelled.publishAfterCommit();
    }
///

```

- payment 서비스 내 PolicyHandler.java에서 아래와 같이 Sub 구현

```java
///

@Service
public class PolicyHandler{
    @Autowired PaymentRepository paymentRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverOrderCancelled_PayCancel(@Payload OrderCancelled orderCancelled)
    {
        if(orderCancelled.validate())
        {
            System.out.println("\n\n##### listener PayCancel : " + orderCancelled.toJson() + "\n\n");
            List<Payment> paymanetsList = paymentRepository.findByOrderId(orderCancelled.getId());
            if(paymanetsList.size()>0) {
                for(Payment payment : paymanetsList) {
                    if(payment.getOrderId().equals(orderCancelled.getId())){
                        System.out.println("##### OrderId :: "+ payment.getId() 
                                                      +" ... "+ payment.getProductName()+" is Cancelled");
                        payment.setPaymentType("ORDER CANCEL");
                        paymentRepository.save(payment);
                    }
                }
            }
        }
    }
///
}

```

- 비동기식 호출은 다른 서비스가 비정상이여도 이상없이 동작가능하여, payment 서비스에 장애가 나도 order 서비스는 정상 동작을 확인

### payment 서비스 내림

![image](https://user-images.githubusercontent.com/86760678/130352224-7d22d74d-4ebf-4ac5-91b0-b36092219ca4.png)

### 주문 취소

![image](https://user-images.githubusercontent.com/86760678/130352256-ff8aa934-f49c-4f38-a3a8-3774c05fc956.png)



# CQRS

viewer 인 ordertraces 서비스를 별도로 구현하여 아래와 같이 view가 출력된다.

### 주문 수행 후, ordertraces

![image](https://user-images.githubusercontent.com/86760678/130352429-83e1a1d3-e263-47d7-9760-becfccf9cc96.png)

![image](https://user-images.githubusercontent.com/86760678/130352435-18c4912e-11d7-4368-b0b5-0a8568bc740d.png)

### 주문 취소 수행 후, ordertraces

![image](https://user-images.githubusercontent.com/86760678/130352458-f2b7ad3e-4b00-4fb8-a06e-75e985475c53.png)

