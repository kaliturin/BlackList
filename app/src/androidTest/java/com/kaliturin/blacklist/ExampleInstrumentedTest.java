package com.kaliturin.blacklist;

import android.content.Context;
import android.os.Build;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.Log;

import com.kaliturin.blacklist.utils.ContactsAccessHelper;
import com.kaliturin.blacklist.utils.SubscriptionHelper;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() throws Exception {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();

        assertEquals("com.kaliturin.blacklist", appContext.getPackageName());
    }

    @Test
    public void privatePhoneNumber() throws Exception {
        String[] privateNumbers = {null, "-1", "-2", " -3", " -4\n", " ", "\n", "\t", ""};
        for (String number : privateNumbers) {
            assertTrue("number = {" + number + "} is not private", ContactsAccessHelper.isPrivatePhoneNumber(number));
        }

        String[] normalNumbers = {"-2-2", "-2 -2", "+01234567890", "+0 123 456 78 90", "0 123 456 78 90",
                "(123) 456 78 90", "(123)-456-78-90", "0 (123)-456-78-90", "-4 (123)-456-78-90"};
        for (String number : normalNumbers) {
            assertFalse("number = {" + number + "} is private", ContactsAccessHelper.isPrivatePhoneNumber(number));
        }
    }

    @Test
    public void normalizePhoneNumber() throws Exception {
        String normalizedNumber = "+01234567890";
        String[] notNormalizedNumbers = {"+0 123 456 78 90", "+0 (123) 456-78-90", "+0-123-456-78-90",
                "+ 0 (123) 456 78 90", " +0 123 456 78-90 ", "\n+ 0123 456 78 90\n"};
        for (String number : notNormalizedNumbers) {
            assertEquals("number = {" + number + "} cannot be normalized", normalizedNumber,
                    ContactsAccessHelper.normalizePhoneNumber(number));
        }
    }

    @Test
    public void smsSubscriptionManager() throws Exception {
        Context context = InstrumentationRegistry.getTargetContext();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            List<SubscriptionInfo> list = SubscriptionHelper.getSubscriptions(context);
            if (list != null) {
                int subscriptionId = -1;
                for (SubscriptionInfo info : list) {
                    Log.d("TEST_SmsManager", info.toString());
                    subscriptionId = info.getSubscriptionId();
                }
                assertTrue(subscriptionId >= 0);
            }
        }
    }
}
