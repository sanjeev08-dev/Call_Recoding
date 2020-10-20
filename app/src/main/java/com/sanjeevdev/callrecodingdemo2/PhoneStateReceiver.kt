package com.sanjeevdev.callrecodingdemo2

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.MediaRecorder
import android.net.Uri
import android.os.Environment
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.ktx.storage
import java.io.File
import java.io.IOException
import java.net.URI
import java.text.SimpleDateFormat
import java.util.*


@Suppress("DEPRECATION", "NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
open class PhoneStateReceiver : BroadcastReceiver() {

    private var lastState = TelephonyManager.CALL_STATE_IDLE
    private var callStartTime: Date? = null
    private var isIncoming = false
    private var savedNumber: String? = null
    private lateinit var recorder: MediaRecorder
    private var audioFile: File? = null
    private var recordStart = false
    private lateinit var fileName: String
    private lateinit var currentTime: String
    //private lateinit var sharedPreferences: SharedPreferences

    override fun onReceive(context: Context?, intent: Intent?) {

        Toast.makeText(context, "Hello Sanjeev", Toast.LENGTH_SHORT).show()
        /*sharedPreferences =
            context!!.getSharedPreferences(Constants.SHARED_PREF, Context.MODE_PRIVATE)*/
        if (intent?.action.equals("android.intent.action.NEW_OUTGOING_CALL")) {
            savedNumber = intent?.extras?.getString("android.intent.extra.PHONE_NUMBER")
        } else {
            val stateStr = intent?.extras?.getString(TelephonyManager.EXTRA_STATE)
            val number = intent?.extras?.getString(TelephonyManager.EXTRA_INCOMING_NUMBER)
            var state = 0
            when (stateStr) {
                TelephonyManager.EXTRA_STATE_IDLE -> {
                    state = TelephonyManager.CALL_STATE_IDLE
                }
                TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                    state = TelephonyManager.CALL_STATE_OFFHOOK
                }
                TelephonyManager.EXTRA_STATE_RINGING -> {
                    state = TelephonyManager.CALL_STATE_RINGING
                }
            }
            if (number != null) {
                Log.e("Number", number)
                onCallStateChanged(context, state, number)
            }
        }
    }

    //Derived classes should override these to respond to specific events of interest
    private fun onIncomingCallStarted(context: Context?) {
        Toast.makeText(context, "Incoming call", Toast.LENGTH_SHORT).show()
        Log.e("Call", "Incoming call started")
    }

    private fun onOutgoingCallStarted(context: Context?, number: String?) {
        Toast.makeText(context, "O S", Toast.LENGTH_SHORT).show()
        Log.e("Call", "recording started")

        val dir = File(
            Environment.getExternalStorageDirectory(),
            "/Android/data/com.sanjeevdev.callrecodingdemo2/Recordings"
        )
        if (!dir.exists()) {
            dir.mkdirs()
        }
        currentTime = SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date())
        fileName =
            number + "Record_" + SimpleDateFormat("ddMMyyyyHHmmss", Locale.US).format(Date())
        try {
            audioFile = File.createTempFile(fileName, ".amr", dir)
        } catch (e: IOException) {
            Log.e("Exception", e.message!!)
        }

        recorder = MediaRecorder()
        recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
        recorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB)
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
        recorder.setOutputFile(audioFile?.absolutePath)
        try {
            recorder.prepare()
        } catch (e: Exception) {
            Toast.makeText(context, "Recorder prepare error :" + e.message, Toast.LENGTH_SHORT)
                .show()
            Log.e("Prepare Exception", e.message!!)
        }
        recorder.start()
        recordStart = true
    }

    private fun onOutgoingCallEnded(context: Context?, number: String?) {
        Toast.makeText(context, "recording saved", Toast.LENGTH_SHORT).show()
        Log.e("Number", "Outgoing call end with$number")
        if (recordStart) {
            recorder.stop()
            recordStart = false
            uploadAudioToFirebase(number, context)
        }
    }

    private fun onCallStateChanged(context: Context?, state: Int, number: String) {

        Log.e("Number 2", number)
        lastState = state
        when (state) {
            TelephonyManager.CALL_STATE_RINGING -> {
                isIncoming = true
                callStartTime = Date()
                savedNumber = number
                onIncomingCallStarted(context)
            }
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                //Transition of ringing->offhook are pickups of incoming calls.  Nothing done on them
                if (lastState != TelephonyManager.CALL_STATE_RINGING) {
                    isIncoming = false
                    callStartTime = Date()
                    if (savedNumber == null) {
                        savedNumber = number
                    }
                    onOutgoingCallStarted(context, savedNumber)
                }
            }
            TelephonyManager.CALL_STATE_IDLE -> {
                //Went to idle-  this is the end of a call.  What type depends on previous state(s)
                onOutgoingCallEnded(context, savedNumber)
            }
        }
    }

    private fun uploadAudioToFirebase(number: String?, context: Context?) {
        Toast.makeText(context, "Process to firebase storage start", Toast.LENGTH_SHORT).show()
        val userID = FirebaseAuth.getInstance().currentUser!!.uid
        Log.e("Path", audioFile?.absolutePath.toString())
        Log.e("ID User", userID)
        Log.e("FileName", fileName)
        Log.e("Number H", number!!)
        val mStorageRef = Firebase.storage.reference
            .child("Recording")
            .child(userID)
        val file = Uri.fromFile(File(audioFile?.absolutePath))
       mStorageRef.child(fileName+".amr")
            .putFile(file)
            .addOnCompleteListener { it ->
                if (it.isSuccessful) {
                   var downloadUri: String
                    mStorageRef.child(fileName+".amr").downloadUrl.addOnSuccessListener { uri ->
                        downloadUri = uri.toString()
                        Toast.makeText(context, "F Storage Upload Success", Toast.LENGTH_SHORT)
                            .show()
                        uploadAudioDataToFirebaseFirestore(
                            "$fileName.amr",
                            downloadUri,
                            userID,
                            context
                        )
                    }.addOnFailureListener {
                            Log.e("Strorage Link",it.message.toString())
                        }
                }
            }
            .addOnFailureListener {it ->
                Log.e("F Storage Exception", it.message.toString())
                Toast.makeText(context, "F Storage Exception " + it.message, Toast.LENGTH_SHORT)
                    .show()
            }

    }


    private fun uploadAudioDataToFirebaseFirestore(
        fileName: String,
        downloadUri: String,
        userID: String,
        context: Context?
    ) {
        Log.e("File Name", fileName)
        Log.e("URL", downloadUri)

        val recording = RecordingModal(fileName, downloadUri, currentTime)
        val database: FirebaseFirestore = FirebaseFirestore.getInstance()
        database.collection(Constants.COLLECTION_NAME).document(Constants.DOCUMENT_NAME)
            .collection(userID).document(fileName).set(recording)
            .addOnSuccessListener {
                Toast.makeText(context, "F Storage upload success", Toast.LENGTH_LONG).show()
                Log.e("Data Insertion", "Success")
                val path = audioFile?.absolutePath
                deleteFile(path)
            }
            .addOnFailureListener {
                Toast.makeText(context, "F Storage upload success", Toast.LENGTH_LONG).show()
                Log.e("Data Insertion", "Failed " + it.message)
            }
    }

    private fun deleteFile(path: String?) {
        val file = File(path)
        if (file.exists()) {
            if (file.delete()) {
                Log.e("Delete file", "Success $path")
            } else {
                Log.e("File Delete", "Failed")
            }
        }
    }

}