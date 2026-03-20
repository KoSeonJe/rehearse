package com.rehearse.api.infra.ai;

import org.springframework.web.multipart.MultipartFile;

public interface SttService {

    String transcribe(MultipartFile audioFile);
}
