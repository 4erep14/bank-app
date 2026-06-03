// Story: US-001
package com.northbank.registration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * NorthBank Customer Registration Service.
 * Entry-point for US-001: Customer Self-Registration.
 */
@SpringBootApplication
public class RegistrationApplication {

    public static void main(String[] args) {
        SpringApplication.run(RegistrationApplication.class, args);
    }
}
