package com.Banking_app.controllers;
import com.Banking_app.dto.responseBodies.BootstrapStatusResponse;
import com.Banking_app.service.BootstrapService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthBootstrapController {
    private final BootstrapService bootstrapService;
    @Autowired
    public AuthBootstrapController(BootstrapService bootstrapService){
        this.bootstrapService = bootstrapService;
    }
    @GetMapping("/bootstrap-status")
    public ResponseEntity<BootstrapStatusResponse> getBootstrapStatus() {
        return ResponseEntity.ok(bootstrapService.getStatus());
    }
/*    @PostMapping("/bootstrap-admin/register")
    public ResponseEntity<UserProfileResponseBody> registerBootstrapAdmin(@Valid @RequestBody UserProfileRequestBody body) {
        UserProfileJpaEntity created = bootstrapService.registerFirstAdmin(
                body.getUsername(),
                body.getFullName(),
                body.getEmail(),
                body.getPassword()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(userProfileMapper.toResponse(created));
    }*/
}
