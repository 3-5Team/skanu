package sktkanumodel;

import sktkanumodel.config.kafka.KafkaProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;


import java.util.List;
// import java.io.IOException;
// import java.util.Optional;

@Service
public class OrderTraceViewHandler {


    @Autowired
    private OrderTraceRepository orderTraceRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void whenOrdered_then_CREATE_1 (@Payload Ordered ordered) {
        try {

            if (!ordered.validate()) return;

            // view 객체 생성
            OrderTrace orderTrace = new OrderTrace();
            // view 객체에 이벤트의 Value 를 set 함
            orderTrace.setOrderId(ordered.getId());
            orderTrace.setProductId(ordered.getProductId());
            orderTrace.setQty(ordered.getQty());
            orderTrace.setCost(ordered.getCost());
            orderTrace.setProductName(ordered.getProductName());
            orderTrace.setStatus("Order END");
            // view 레파지 토리에 save
            orderTraceRepository.save(orderTrace);

        }catch (Exception e){
            e.printStackTrace();
        }
    }


    @StreamListener(KafkaProcessor.INPUT)
    public void whenPaid_then_UPDATE(@Payload Paid paid) {
        try {
            if (!paid.validate()) return;
                // view 객체 조회

                List<OrderTrace> orderTraceList = orderTraceRepository.findByOrderId(paid.getOrderId());
                for(OrderTrace orderTrace : orderTraceList){
                    // view 객체에 이벤트의 eventDirectValue 를 set 함
                    if("END".equals(paid.getPaymentStatus())){
                        orderTrace.setStatus("PAY END");
                        // view 레파지 토리에 save
                        orderTraceRepository.save(orderTrace);
                    }
                }

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @StreamListener(KafkaProcessor.INPUT)
    public void whenDelivered_then_UPDATE_1(@Payload Delivered delivered) {
        try {
            if (!delivered.validate()) return;
                // view 객체 조회

                List<OrderTrace> orderTraceList = orderTraceRepository.findByOrderId(delivered.getOrderId());
                for(OrderTrace orderTrace : orderTraceList){
                    // view 객체에 이벤트의 eventDirectValue 를 set 함
                    if("START".equals(delivered.getDeliveryStatus())){
                        orderTrace.setStatus("DELEVERY START");
                        // view 레파지 토리에 save
                        orderTraceRepository.save(orderTrace);
                    }
                }

        }catch (Exception e){
            e.printStackTrace();
        }
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void whenDeliveryCancelled_then_UPDATE_2(@Payload DeliveryCancelled deliveryCancelled) {
        try {
            if (!deliveryCancelled.validate()) return;
                // view 객체 조회

                    List<OrderTrace> orderTraceList = orderTraceRepository.findByOrderId(deliveryCancelled.getOrderId());
                    for(OrderTrace orderTrace : orderTraceList){
                    // view 객체에 이벤트의 eventDirectValue 를 set 함
                    orderTrace.setStatus("DELIVERY CANCEL");
                // view 레파지 토리에 save
                orderTraceRepository.save(orderTrace);
                }

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    
    @StreamListener(KafkaProcessor.INPUT)
    public void whenOrderCancelled_then_UPDATE_3 (@Payload OrderCancelled orderCancelled) {
        try {

            if (!orderCancelled.validate()) return;

            List<OrderTrace> orderTraceList = orderTraceRepository.findByOrderId(orderCancelled.getId());
                for(OrderTrace orderTrace : orderTraceList){
                // view 객체에 이벤트의 eventDirectValue 를 set 함
                orderTrace.setStatus("ORDER CANCEL");
                // view 레파지 토리에 save
                orderTraceRepository.save(orderTrace);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}

