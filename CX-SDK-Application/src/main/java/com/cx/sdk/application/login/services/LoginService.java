package com.cx.sdk.application.login.services;

import com.cx.sdk.login.Session;

/**
 * Created by ehuds on 2/22/2017.
 */
public interface LoginService {
    Session login(String userName, String password);
}