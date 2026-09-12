package com.samu.customerservice.customer;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    boolean existsByCpf(String cpf);

    List<Customer> findByNameContainingIgnoreCase(String name);

    List<Customer> findByStatus(CustomerStatus status);
}
