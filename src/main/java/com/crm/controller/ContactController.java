package com.crm.controller;

import com.crm.dto.request.ContactRequest;
import com.crm.dto.response.ApiResponse;
import com.crm.entity.Contact;
import com.crm.entity.User;
import com.crm.service.ContactService;
import com.crm.util.AuthUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contacts")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;
    private final AuthUtil authUtil;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Contact>>> getAll(Authentication auth) {
        User user = authUtil.getCurrentUser(auth);
        return ResponseEntity.ok(ApiResponse.success("Contacts fetched",
                contactService.getAllContacts(user.getUserid(), user.getRole())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Contact>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Contact fetched", contactService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Contact>> create(@Valid @RequestBody ContactRequest request, Authentication auth) {
        User user = authUtil.getCurrentUser(auth);
        return ResponseEntity.ok(ApiResponse.success("Contact created", contactService.create(request, user.getUserid())));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Contact>> update(@PathVariable Long id, @Valid @RequestBody ContactRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Contact updated", contactService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        contactService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Contact deleted", null));
    }
}
