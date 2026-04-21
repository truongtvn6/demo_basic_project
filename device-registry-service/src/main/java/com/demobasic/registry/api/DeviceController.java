package com.demobasic.registry.api;

import com.demobasic.registry.domain.DeviceToken;
import com.demobasic.registry.domain.DeviceTokenRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping
public class DeviceController {

    private final DeviceTokenRepository repository;

    public DeviceController(DeviceTokenRepository repository) {
        this.repository = repository;
    }

    public record RegisterRequest(@NotBlank String token, String label) {
    }

    public record TokenView(Long id, String token, String label, OffsetDateTime createdAt) {
        static TokenView from(DeviceToken t) {
            return new TokenView(t.getId(), t.getToken(), t.getLabel(), t.getCreatedAt());
        }
    }

    @PostMapping("/devices/register")
    @Transactional
    public ResponseEntity<TokenView> register(@Valid @RequestBody RegisterRequest request) {
        DeviceToken saved = repository.findByToken(request.token())
                .orElseGet(() -> repository.save(new DeviceToken(request.token(), request.label())));
        return ResponseEntity.ok(TokenView.from(saved));
    }

    @GetMapping("/internal/tokens")
    public List<TokenView> listTokens() {
        return repository.findAll().stream().map(TokenView::from).toList();
    }

    @GetMapping("/devices")
    public List<TokenView> listDevices() {
        return repository.findAll().stream().map(TokenView::from).toList();
    }

    @DeleteMapping("/devices/{id}")
    @Transactional
    public ResponseEntity<Void> deleteDevice(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
