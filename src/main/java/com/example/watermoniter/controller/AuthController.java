package com.example.watermoniter.controller;

import com.example.watermoniter.model.AppUser;
import com.example.watermoniter.service.AppUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final AppUserService appUserService;

    public AuthController(AuthenticationManager authenticationManager, AppUserService appUserService) {
        this.authenticationManager = authenticationManager;
        this.appUserService = appUserService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.get("username"), request.get("password"))
            );

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            httpRequest.getSession(true).setAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    context
            );

            AppUser user = appUserService.findDomainUser(authentication.getName());
            return ResponseEntity.ok(buildUserResponse(user));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid username or password"));
        }
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        try {
            AppUser created = appUserService.signup(
                    request.get("name"),
                    request.get("email"),
                    request.get("password")
            );

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(created.getUsername(), request.get("password"))
            );

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            httpRequest.getSession(true).setAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    context
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(buildUserResponse(created));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @GetMapping("/me")
    public Map<String, Object> currentUser(Authentication authentication) {
        AppUser user = appUserService.findDomainUser(authentication.getName());
        return buildUserResponse(user);
    }

    @PostMapping("/logout")
    public Map<String, String> logout(HttpServletRequest request, HttpServletResponse response) {
        SecurityContextHolder.clearContext();
        if (request.getSession(false) != null) {
            request.getSession(false).invalidate();
        }
        return Map.of("message", "Logged out");
    }

    private Map<String, Object> buildUserResponse(AppUser user) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", user.getId());
        response.put("username", user.getUsername());
        response.put("email", user.getUsername());
        response.put("displayName", user.getDisplayName());
        response.put("role", user.getRole());
        return response;
    }
}
