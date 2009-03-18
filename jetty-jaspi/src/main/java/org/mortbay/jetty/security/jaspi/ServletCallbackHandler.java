/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.mortbay.jetty.security.jaspi;

import java.io.IOException;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.message.callback.CallerPrincipalCallback;
import javax.security.auth.message.callback.CertStoreCallback;
import javax.security.auth.message.callback.GroupPrincipalCallback;
import javax.security.auth.message.callback.PasswordValidationCallback;
import javax.security.auth.message.callback.PrivateKeyCallback;
import javax.security.auth.message.callback.SecretKeyCallback;
import javax.security.auth.message.callback.TrustStoreCallback;

import org.mortbay.jetty.security.LoginService;
import org.mortbay.jetty.security.authentication.LoginCallback;
import org.mortbay.jetty.security.authentication.LoginCallbackImpl;
import org.mortbay.jetty.security.jaspi.callback.CredentialValidationCallback;
import org.mortbay.jetty.server.UserIdentity;

/**
 * 
 * Idiot class required by jaspi stupidity
 * 
 * @#*($)#@&^)$@#&*$@
 * @version $Rev$ $Date$
 */
public class ServletCallbackHandler implements CallbackHandler
{
    private final LoginService _loginService;

    private final ThreadLocal<CallerPrincipalCallback> _callerPrincipals = new ThreadLocal<CallerPrincipalCallback>();
    private final ThreadLocal<GroupPrincipalCallback> _groupPrincipals = new ThreadLocal<GroupPrincipalCallback>();

    public ServletCallbackHandler(LoginService loginService)
    {
        _loginService = loginService;
    }

    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException
    {
        for (Callback callback : callbacks)
        {
            // jaspi to server communication
            if (callback instanceof CallerPrincipalCallback)
            {
                _callerPrincipals.set((CallerPrincipalCallback) callback);
            }
            else if (callback instanceof GroupPrincipalCallback)
            {
                _groupPrincipals.set((GroupPrincipalCallback) callback);
            }
            else if (callback instanceof PasswordValidationCallback)
            {
                PasswordValidationCallback passwordValidationCallback = (PasswordValidationCallback) callback;
                Subject subject = passwordValidationCallback.getSubject();

                UserIdentity user = _loginService.login(passwordValidationCallback.getUsername(),passwordValidationCallback.getPassword());
                
                if (user!=null)
                {
                    passwordValidationCallback.setResult(true);
                    passwordValidationCallback.getSubject().getPrincipals().addAll(user.getSubject().getPrincipals());
                    passwordValidationCallback.getSubject().getPrivateCredentials().add(user);
                }
            }
            else if (callback instanceof CredentialValidationCallback)
            {
                CredentialValidationCallback credentialValidationCallback = (CredentialValidationCallback) callback;
                Subject subject = credentialValidationCallback.getSubject();
                LoginCallback loginCallback = new LoginCallbackImpl(subject,
                        credentialValidationCallback.getUsername(),
                        credentialValidationCallback.getCredential());

                UserIdentity user = _loginService.login(credentialValidationCallback.getUsername(),credentialValidationCallback.getCredential());

                if (user!=null)
                {
                    credentialValidationCallback.setResult(true);

                    credentialValidationCallback.getSubject().getPrincipals().addAll(user.getSubject().getPrincipals());
                    credentialValidationCallback.getSubject().getPrivateCredentials().add(user);
                }
            }
            // server to jaspi communication
            // TODO implement these
            else if (callback instanceof CertStoreCallback)
            {
            }
            else if (callback instanceof PrivateKeyCallback)
            {
            }
            else if (callback instanceof SecretKeyCallback)
            {
            }
            else if (callback instanceof TrustStoreCallback)
            {
            }
            else
            {
                throw new UnsupportedCallbackException(callback);
            }
        }
    }

    public CallerPrincipalCallback getThreadCallerPrincipalCallback()
    {
        CallerPrincipalCallback callerPrincipalCallback = _callerPrincipals.get();
        _callerPrincipals.remove();
        return callerPrincipalCallback;
    }

    public GroupPrincipalCallback getThreadGroupPrincipalCallback()
    {
        GroupPrincipalCallback groupPrincipalCallback = _groupPrincipals.get();
        _groupPrincipals.remove();
        return groupPrincipalCallback;
    }
}
