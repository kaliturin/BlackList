package com.kaliturin.blacklist;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

/**
 * HeadlessSmsSendService stub
 */

public class HeadlessSmsSendService extends Service {
    public HeadlessSmsSendService() {
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
