package com.cx.sdk.IntegrationTests.login;

import com.cx.sdk.providers.CredentialsValidator;

/**
 * Created by ehuds on 2/26/2017.
 */
public class CredentialsValidatorImp implements CredentialsValidator {
    public void validate(String userName, String password) {
        if (userName.isEmpty() || password.isEmpty())
            throw new IllegalArgumentException();
    }
}