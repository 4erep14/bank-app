// Story: US-001
package com.northbank.registration.customer.controller;

import com.northbank.registration.customer.service.CustomerService;
import com.northbank.registration.customer.service.dto.RegisterCustomerRequest;
import com.northbank.registration.customer.service.dto.RegisterCustomerResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * REST controller for customer self-registration.
 *
 * <p>Exposes {@code POST /api/v1/customers} as a public (anonymous) endpoint.
 * All business logic is delegated to {@link CustomerService}.</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Tag(name = "Customers", description = "Customer self-registration endpoints")
public class CustomerController {

    private final CustomerService customerService;

    /**
     * Register a new customer.
     *
     * <p>On success the response body contains only the new customer {@code id}
     * and a {@code Location} header pointing to the new resource (AC6).</p>
     */
    @Operation(
        summary     = "Register a new customer",
        description = "Creates a customer record with status PENDING_VERIFICATION. " +
                      "The password is hashed (BCrypt strength 12) and never returned. " +
                      "Duplicate emails return 409."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description  = "Customer registered successfully",
            headers      = @Header(
                name        = "Location",
                description = "URI of the newly created customer resource",
                schema      = @Schema(type = "string", format = "uri")
            ),
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema    = @Schema(implementation = RegisterCustomerResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description  = "Validation failed — field-level errors returned",
            content      = @Content(
                mediaType = "application/problem+json",
                schema    = @Schema(implementation = ProblemDetail.class)
            )
        ),
        @ApiResponse(
            responseCode = "409",
            description  = "Email address is already registered",
            content      = @Content(
                mediaType = "application/problem+json",
                schema    = @Schema(implementation = ProblemDetail.class)
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description  = "Unexpected internal error",
            content      = @Content(
                mediaType = "application/problem+json",
                schema    = @Schema(implementation = ProblemDetail.class)
            )
        )
    })
    @PostMapping(
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<RegisterCustomerResponse> registerCustomer(
            @Valid @RequestBody RegisterCustomerRequest request) {

        log.debug("Registration request received for email domain: {}",
                sanitiseEmailForLog(request.email()));

        RegisterCustomerResponse response = customerService.registerCustomer(request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity
                .created(location)
                .body(response);
    }

    /**
     * Returns only the domain part of an email for safe logging — never logs
     * the local-part to minimise PII exposure.
     */
    private String sanitiseEmailForLog(String email) {
        if (email == null) return "unknown";
        int atIdx = email.indexOf('@');
        return atIdx >= 0 ? "*@" + email.substring(atIdx + 1) : "unknown";
    }
}
