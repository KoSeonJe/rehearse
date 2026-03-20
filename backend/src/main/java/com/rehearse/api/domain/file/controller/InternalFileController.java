package com.rehearse.api.domain.file.controller;

import com.rehearse.api.domain.file.dto.FileMetadataResponse;
import com.rehearse.api.domain.file.dto.UpdateFileStatusRequest;
import com.rehearse.api.domain.file.service.InternalFileService;
import com.rehearse.api.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/internal/files")
@RequiredArgsConstructor
public class InternalFileController {

    private final InternalFileService internalFileService;

    @PutMapping("/{fileMetadataId}/status")
    public ResponseEntity<ApiResponse<Void>> updateFileStatus(
            @PathVariable Long fileMetadataId,
            @Valid @RequestBody UpdateFileStatusRequest request) {

        internalFileService.updateFileStatus(fileMetadataId, request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/{fileMetadataId}/retry-convert")
    public ResponseEntity<ApiResponse<Void>> retryConvert(
            @PathVariable Long fileMetadataId) {
        internalFileService.retryConvert(fileMetadataId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/by-s3-key")
    public ResponseEntity<ApiResponse<FileMetadataResponse>> findByS3Key(
            @RequestParam String key) {

        FileMetadataResponse response = FileMetadataResponse.from(internalFileService.findByS3Key(key));
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
