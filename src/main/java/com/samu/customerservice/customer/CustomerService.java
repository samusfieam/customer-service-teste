package com.samu.customerservice.customer;

import com.samu.customerservice.customer.dto.CreateCustomerRequest;
import com.samu.customerservice.customer.dto.CustomerResponse;
import com.samu.customerservice.exception.CustomerAlreadyExistsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional
    public CustomerResponse create(CreateCustomerRequest request) {
        if (customerRepository.existsByCpf(request.getCpf())) {
            throw new CustomerAlreadyExistsException(request.getCpf());
        }

        Customer customer = new Customer(
                request.getName(), request.getCpf(), request.getEmail(), request.getStatus());
        Customer savedCustomer = customerRepository.save(customer);

        return new CustomerResponse(
                savedCustomer.getId(),
                savedCustomer.getName(),
                savedCustomer.getCpf(),
                savedCustomer.getEmail(),
                savedCustomer.getStatus());
    }
}
