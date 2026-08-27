package com.rally.translation;

import com.rally.domain.translation.gateway.TranslationRepository;
import com.rally.domain.translation.model.TranslationData;
import com.rally.contentproduction.instanttexttranslation.activity.TranslateSubmittedTextsActivity;
import com.rally.contentproduction.pendingtranslationbatch.activity.TranslatePendingContentActivity;
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
    private TranslationRepository translationRepository;

    @Resource
    private TranslatePendingContentActivity translatePendingContentActivity;

    @Resource
    private TranslateSubmittedTextsActivity translateSubmittedTextsActivity;

    public int batch() {
        return translatePendingContentActivity.execute();
    }

    public List<String> process(List<TranslationData> data) {
        return translateSubmittedTextsActivity.execute(data);
    }

    public List<TranslationData> findAllPending() {
        return translationRepository.findAllPending();
    }







}
