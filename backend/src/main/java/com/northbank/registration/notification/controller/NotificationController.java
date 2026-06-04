// Story: US-016
package com.northbank.registration.notification.controller;

import com.northbank.registration.notification.exception.NotificationAccessDeniedException;
import com.northbank.registration.notification.service.NotificationService;
import com.northbank.registration.notification.service.dto.NotificationResponse;
import com.northbank.registration.shared.security.AuthenticatedCustomer;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Customer notification inbox")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<NotificationResponse>> list(
            @RequestParam(required = false) UUID customerId,
            @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal Object principal) {

        UUID authenticatedCustomerId = AuthenticatedCustomer.resolveId(principal);
        if (customerId != null && !customerId.equals(authenticatedCustomerId)) {
            throw new NotificationAccessDeniedException();
        }
        return ResponseEntity.ok(notificationService.listCustomerNotifications(authenticatedCustomerId, pageable));
    }
}
