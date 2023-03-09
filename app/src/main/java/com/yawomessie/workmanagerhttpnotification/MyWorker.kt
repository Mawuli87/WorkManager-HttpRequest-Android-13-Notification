package com.yawomessie.workmanagerhttpnotification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent.*
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.media.RingtoneManager.getDefaultUri
import android.net.Uri
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.loopj.android.http.AsyncHttpResponseHandler
import com.loopj.android.http.SyncHttpClient
import cz.msebera.android.httpclient.Header
import org.json.JSONObject
import java.text.DecimalFormat


class MyWorker(private val context: Context,workerParameters: WorkerParameters):
    Worker(context,workerParameters) {

    companion object {
        private val TAG = "MyWorker"
        const val APP_ID = "c55d4a58da4dc4d141c1adbcb64cd509"
        const val EXTRA_CITY = "city"
        const val CHANNEL_ID = "channel_id"
        const val CHANNEL_NAME = "Channel_name"

    }


    private var resultStatus: Result? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun doWork(): Result {

        val dataCity = inputData.getString(EXTRA_CITY)
        return getCurrentWeather(dataCity)
    }

    private fun getCurrentWeather(city: String?): Result {
        Log.d(TAG, "getCurrentWeather: Start.....")
        val client = SyncHttpClient()
        val url = "https://api.openweathermap.org/data/2.5/weather?q=$city&appid=$APP_ID"
        Log.d(TAG, "getCurrentWeather: $url")
        Looper.prepare()
        client.post(url, object : AsyncHttpResponseHandler() {


            @RequiresApi(Build.VERSION_CODES.S)
            override fun onSuccess(
                statusCode: Int,
                headers: Array<out Header>?,
                responseBody: ByteArray?
            ) {
                val result = String(responseBody as ByteArray)
                Log.d(TAG, result)
                try {
                    val responseObject = JSONObject(result)
                    val currentWeather: String =
                        responseObject.getJSONArray("weather").getJSONObject(0).getString("main")
                    val description: String =
                        responseObject.getJSONArray("weather").getJSONObject(0)
                            .getString("description")
                    val tempInKelvin = responseObject.getJSONObject("main").getDouble("temp")
                    val tempInCelsius = tempInKelvin - 273
                    val temperature: String = DecimalFormat("##.##").format(tempInCelsius)
                    val title = "Current Weather in $city"
                    val message = "$currentWeather, $description with $temperature celsius"
                    //showNotification(title, message)
                    displayNotification(title, message)
                    Log.d(TAG, "onSuccess: Finish.....")
                    resultStatus = Result.success()
                } catch (e: Exception) {
                    //showNotification("Get Current Weather Not Success", e.message)
                    displayNotification("Get Current Weather Not Success", e.message)
                    Log.d(TAG, "onSuccess: Failed.....")
                    resultStatus = Result.failure()
                }
            }


            @RequiresApi(Build.VERSION_CODES.S)
            override fun onFailure(
                statusCode: Int,
                headers: Array<out Header>?,
                responseBody: ByteArray?,
                error: Throwable?
            ) {
                Log.d(TAG, "onFailure: Failed")
                displayNotification("Get Current Weather Failed", (error as Throwable).message)
                resultStatus = Result.failure()
            }
        })

        return resultStatus as Result
    }

  @RequiresApi(Build.VERSION_CODES.S)
  private fun displayNotification(title: String, description: String?){

    val notificationIntent = Intent(applicationContext, MainActivity::class.java)
    val alarmSound: Uri = getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    val activityIntent = Intent(context, MainActivity::class.java)



    val activityPendingIntent = getActivity(
        context,
        1,
        activityIntent,
        FLAG_UPDATE_CURRENT or FLAG_MUTABLE
    )

    val notificationManager =
        applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    val mBuilder: NotificationCompat.Builder =
        NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            // val notification: NotificationCompat.Builder =
            //  NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_baseline_notifications_24)
            .setContentTitle(title)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(description)
            )
            .setContentText(description).setAutoCancel(false)
    mBuilder.setSound(alarmSound)
    mBuilder.setContentIntent(activityPendingIntent)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel =
            NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
        mBuilder.setChannelId(CHANNEL_ID)
        notificationManager.createNotificationChannel(channel)
    }
    notificationManager.notify(12, mBuilder.build())

 }

}
