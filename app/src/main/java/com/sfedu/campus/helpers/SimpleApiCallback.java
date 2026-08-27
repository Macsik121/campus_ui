package com.sfedu.campus.helpers;

import com.sfedu.campus.generated.invoker.ApiCallback;
import com.sfedu.campus.generated.invoker.ApiException;
import java.util.List;
import java.util.Map;

// Абстрактный класс, который наследует все методы, кроме onSuccess и onFailure
public abstract class SimpleApiCallback<T> implements ApiCallback<T> {
    public SimpleApiCallback() {}
    @Override
    public void onUploadProgress(long bytesWritten, long contentLength, boolean done) {
        // Пустая реализация (переопределяем только то, что нужно)
    }

    @Override
    public void onDownloadProgress(long bytesRead, long contentLength, boolean done) {
        // Пустая реализация
    }
}