package io.branch.referral;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.lang.reflect.Method;

/**
 * EMT-3861: the no-argument Branch.logout() overload that 5.x exposed was dropped in the beta, so
 * the very common branch.logout() call no longer compiles for upgraders. These checks pin the
 * public contract: the no-arg overload exists again, and the callback overload is left untouched.
 */
@RunWith(RobolectricTestRunner.class)
public class LogoutApiCompatTest {

    @Test
    public void noArgLogoutOverloadExists() throws NoSuchMethodException {
        Method logout = Branch.class.getMethod("logout");
        assertEquals("logout() must return void", void.class, logout.getReturnType());
    }

    @Test
    public void callbackLogoutOverloadUnchanged() throws NoSuchMethodException {
        Method logout = Branch.class.getMethod("logout", Branch.LogoutStatusListener.class);
        assertEquals("logout(LogoutStatusListener) must remain", void.class, logout.getReturnType());
    }
}
