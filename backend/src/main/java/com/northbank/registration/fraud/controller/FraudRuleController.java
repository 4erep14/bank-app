// Story: US-014
package com.northbank.registration.fraud.controller;

import com.northbank.registration.fraud.service.FraudRuleService;
import com.northbank.registration.fraud.service.dto.CreateFraudRuleRequest;
import com.northbank.registration.fraud.service.dto.FraudRuleResponse;
import com.northbank.registration.fraud.service.dto.UpdateFraudRuleRequest;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/fraud/rules")
@RequiredArgsConstructor
@Tag(name = "Fraud Rules", description = "Fraud analyst rule management")
@SecurityRequirement(name = "bearerAuth")
public class FraudRuleController {

    private final FraudRuleService fraudRuleService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FraudRuleResponse> create(@Valid @RequestBody CreateFraudRuleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(fraudRuleService.create(request));
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<FraudRuleResponse>> list(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(fraudRuleService.list(pageable));
    }

    @PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FraudRuleResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateFraudRuleRequest request) {

        return ResponseEntity.ok(fraudRuleService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        fraudRuleService.delete(id);
    }
}
