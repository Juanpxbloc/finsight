package com.finsight.finsight_api.user;

import com.finsight.finsight_api.user.UpdateEmailRequest;
import com.finsight.finsight_api.user.UpdatePasswordRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/users/settings")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PatchMapping("/email")
    public ResponseEntity<Map<String, String>> updateEmail(@Valid @RequestBody UpdateEmailRequest request) {
        userService.updateEmail(request);
        return ResponseEntity.ok(Map.of("message", "Email updated successfully."));
    }

    @PatchMapping("/password")
    public ResponseEntity<Map<String, String>> updatePassword(@Valid @RequestBody UpdatePasswordRequest request) {
        userService.updatePassword(request);
        return ResponseEntity.ok(Map.of("message", "Password updated successfully."));
    }
}