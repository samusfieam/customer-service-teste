package com.samu.customerservice.customer;

import com.samu.customerservice.customer.dto.CreateCustomerRequest;
import com.samu.customerservice.customer.dto.CustomerResponse;
import com.samu.customerservice.customer.dto.UpdateCustomerRequest;
import com.samu.customerservice.exception.CustomerAlreadyExistsException;
import com.samu.customerservice.exception.CustomerNotFoundException;
import java.util.ArrayList;
import java.util.List;
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

    @Transactional(readOnly = true)
    public CustomerResponse findById(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException(id));

        return new CustomerResponse(
                customer.getId(),
                customer.getName(),
                customer.getCpf(),
                customer.getEmail(),
                customer.getStatus());
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> findAll(CustomerStatus status) {
        List<Customer> customers;
        if (status == null) {
            customers = customerRepository.findAll();
        } else {
            customers = customerRepository.findByStatus(status);
        }

        List<CustomerResponse> responses = new ArrayList<>();
        for (Customer customer : customers) {
            responses.add(new CustomerResponse(
                    customer.getId(),
                    customer.getName(),
                    customer.getCpf(),
                    customer.getEmail(),
                    customer.getStatus()));
        }

        return responses;
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> searchByName(String name) {
        List<Customer> customers = customerRepository.findByNameContainingIgnoreCase(name);

        List<CustomerResponse> responses = new ArrayList<>();
        for (Customer customer : customers) {
            responses.add(new CustomerResponse(
                    customer.getId(),
                    customer.getName(),
                    customer.getCpf(),
                    customer.getEmail(),
                    customer.getStatus()));
        }

        return responses;
    }

    @Transactional
    public CustomerResponse update(Long id, UpdateCustomerRequest request) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException(id));

        customer.setName(request.getName());
        customer.setEmail(request.getEmail());
        customer.setStatus(request.getStatus());

        Customer savedCustomer = customerRepository.save(customer);

        return new CustomerResponse(
                savedCustomer.getId(),
                savedCustomer.getName(),
                savedCustomer.getCpf(),
                savedCustomer.getEmail(),
                savedCustomer.getStatus());
    }

    @Transactional
    public void delete(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException(id));

        customerRepository.delete(customer);
    }
}
