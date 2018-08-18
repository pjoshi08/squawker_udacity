package android.example.com.squawker.fcm;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Intent;
import android.example.com.squawker.R;
import android.example.com.squawker.provider.SquawkContract;
import android.example.com.squawker.provider.SquawkProvider;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

// COMPLETED (1) Make a new Service in the fcm package that extends from FirebaseMessagingService.
// COMPLETED (2) As part of the new Service - Override onMessageReceived. This method will
// be triggered whenever a squawk is received. You can get the data from the squawk
// message using getData(). When you send a test message, this data will include the
// following key/value pairs:
// test: true
// author: Ex. "TestAccount"
// authorKey: Ex. "key_test"
// message: Ex. "Hello world"
// date: Ex. 1484358455343
public class MyFirebaseMessageService extends FirebaseMessagingService {

    private static final String JSON_KEY_AUTHOR = SquawkContract.COLUMN_AUTHOR;
    private static final String JSON_KEY_AUTHOR_KEY = SquawkContract.COLUMN_AUTHOR_KEY;
    private static final String JSON_KEY_MESSAGE = SquawkContract.COLUMN_MESSAGE;
    private static final String JSON_KEY_DATE = SquawkContract.COLUMN_DATE;

    private static final int NOTIFICATION_MAX_CHARACTERS = 30;
    private static String LOG_TAG = MyFirebaseMessageService.class.getSimpleName();

    // COMPLETED (3) As part of the new Service - If there is message data, get the data using
    // the keys and do two things with it :
    // 1. Display a notification with the first 30 character of the message
    // 2. Use the content provider to insert a new message into the local database
    // Hint: You shouldn't be doing content provider operations on the main thread.
    // If you don't know how to make notifications or interact with a content provider
    // look at the notes in the classroom for help.

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // There are two types of messages data messages and notification messages. Data messages
        // are handled
        // here in onMessageReceived whether the app is in the foreground or background. Data
        // messages are the type
        // traditionally used with FCM. Notification messages are only received here in
        // onMessageReceived when the app
        // is in the foreground. When the app is in the background an automatically generated
        // notification is displayed.
        // When the user taps on the notification they are returned to the app. Messages
        // containing both notification
        // and data payloads are treated as notification messages. The Firebase console always
        // sends notification
        // messages. For more see: https://firebase.google.com/docs/cloud-messaging/concept-options\
        // The Squawk server always sends just *data* messages, meaning that onMessageReceived when
        // the app is both in the foreground AND the background

        Log.d(LOG_TAG, "From: " + remoteMessage.getFrom());

        Map<String, String> data = remoteMessage.getData();
        if(data.size() > 0) {
            Log.d(LOG_TAG, "Message data: " + data);

            // Add the message in the database and send a notification that you got a new squak
            sendNotification(data);
            insertSquawk(data);
            /*String author = data.get("author");
            String authorKey = data.get("authorKey");
            String message = data.get("message");
            String date = data.get("date");*/
        }
    }

    private void insertSquawk(final Map<String, String> data){

        // Database operations should not be done on the Main Thread
        AsyncTask<Void, Void, Void> insertSquawkTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                ContentValues newMessage = new ContentValues();
                newMessage.put(SquawkContract.COLUMN_AUTHOR, data.get(JSON_KEY_AUTHOR));
                newMessage.put(SquawkContract.COLUMN_AUTHOR_KEY, data.get(JSON_KEY_AUTHOR_KEY));
                newMessage.put(SquawkContract.COLUMN_MESSAGE, data.get(JSON_KEY_MESSAGE));
                newMessage.put(SquawkContract.COLUMN_DATE, data.get(JSON_KEY_DATE));

                getContentResolver().insert(SquawkProvider.SquawkMessages.CONTENT_URI, newMessage);
                return null;
            }
        };

        insertSquawkTask.execute();
    }

    private void sendNotification(Map<String, String> data) {
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // Pending intent to launch the activity
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT);

        String author = data.get(JSON_KEY_AUTHOR);
        String message = data.get(JSON_KEY_MESSAGE);

        // If the message is longer than the max number of characters we want in our
        // notification, truncate it and add the unicode character for ellipsis
        if (message.length() > NOTIFICATION_MAX_CHARACTERS) {
            message = message.substring(0, NOTIFICATION_MAX_CHARACTERS) + "\u2026";
        }

        // Notification requires Notification channel on Devices running Oreo and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("0", "Test Channel",
                    NotificationManager.IMPORTANCE_DEFAULT);


            Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, "0")
                    .setSmallIcon(R.drawable.ic_duck)
                    .setContentTitle(String.format(getString(R.string.notification_message), author))
                    .setContentText(message)
                    .setAutoCancel(true)
                    .setSound(defaultSoundUri)
                    .setContentIntent(pendingIntent);

            NotificationManager notificationManager =
                    getSystemService(NotificationManager.class);

            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                notificationManager.notify(0, notificationBuilder.build());
            }
        }
    }
}
