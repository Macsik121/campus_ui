package com.sfedu.campus.data.datasource;

public interface DataCallback<T> {
    void onSuccess(T data);
    void onError(String error);
}
