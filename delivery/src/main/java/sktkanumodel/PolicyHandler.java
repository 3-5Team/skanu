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
    @Autowired DeliveryRepository deliveryRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverPaid_StartDelivery(@Payload Paid paid){

        if(paid.validate())        {

            System.out.println("\n\n##### listener paid : " + paid.toJson() + "\n\n");
            
            Delivery del = new Delivery();
            if("END".equals(paid.getPaymentStatus())) {
                del.setPaymentId(paid.getId());
                del.setDeliveryStatus("Prepare Delivery...");
    //                del.setId(del.getId());
                del.setOrderId(paid.getOrderId());
                del.setProductId(paid.getProductId());
                del.setProductName(paid.getProductName());
                del.setQty(paid.getQty());
                deliveryRepository.save(del);
            }
        }
    }

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverPayCancelled_CancelDelivery(@Payload PayCancelled payCancelled){

        if(payCancelled.validate())
        {
            System.out.println("\n\n##### listener paid : " + payCancelled.toJson() + "\n\n");
            List<Delivery> deliveryList = deliveryRepository.findByPaymentId(payCancelled.getId());
            if(deliveryList.size()>0) {
                for(Delivery delivery : deliveryList) {
                    if(delivery.getOrderId().equals(payCancelled.getId())){
                        deliveryRepository.deleteById(delivery.getId());
                    }
                }
            }
        }

    }


    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString){}


}