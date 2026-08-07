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

    @DeleteMapping("/permanent/{module}/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePermanently(@PathVariable String module, @PathVariable Long id, Authentication auth) {
        User user = authUtil.getCurrentUser(auth);
        trashService.deletePermanently(module, id, user);
        return ResponseEntity.ok(ApiResponse.success("Item permanently deleted", null));
    }
}
