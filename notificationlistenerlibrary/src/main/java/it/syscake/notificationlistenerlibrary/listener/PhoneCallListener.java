package it.syscake.notificationlistenerlibrary.listener;

import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

public class PhoneCallListener extends PhoneStateListener {

    private static final String TAG = PhoneCallListener.class.getSimpleName();

    public interface PhoneCallInterface {
        void outgoingCallStarted();
        void disconnected();
        void ingoingCallStarted();
    }

    private PhoneCallInterface listener;

    public void setListener(PhoneCallInterface listener) {
        this.listener = listener;
    }

    @Override
    public void onCallStateChanged(int state, String phoneNumber) {
        super.onCallStateChanged(state, phoneNumber);
        Log.d(TAG, String.format("onCallStateChanged: %d _%s_", state, phoneNumber));
        if (listener == null) {
            Log.d(TAG, "onCallStateChanged: no listener.");
            return;
        }

        switch (state) {
            case TelephonyManager.CALL_STATE_OFFHOOK: {
                listener.outgoingCallStarted();
                break;
            }

            case TelephonyManager.CALL_STATE_RINGING: {
                listener.ingoingCallStarted();
                break;
            }

            case TelephonyManager.CALL_STATE_IDLE:
            default: {
                listener.disconnected();
            }
        }

    }
}
