package com.northbank.registration.customer.service.mapper;

import com.northbank.registration.customer.domain.model.Customer;
import com.northbank.registration.customer.service.dto.RegisterCustomerRequest;
import com.northbank.registration.customer.service.dto.RegisterCustomerResponse;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-03T19:32:28+0300",
    comments = "version: 1.6.2, compiler: Eclipse JDT (IDE) 3.46.0.v20260407-0427, environment: Java 21.0.10 (Eclipse Adoptium)"
)
@Component
public class CustomerMapperImpl implements CustomerMapper {

    @Override
    public Customer toEntity(RegisterCustomerRequest request, String passwordHash) {
        if ( request == null && passwordHash == null ) {
            return null;
        }

        Customer.CustomerBuilder customer = Customer.builder();

        if ( request != null ) {
            customer.email( request.email() );
            customer.dateOfBirth( request.dateOfBirth() );
            customer.firstName( request.firstName() );
            customer.lastName( request.lastName() );
            customer.phoneNumber( request.phoneNumber() );
        }
        customer.passwordHash( passwordHash );

        return customer.build();
    }

    @Override
    public RegisterCustomerResponse toResponse(Customer customer) {
        if ( customer == null ) {
            return null;
        }

        UUID id = null;

        id = customer.getId();

        RegisterCustomerResponse registerCustomerResponse = new RegisterCustomerResponse( id );

        return registerCustomerResponse;
    }
}
