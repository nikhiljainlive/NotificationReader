package com.nikhiljain.notificationreader.ui.services

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Icon
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.core.graphics.drawable.toBitmap
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentifier
import com.google.mlkit.nl.languageid.LanguageIdentifier.UNDETERMINED_LANGUAGE_TAG
import com.nikhiljain.notificationreader.R
import com.nikhiljain.notificationreader.model.NotificationContent
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class NotificationReaderService : Service() {
    private lateinit var languageIdentifier: LanguageIdentifier

    private var textToSpeech: TextToSpeech? = null
    private var whatsAppUserName: String? = null

    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val mainLaunch: (block: suspend CoroutineScope.() -> Unit) -> Job = {
        mainScope.launch(context = CoroutineExceptionHandler { _, e ->
            Log.e(TAG, "Coroutine failed ${e.localizedMessage}", e)
            return@CoroutineExceptionHandler
        }, block = it)
    }

    private val notificationContentReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val notification = IntentCompat.getParcelableExtra(
                intent, NotificationListenerExampleService.EXTRA_NOTIFICATION_CONTENT,
                NotificationContent::class.java
            ) ?: run {
                Log.e(TAG, "onReceive: notification is null")
                return
            }

            val title = notification.title
            val text = notification.text
            val largeIcon = notification.icon

            val smallIcon = R.drawable.ic_launcher_foreground
            startForeground(title.orEmpty(), text.orEmpty(), smallIcon, largeIcon)
            // remove emoji from
            val characterFilter = "[^\\p{L}\\p{M}\\p{N}\\p{P}\\p{Z}\\p{Cf}\\p{Cs}\\s]".toRegex()
            val message = buildString {
                if (whatsAppUserName == null || whatsAppUserName != title) {
                    whatsAppUserName = title
                    append("${title?.replace(characterFilter, "")} says")
                    append("\n")
                }
                append(text?.replace(characterFilter, ""))
            }

            mainLaunch {
                try {
                    var language = detectLanguage(message)
                    if (language == UNDETERMINED_LANGUAGE_TAG) {
                        language = LANGUAGE_CODE_HINDI // hindi language
                        showToastMessage(R.string.error_message_undefined_language_detected)
                    }

                    try {
                        convertTextToSpeech(language, message)
                    } catch (exception: IllegalStateException) {
                        if (exception.message != EXCEPTION_MESSAGE_LANGUAGE_MISSING) {
                            throw exception
                        }
                        /*showDownloadLanguageDialog(Locale.forLanguageTag(it).displayName)*/
                        Log.e(TAG, "onReceive: Language is missing")
                        // TODO :: show notification with pending intent to redirect to activity
                        //  so user can go to activity and we can show dialog to download missing language
                    }
                } catch (argumentException: IllegalArgumentException) {
                    if (argumentException.message != EXCEPTION_MESSAGE_EMPTY_TEXT) {
                        throw argumentException
                    }
                    showToastMessage(R.string.error_message_empty_text)
                } catch (_: Exception) {
                    showToastMessage(R.string.error_message_something_went_wrong)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        initLanguageIdentifier()
        mainLaunch {
            textToSpeech = initTextToSpeech()
            Log.e(TAG, "initTextToSpeech: engine ${textToSpeech?.Engine()}")
            for (engineInfo in (textToSpeech?.engines ?: return@mainLaunch)) {
                if (engineInfo.name.contains("google", ignoreCase = true)) {
                    textToSpeech = setGoogleTTSEngineIfPresent(engineInfo.name)
                    return@mainLaunch
                }
            }
        }
        val title = "Notification reader service is running"
        val text = "Service running"
        val smallIcon = R.drawable.ic_launcher_foreground
        startForeground(title, text, smallIcon)
    }

    private fun startForeground(
        title: String,
        text: String,
        smallIcon: Int,
        largeIcon: Icon? = null
    ) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(smallIcon)
            .also { builder ->
                largeIcon?.loadDrawable(this)?.toBitmap()?.let { bitmap ->
                    builder.setLargeIcon(bitmap)
                } ?: run {
                    Log.e(TAG, "startForeground: could not convert to bitmap")
                }
            }
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
        startForeground(100, notification)
    }

    override fun onBind(p0: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.getStringExtra(SERVICE_ACTION)) {
            ACTION_START_SERVICE -> {
                val intentFilter = IntentFilter()
                intentFilter.addAction(NotificationListenerExampleService.NOTIFICATION_RECEIVED_ACTION)
                ContextCompat.registerReceiver(
                    this,
                    notificationContentReceiver,
                    intentFilter,
                    ContextCompat.RECEIVER_NOT_EXPORTED
                )
            }

            ACTION_STOP_SERVICE -> {
                unregisterReceiver(notificationContentReceiver)
                stopSelf()
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            }
        }
        return START_NOT_STICKY
    }


    private fun initLanguageIdentifier() {
        languageIdentifier = LanguageIdentification.getClient()
    }

    private suspend fun initTextToSpeech() = suspendCoroutine { cont ->
        textToSpeech = TextToSpeech(this) {
            when (it) {
                TextToSpeech.SUCCESS -> {
                    cont.resume(textToSpeech!!)
                }

                TextToSpeech.ERROR -> {
                    cont.resumeWithException(IllegalStateException("Error in initializing TextToSpeech"))
                }
            }
        }
    }

    private suspend fun setGoogleTTSEngineIfPresent(engineName: String) =
        suspendCoroutine { cont ->
            textToSpeech = TextToSpeech(this, {
                when (it) {
                    TextToSpeech.ERROR -> {
                        Log.e(TAG, "setGoogleTTSEngineIfPresent: error in initializing tts")
                        cont.resumeWithException(IllegalStateException("Error in initializing TextToSpeech"))
                    }

                    else -> {
                        cont.resume(textToSpeech!!)
                    }
                }
            }, engineName)
        }

    private suspend fun detectLanguage(text: String) = suspendCoroutine<String> { cont ->
        text.takeIf { it.isNotEmpty() } ?: run {
            cont.resumeWithException(IllegalArgumentException(EXCEPTION_MESSAGE_EMPTY_TEXT))
            return@suspendCoroutine
        }

        languageIdentifier.identifyLanguage(text)
            .addOnSuccessListener { languageCode ->
                cont.resume(languageCode)
            }
            .addOnFailureListener {
                cont.resumeWithException(it)
            }
    }

    private suspend fun convertTextToSpeech(languageCode: String, text: String) =
        suspendCoroutine { cont ->
            val langAvailable = textToSpeech?.setLanguage(Locale.forLanguageTag(languageCode))
            if (langAvailable == TextToSpeech.LANG_MISSING_DATA ||
                langAvailable == TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                cont.resumeWithException(IllegalStateException(EXCEPTION_MESSAGE_LANGUAGE_MISSING))
                return@suspendCoroutine
            }

            if (text.isEmpty()) {
                cont.resumeWithException(IllegalArgumentException(EXCEPTION_MESSAGE_EMPTY_TEXT))
                return@suspendCoroutine
            }

            textToSpeech?.speak(
                text, TextToSpeech.QUEUE_ADD,       // add to the queue
                null, "TEXT"
            )
            cont.resume(Unit)
        }

    @Suppress("UNUSED")
    private fun showDownloadLanguageDialog(language: String) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.text_language_not_supported, language))
            .setMessage(getString(R.string.error_message_download_language, language))
            .setPositiveButton(android.R.string.ok) { _, _ ->
                try {
                    val installIntent = Intent()
                    installIntent.action = TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA
                    startActivity(installIntent)
                } catch (exception: Exception) {
                    exception.printStackTrace()
                    showToastMessage(R.string.error_message_something_went_wrong)
                }
            }
            .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }.create()
            .show()
    }

    private fun showToastMessage(@StringRes resId: Int) {
        Toast.makeText(
            this,
            getString(resId),
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        languageIdentifier.close()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
    }

    companion object {
        private const val TAG = "NotificationReader"
        private const val EXCEPTION_MESSAGE_EMPTY_TEXT = "Text is empty"
        private const val EXCEPTION_MESSAGE_LANGUAGE_MISSING =
            "Language is either not available or supported"
        private const val LANGUAGE_CODE_HINDI = "hi"
        const val CHANNEL_ID = "com.nikhiljain.notificationreader.notification_reader_service"
        const val SERVICE_ACTION = "SERVICE_ACTION"
        const val ACTION_START_SERVICE = "START_SERVICE"
        const val ACTION_STOP_SERVICE = "STOP_SERVICE"

        @JvmStatic
        fun startService(context: Context) {
            val intent = Intent(context, NotificationReaderService::class.java)
            intent.putExtra(SERVICE_ACTION, ACTION_START_SERVICE)
            ContextCompat.startForegroundService(context, intent)
        }

        @JvmStatic
        fun stopService(context: Context) {
            val intent = Intent(context, NotificationReaderService::class.java)
            intent.putExtra(SERVICE_ACTION, ACTION_STOP_SERVICE)
            ContextCompat.startForegroundService(context, intent)
        }
    }
}