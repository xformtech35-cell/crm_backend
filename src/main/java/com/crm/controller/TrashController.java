package com.crm.controller;

import com.crm.dto.response.ApiResponse;
import com.crm.dto.response.TrashItemResponse;
import com.crm.entity.User;
import com.crm.service.TrashService;
import com.crm.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/trash")
@RequiredArgsConstructor
public class TrashController {

    private final TrashService trashService;
    private final AuthUtil authUtil;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TrashItemResponse>>> getAllTrash(Authentication auth) {
        User user = authUtil.getCurrentUser(auth);
        return ResponseEntity.ok(ApiResponse.success("Trash items fetched", trashService.getAllTrashItems(user)));
    }

    @PostMapping("/restore/{module}/{id}")
    public ResponseEntity<ApiResponse<Void>> restoreItem(@PathVariable String module, @PathVariable Long id, Authentication auth) {
        User user = authUtil.getCurrentUser(auth);
        trashService.restoreItem(module, id, user);
        return ResponseEntity.ok(ApiResponse.success("Item restored successfully", null));
    }

    @PostMapping("/request-delete/{module}/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> requestPermanentDelete(
            @PathVariable String module, 
            @PathVariable Long id, 
            @RequestBody(required = false) Map<String, String> body,
            Authentication auth) {
        User user = authUtil.getCurrentUser(auth);
        String reason = body != null ? body.get("reason") : null;
        Map<String, Object> result = trashService.requestPermanentDelete(module, id, reason, user);
        return ResponseEntity.ok(ApiResponse.success("Permanent deletion request submitted. Company Administrator and Team Lead have been notified.", result));
    }

    @DeleteMapping("/permanent/{module}/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deletePermanently(@PathVariable String module, @PathVariable Long id, Authentication auth) {
        User user = authUtil.getCurrentUser(auth);
        Map<String, Object> result = trashService.deletePermanently(module, id, user);
        String msg = (result != null && Boolean.TRUE.equals(result.get("isDeleted"))) 
                ? "Item permanently deleted" 
                : "Permanent deletion request submitted and notifications dispatched to Company Administrator and Team Lead";
        return ResponseEntity.ok(ApiResponse.success(msg, result));
    }
}
