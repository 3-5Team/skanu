# SKANU
 ![image](https://user-images.githubusercontent.com/89397401/130740460-91135a6b-9676-460e-bac1-6f89d1da562f.png)

 
 # 서비스 시나리오

기능적 요구사항
1. 고객이 커피(음료)를 주문(Order)한다.
2. 고객이 지불(Pay)한다.
3. 결제모듈(payment)에 결제를 진행하게 되고 '지불'처리 된다.
4. 결제 '승인' 처리가 되면 주방에서 음료를 제조한다.
5. 고객과 매니저는 마이페이지를 통해 진행상태(OrderTrace)를 확인할 수 있다.
6. 음료가 준비되면 배달(Delivery)을 한다.
7. 고객이 취소(Cancel)하는 경우 지불 및 제조, 배달이 취소가 된다.

비기능적 요구사항
1. 트랜잭션
    1. 결제가 되지 않은 주문건은 등록이 성립되지 않는다. - Sync 호출
2. 장애격리
    1. 결제가 수행되지 않더라도 주문 취소은 365일 24시간 받을 수 있어야 한다  - Async(event-driven), Eventual Consistency
    2. 결제 시스템이 과중되면 주문(Order)을 잠시 후 처리하도록 유도한다  - Circuit breaker, fallback
3. 성능
    1. 마이페이지에서 주문상태(OrderTrace) 확인  - CQRS

# 분석/설계


## Event Storming 결과
* MSAEz 로 모델링한 이벤트스토밍 결과: http://labs.msaez.io/#/storming/P3HDhaDCvERl1kR9ZDeRJxSKcBj1/516387ea675136cbac7cb8ac3c903426

### 이벤트 도출
![image](https://user-images.githubusercontent.com/79756040/129881425-3b9d3209-16b3-4d8a-a565-c82a85056980.png)

### 부적격 이벤트 탈락
![image](https://user-images.githubusercontent.com/79756040/129881872-bfa9ddb8-1e01-4885-b688-8a68d9770db4.png)

### 완성된 1차 모형
![image](https://user-images.githubusercontent.com/79756040/129881929-c6d1f38e-4115-4b5c-b650-4573852f9dd6.png)

### 완성된 최종 모형 ( 시나리오 점검 후 )
![image](https://user-images.githubusercontent.com/79756040/130614202-d1ddaef6-466f-436f-a4a3-51714383d43a.png)

## 헥사고날 아키텍처 다이어그램 도출 
![HEXAGONAL2](https://user-images.githubusercontent.com/79756040/130914262-ec9dd0ea-0f13-4195-befd-566cb4de2620.png)
 
 # 구현
 
 ### 흐름도
 

 
 - 주문(order) 후 주문취소(cancel order) 시, 흐름도
![image](https://user-images.githubusercontent.com/86760678/130794931-a22b6b52-044b-4f96-95b0-86acd56014c7.png)

 - 주문(order) 후 결제수행(pay) 시, 흐름도
![image](https://user-images.githubusercontent.com/86760678/130794970-29d3b38e-ea00-43cc-a4d8-51f5144d3803.png)

 
 
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

    @Idㄹ패
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

# 동기식 호출(Req/Res 방식)과 Fallback 처리
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



# 운영
  
## Deploy / Pipeline

- git에서 소스 가져오기

```
git clone https://github.com/3-5Team/skanu.git
```

- Build 및 Azure Container Resistry(ACR) 에 Push 하기
 
```bash
cd ..
cd order
mvn package -B
az acr build --registry x0006319acr --image x0006319acr.azurecr.io/order:latest .

cd ..
cd payment
mvn package -B
az acr build --registry x0006319acr --image x0006319acr.azurecr.io/payment:latest .

cd ..
cd delivery
mvn package -B
az acr build --registry x0006319acr --image x0006319acr.azurecr.io/delivery:latest .

cd ..
cd ordertrace
mvn package -B
az acr build --registry x0006319acr --image x0006319acr.azurecr.io/ordertrace:latest .

cd ..
cd gateway
mvn package -B
az acr build --registry x0006319acr --image x0006319acr.azurecr.io/gateway:latest .

```

- ACR에 정상 Push 완료

![image](https://user-images.githubusercontent.com/89397401/130728262-6870ae32-b1fd-487d-9c38-1314b5fe9a23.png)

- Kafka 설치 및 배포 완료

![image](https://user-images.githubusercontent.com/89397401/130728767-545dc1e9-6c3b-4937-b3e7-0a8af179567c.png)

- Kubernetes Deployment, Service 생성

```sh
cd ..
cd order/kubernetes
kubectl apply -f deployment.yml
kubectl apply -f service.yaml

cd ..
cd payment/kubernetes
kubectl apply -f deployment.yml
kubectl apply -f service.yaml

cd ..
cd delivery/kubernetes
kubectl apply -f deployment.yml
kubectl apply -f service.yaml

cd ..
cd ordertrace/kubernetes
kubectl apply -f deployment.yml
kubectl apply -f service.yaml

cd ..
cd gateway/kubernetes
kubectl apply -f deployment.yml
kubectl apply -f service.yaml
```

/order/kubernetes/deployment.yml 파일 
```yml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: order
  labels:
    app: order
spec:
  replicas: 1
  selector:
    matchLabels:
      app: order
  template:
    metadata:
      labels:
        app: order
    spec:
      containers:
        - name: order
          image: x0006319acr.azurecr.io/order:latest
          ports:
            - containerPort: 8080
```

/order/kubernetes/service.yaml 파일 
```yml
apiVersion: v1
kind: Service
metadata:
  name: order
  labels:
    app: order
spec:
  ports:
    - port: 8080
      targetPort: 8080
  selector:
    app: order
```

전체 deploy 완료 현황

![image](https://user-images.githubusercontent.com/89397401/130646019-c0a9e001-3433-44da-8acd-9a1637a4b2e9.png)

## Persistence Volume

- 비정형 데이터를 관리하기 위해 PVC 생성 파일

pvc.yml
- AccessModes: **ReadWriteMany**
- storeageClass: **azurefile**
```yml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: order-disk
spec:
  accessModes:
  - ReadWriteMany
  storageClassName: azurefile
  resources:
    requests:
      storage: 1Gi
```
deploymeny.yml
```yml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: order
  labels:
    app: order
spec:
  replicas: 1
  -- 생략 --
  template:
  -- 생략 --
    spec:
      containers:
        - name: order
        -- 생략 --
          volumeMounts:
            - name: volume
              mountPath: "/mnt/azure"
      volumes:
      - name: volume
        persistentVolumeClaim:
          claimName: order-disk
```
application.yml
```yml
logging:
  level:
    root: info
  file: /mnt/azure/logs/order.log
```
- 로그 확인

![PVC-LOGS](https://user-images.githubusercontent.com/89397401/130727058-c8b4e2d1-b07e-4b4f-9fdb-9acfe25f5943.png)


## Autoscale (HPA:HorizontalPodAutoscaler)

- 특정 수치 이상으로 사용자 요청이 증가할 경우 안정적으로 운영 할 수 있도록 HPA를 설치한다.

order 서비스에 resource 사용량을 정의한다.

order/kubernetes/deployment.yml
```yml
  resources:
    requests:
      memory: "64Mi"
      cpu: "250m"
    limits:
      memory: "500Mi"
      cpu: "500m"
```

order 서비스에 대한 replica 를 동적으로 늘려주도록 HPA 를 설정한다. 설정은 CPU 사용량이 15%를 넘어서면 replica를 10개까지 늘려준다.

```sh
kubectl autoscale deploy order --min=1 --max=10 --cpu-percent=15
```

![image](https://user-images.githubusercontent.com/89397401/130735894-d889090c-95c6-4fa7-a1b5-aa1311e11eb9.png)

siege를 활용하여, 부하 생성한다. (2명의 동시사용자가 10초간 부하 발생)

```sh
siege -c2 -t10S -v --content-type "application/json" 'http://order:8080/orders POST { "productId": 1, "qty": 2, "paymentType" : "card", "cost" : 2000, "productName" : "RedTea"}'
```

- Autoscale을 확인하기 위해 모니터링한다.

```sh
$ watch kubectl get all
```

- 결과 확인 : 부하 생성 이후 CPU 15% 이상 사용 시 자동으로 POD가 증가하면서 Autoscale 됨을 확인 할 수 있다.

![image](https://user-images.githubusercontent.com/89397401/130736630-9a4de0c5-82d6-416c-a24a-11253d8412f0.png)

## Circuit Breaker

- Spring FeignClient + istio를 활용하여 Circuit Breaker 동작을 확인한다.
- istio 설치 후 istio injection이 enabled 된 namespace를 생성한다.
```
kubectl create namespace istio-test-ns
kubectl label namespace istio-test-ns istio-injection=enabled
```

- namespace label에 istio-injection이 enabled 된 것을 확인한다.  

![image](https://user-images.githubusercontent.com/89397401/130896837-0c6d68ec-f5e0-4ccf-be46-7f089829aaa7.png)

- 해당 namespace에 기존 서비스들을 재배포한다.

- deploy 실행
```Bash
kubectl create deploy order --image x0006319acr.azurecr.io/order:latest -n istio-test-ns
kubectl create deploy payment --image x0006319acr.azurecr.io/payment:latest -n istio-test-ns
kubectl create deploy delivery --image x0006319acr.azurecr.io/delivery:latest -n istio-test-ns
kubectl create deploy gateway --image x0006319acr.azurecr.io/gateway:latest -n istio-test-ns
kubectl create deploy ordertrace --image x0006319acr.azurecr.io/ordertrace:latest -n istio-test-ns
kubectl get all
```

- expose 하기
```Bash
kubectl expose deploy order --type="ClusterIP" --port=8080 -n istio-test-ns
kubectl expose deploy payment --type="ClusterIP" --port=8080 -n istio-test-ns
kubectl expose deploy delivery --type="ClusterIP" --port=8080 -n istio-test-ns
kubectl expose deploy gateway --type="LoadBalancer" --port=8080 -n istio-test-ns
kubectl expose deploy ordertrace --type="ClusterIP" --port=8080 -n istio-test-ns
kubectl get all
```

- 서비스들이 정상적으로 배포되었고, Container가 2개씩 생성된 것을 확인한다. 

![image](https://user-images.githubusercontent.com/89397401/130897459-f4d97481-bfb0-4422-957b-cab08b3fa9ef.png)

- Circuit Breaker 설정을 위해 아래와 같은 Destination Rule을 생성한다.

- Pending Request가 많을수록 오랫동안 쌓인 요청은 Response Time이 증가하게 되므로, 적절한 대기 쓰레드 풀을 적용하기 위해 connection pool을 설정했다.
```yaml
  apiVersion: networking.istio.io/v1alpha3
  kind: DestinationRule
  metadata:
    name: dr-httpbin
    namespace: istio-test-ns
  spec:
    host: gateway
    trafficPolicy:
      connectionPool:
        http:
          http1MaxPendingRequests: 1
          maxRequestsPerConnection: 1
```

- 설정된 Destinationrule을 확인한다. 

![image](https://user-images.githubusercontent.com/89397401/130916203-069469c2-248b-49a5-a23c-eedb442e3373.png)


- siege 를 활용하여 User가 2명인 상황에 대해서 요청을 보낸다. (설정값 c2)
  - siege 는 같은 namespace 에 생성하고, 해당 pod 안에 들어가서 siege 요청을 실행한다.
```
kubectl exec -it (siege POD 이름) -c siege -n istio-test-ns -- /bin/bash

siege -c2 -t30S -v --content-type "application/json" 'http://20.200.225.249:8080/orders POST {"productId": 1, "qty":3, "paymentTyp": "cash", "cost": 1000, "productName": "Coffee"}'
```

- 최종결과 확인
![image](https://user-images.githubusercontent.com/89397401/130823691-3df705c8-b964-4175-80c2-8970f4be57da.png)

- 운영시스템은 죽지 않고 지속적으로 Circuit Breaker 에 의하여 적절히 회로가 열림과 닫힘이 벌어지면서 자원을 보호하고 있음을 보여줌. 
- 약 84%정도 정상적으로 처리되었음.


## Zero-Downtime deploy (Readiness Probe)

- 먼저 무정지 재배포가 100% 되는 것인지 확인하기 위해서 Autoscaler 이나 CB 설정을 제거함
- 생성된 siege Pod 안쪽에서 정상작동 확인
```
kubectl exec -it siege -- /bin/bash
```

- siege 로 배포작업 직전에 워크로드를 모니터링 함
```
siege -c1 -t30S -v http://order:8080/orders
```

- Readiness가 설정되지 않은 yml 파일로 배포 진행
 
![image](https://user-images.githubusercontent.com/44763296/130768667-b3df9f46-d907-40aa-9304-4b0888a9983b.png)

```
kubectl apply -f deployment_without_readiness.yml
```

- 아래 그림과 같이, Kubernetes가 준비가 되지 않은 delivery pod에 요청을 보내서 siege의 Availability 가 100% 미만으로 떨어짐 

![image](https://user-images.githubusercontent.com/44763296/130476167-1b1eca10-ac7f-4065-86b7-69af9dcd7be5.png)


- 정지 재배포 여부 확인 전에, siege 로 배포작업 직전에 워크로드를 모니터링
```
siege -c1 -t60S -v http://order:8080/orders --delay=1S
```

- Readiness가 설정된 yml 파일로 배포 진행
```
readinessProbe:
  httpGet:
    path: '/actuator/health'
    port: 8080
  initialDelaySeconds: 10
  timeoutSeconds: 2
  periodSeconds: 5
  failureThreshold: 10
```

```
kubectl apply -f deployment_with_readiness.yml
```

- 배포 중 pod가 2개가 뜨고, 새롭게 띄운 pod가 준비될 때까지, 기존 pod가 유지됨을 확인
![image](https://user-images.githubusercontent.com/44763296/130477984-32b58cbf-a666-4e52-a377-d99c7a1477b7.png)
![image](https://user-images.githubusercontent.com/44763296/130478069-f3b1156e-bddb-48e3-ace9-ee21d3965206.png)


## Self-healing (Liveness Probe)

- order 서비스의 yml 파일에 liveness probe 설정을 바꾸어서, liveness probe 가 동작함을 확인

- liveness probe 옵션을 추가하되, 서비스 포트가 아닌 8090으로 설정, readiness probe 미적용
```
        livenessProbe:
            httpGet:
              path: '/actuator/health'
              port: 8090
            initialDelaySeconds: 5
            timeoutSeconds: 2
            periodSeconds: 5
            failureThreshold: 5
```

- order 서비스에 liveness가 적용된 것을 확인
```
kubectl describe po order
```
![image](https://user-images.githubusercontent.com/44763296/130482646-e598d23a-85b5-4e9d-b48b-6c9b99c7955f.png)


- order 에 liveness가 발동되었고, 8090 포트에 응답이 없기에 Restart가 발생함
![image](https://user-images.githubusercontent.com/44763296/130482675-ec20503f-a5d1-4c3d-9261-a89ae7ee17f1.png)
![image](https://user-images.githubusercontent.com/44763296/130482712-e18cda81-f3c8-47f7-abfc-47188cc423ad.png)


