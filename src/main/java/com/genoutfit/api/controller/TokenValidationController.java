package com.genoutfit.api.controller;

import com.genoutfit.api.JwtTokenProvider;
import com.genoutfit.api.model.User;
import com.genoutfit.api.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.genoutfit.api.model.TokenValidationRequest;

import java.util.Map;

@RestController
public class TokenValidationController {

   @Autowired
   private JwtTokenProvider tokenProvider;

   @Autowired
   private UserService userService;

    @PostMapping("/api/validate-token")
    public ResponseEntity<?> validateToken(@RequestBody TokenValidationRequest request) {
        try {
            if (tokenProvider.validateToken(request.getToken())) {
                String userId = tokenProvider.getUserIdFromToken(request.getToken());
                User user = userService.getUserById(userId);

                return ResponseEntity.ok(Map.of(
                        "status", user.getOnboardingStatus().name(),
                        "email", user.getEmail()
                ));
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}