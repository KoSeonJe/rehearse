package com.rehearse.api.domain.questionset.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UploadUrlResponse {

    private final String uploadUrl;
    private final String s3Key;
    private final Long fileMetadataId;
}
