package com.sfedu.campus.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class RegViewModel extends ViewModel {
    private final MutableLiveData<String> email = new MutableLiveData<>();
    private final MutableLiveData<String> password = new MutableLiveData<>();
    private final MutableLiveData<String> name = new MutableLiveData<>();

    public LiveData<String> getEmail() { return email; }
    public void setEmail(String value) { email.setValue(value); }

    public LiveData<String> getPassword() { return password; }
    public void setPassword(String value) { password.setValue(value); }

    public LiveData<String> getName() { return name; }
    public void setName(String value) { name.setValue(value); }
}
