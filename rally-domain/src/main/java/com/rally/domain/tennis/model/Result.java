package com.rally.domain.tennis.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    private int code;
    private T data;

    public static <T> Result<T> ok(T data) {
        return new Result<>(0, data);
    }
}
