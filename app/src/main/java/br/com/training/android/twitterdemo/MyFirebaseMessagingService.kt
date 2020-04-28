package br.com.training.android.twitterdemo

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService: FirebaseMessagingService() {

    override fun onMessageReceived(p0: RemoteMessage) {
        super.onMessageReceived(p0)

            // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
            Log.d("TAG_Firebase_Messaging", "From: " + p0.from);

            // Check if message contains a data payload.
            if (p0.data.isNotEmpty()) {
                Log.d("TAG_Firebase_Messaging", "Message data payload: " + p0.data);

                if (/* Check if data needs to be processed by long running job */ true) {
                    // For long-running tasks (10 seconds or more) use Firebase Job Dispatcher.
                    //
                } else {
                    // Handle message within 10 seconds
//                    handleNow();
                }

            }

            // Check if message contains a notification payload.
            if (p0.notification != null) {
                Log.d("TAG_Firebase_Messaging", "Message Notification Body: " + p0.notification!!.body);
            }
    }

}