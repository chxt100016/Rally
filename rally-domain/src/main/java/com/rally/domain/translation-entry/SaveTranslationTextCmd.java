package com.rally.domain.content.translationentry;

/**
 * C2 保存译文的命令入参。
 */
public final class SaveTranslationTextCmd {

    private final Long id;
    private final String translatedText;

    public SaveTranslationTextCmd(Long id, String translatedText) {
        this.id = id;
        this.translatedText = translatedText;
    }

    public Long getId() {
        return id;
    }

    public String getTranslatedText() {
        return translatedText;
    }
}
