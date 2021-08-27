package sktkanumodel;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
// import java.util.Optional;

@RepositoryRestResource(collectionResourceRel="payments", path="payments")
public interface PaymentRepository extends PagingAndSortingRepository<Payment, Long>{

    List<Payment> findByOrderId(Long OrderId);
    void deleteByOrderId(Long OrderId);
}
