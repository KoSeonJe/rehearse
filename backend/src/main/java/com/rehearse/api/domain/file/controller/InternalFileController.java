package com.rehearse.api.domain.file.controller;

import com.rehearse.api.domain.file.dto.UpdateFileStatusRequest;
import com.rehearse.api.domain.file.entity.FileMetadata;
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

    @GetMapping("/by-s3-key")
    public ResponseEntity<ApiResponse<FileMetadataResponse>> findByS3Key(
            @RequestParam String key) {

        FileMetadata file = internalFileService.findByS3Key(key);
        FileMetadataResponse response = new FileMetadataResponse(
                file.getId(), file.getFileType().name(), file.getStatus().name(),
                file.getS3Key(), file.getStreamingS3Key(), file.getBucket());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    public record FileMetadataResponse(Long id, String fileType, String status,
                                        String s3Key, String streamingS3Key, String bucket) {}
}
