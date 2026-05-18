package com.rally.translation;

import com.rally.domain.translation.TranslationService;
import com.rally.domain.translation.model.TranslationData;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * app 层门面，委托到 domain 层 TranslationAppService，
 * 保持对 adapter/job 层的调用接口不变
 */
@Service
public class TranslationAppService {

    @Resource
    private TranslationService delegate;

    public int batch() {
        return delegate.batch();
    }

    public List<String> process(List<TranslationData> data) {
        return delegate.process(data);
    }

    public List<TranslationData> findAllPending() {
        return delegate.findAllPending();
    }







}
