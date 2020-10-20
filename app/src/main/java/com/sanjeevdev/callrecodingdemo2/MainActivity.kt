@file:Suppress("DEPRECATION")

package com.sanjeevdev.callrecodingdemo2

import android.Manifest
import android.app.ProgressDialog
import android.content.*
import android.content.pm.PackageManager
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.login_alert_dialog.*
import kotlinx.android.synthetic.main.login_alert_dialog.view.*
import kotlinx.android.synthetic.main.main_alert_dialog.*
import kotlinx.android.synthetic.main.main_alert_dialog.view.*
import kotlinx.android.synthetic.main.signin_alert_dialog.*
import kotlinx.android.synthetic.main.signin_alert_dialog.view.*
import kotlin.properties.Delegates


class MainActivity : AppCompatActivity(), RecordingListener {
    private val RECORD_AUDIO_CODE = 29
    private val REQUEST_CODE_BATTERY_OPTIMIZATION = 1
    private val REQUEST_READ_EXTERNAL_STORAGE = 100
    private val REQUEST_WRITE_EXTERNAL_STORAGE = 101
    private val REQUEST_CALL_LOG = 102
    private val REQUEST_PHONE_STATE = 103
    private val REQUEST_INTERNET = 104
    lateinit var phoneStateReceiver: PhoneStateReceiver
    var isSwitchOn by Delegates.notNull<Boolean>()

    lateinit var mainDialogBuilder: AlertDialog.Builder
    lateinit var mainDialogView: View
    lateinit var mainDialog: AlertDialog

    lateinit var loginDialogBuilder: AlertDialog.Builder
    lateinit var loginDialogView: View
    lateinit var loginDialog: AlertDialog

    lateinit var signinDialogBuilder: AlertDialog.Builder
    lateinit var signinDialogView: View
    lateinit var signinDialog: AlertDialog

    lateinit var userAuth: FirebaseAuth
    private lateinit var sharedPreferences: SharedPreferences

    lateinit var recordingAdaptar: RecordingAdaptar
    var list: List<RecordingModal> = ArrayList()
    lateinit var progressDialogLogin: ProgressDialog
    lateinit var progressDialogSignup: ProgressDialog


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        phoneStateReceiver = PhoneStateReceiver()
        initializeComponents()
        startMethod()
    }

    private fun initializeComponents() {

        progressDialogLogin = ProgressDialog(this)
        progressDialogLogin.setTitle("Login")
        progressDialogLogin.setMessage("Please wait...")

        progressDialogSignup = ProgressDialog(this)
        progressDialogSignup.setTitle("Sign Up")
        progressDialogSignup.setMessage("Please wait...")
        recordingAdaptar = RecordingAdaptar(list, this, this)
        userAuth = Firebase.auth

        mainDialogBuilder = AlertDialog.Builder(this, R.style.AlertDialogTheme)
        mainDialogView =
            LayoutInflater.from(this).inflate(R.layout.main_alert_dialog, mainDialogContainer)
        mainDialogBuilder.setView(mainDialogView)
        mainDialog = mainDialogBuilder.create()
        if (mainDialog.window != null) {
            mainDialog.window!!.setBackgroundDrawable(ColorDrawable(0))
        }

        loginDialogBuilder = AlertDialog.Builder(this, R.style.AlertDialogTheme)
        loginDialogView =
            LayoutInflater.from(this).inflate(R.layout.login_alert_dialog, loginDialogContainer)
        loginDialogBuilder.setView(loginDialogView)
        loginDialog = loginDialogBuilder.create()
        if (loginDialog.window != null) {
            loginDialog.window!!.setBackgroundDrawable(ColorDrawable(0))
        }

        signinDialogBuilder = AlertDialog.Builder(this, R.style.AlertDialogTheme)
        signinDialogView =
            LayoutInflater.from(this).inflate(R.layout.signin_alert_dialog, signinDialogContainer)
        signinDialogBuilder.setView(signinDialogView)
        signinDialog = signinDialogBuilder.create()
        if (signinDialog.window != null) {
            signinDialog.window!!.setBackgroundDrawable(ColorDrawable(0))
        }

    }

    private fun startMethod() {
        progressDialogLogin.hide()
        progressDialogSignup.hide()
        if (userAuth.currentUser == null) {
            recyclerViewRecordings.visibility = View.GONE
            warningLayout.visibility = View.VISIBLE
            noAccount.setOnClickListener {
                startMethod()
            }
            mainDialog.show()
            mainDialogView.loginButtonMain.setOnClickListener {
                loginDialog.show()
                mainDialog.dismiss()
                loginDialogView.CancelButtonLogin.setOnClickListener {
                    loginDialog.dismiss()
                    startMethod()
                }
                loginDialogView.loginButtonLogin.setOnClickListener {
                    loginDialog.dismiss()
                    loginFirebaseUser()
                }
            }
            mainDialogView.signinButtonMain.setOnClickListener {
                signinDialog.show()
                mainDialog.dismiss()
                signinDialogView.CancelButtonSignin.setOnClickListener {
                    signinDialog.dismiss()
                }
                signinDialogView.signinButtonSignin.setOnClickListener {
                    signinDialog.dismiss()
                    signFirebaseUser()
                }
            }
            return
        } else {
            mainDialog.dismiss()
            loginDialog.dismiss()
            signinDialog.dismiss()
            checkPermission()

        }
    }

    private fun loginFirebaseUser() {
        val email = loginDialogView.loginEmailLogin.text.toString().trim()
        val password = loginDialogView.loginPasswordLogin.text.toString().trim()
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(applicationContext, "Please fill entries", Toast.LENGTH_SHORT).show()
            return
        }

        progressDialogLogin.show()
        userAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    Toast.makeText(this, "Login successfully", Toast.LENGTH_SHORT).show()
                    loginDialog.dismiss()
                    progressDialogLogin.hide()
                    progressDialogSignup.hide()
                    checkPermission()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
                progressDialogLogin.hide()
                progressDialogSignup.hide()
                startMethod()
            }
    }

    private fun signFirebaseUser() {
        val email = signinDialogView.signinEmailSignin.text.toString().trim()
        val password = signinDialogView.signinPasswordSignin.text.toString().trim()
        val confirmPassword = signinDialogView.signinConfirmPasswordSignin.text.toString().trim()

        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(applicationContext, "Please fill entries", Toast.LENGTH_SHORT).show()
            return
        } else if (password != confirmPassword) {
            Toast.makeText(
                applicationContext,
                "Password and Confirm Password should be same",
                Toast.LENGTH_SHORT
            ).show()
            return
        } else {
            progressDialogSignup.show()
            userAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        Toast.makeText(this, "Sign up successfully", Toast.LENGTH_SHORT)
                            .show()
                        progressDialogSignup.hide()
                        progressDialogLogin.hide()
                        signinDialog.dismiss()
                        checkPermission()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, it.message, Toast.LENGTH_LONG).show()
                    Log.e("Error Creation", it.message.toString())
                    progressDialogSignup.hide()
                    progressDialogLogin.hide()
                    signinDialog.dismiss()
                    startMethod()
                }
        }
    }

    private fun loadRecordings() {

        recyclerViewRecordings.visibility = View.VISIBLE
        warningLayout.visibility = View.GONE
        sharedPreferences = getSharedPreferences(Constants.SHARED_PREF, Context.MODE_PRIVATE)
        isSwitchOn = sharedPreferences.getBoolean(Constants.IS_RECEIVER_ON, false)

        logout.setOnClickListener {
            userAuth.signOut()
            startMethod()
        }
        swipeRefreshLayout.setOnRefreshListener {
            loadRecordings()
        }

        if (isSwitchOn) {
            switchBar.setMinAndMaxProgress(0.5f, 1.0f)
            switchBar.playAnimation()
            getServiceState(isSwitchOn)
            isSwitchOn = false
        } else {
            switchBar.setMinAndMaxProgress(0.0f, 0.5f)
            switchBar.playAnimation()
            getServiceState(isSwitchOn)
            isSwitchOn = true
        }
        switchBar.speed = 3f
        switchBar.setOnClickListener {
            isSwitchOn = if (isSwitchOn) {
                switchBar.setMinAndMaxProgress(0.5f, 1.0f)
                switchBar.playAnimation()
                val editor = sharedPreferences.edit()
                editor.apply {
                    putBoolean(Constants.IS_RECEIVER_ON, true)
                }
                editor.apply()
                getServiceState(isSwitchOn)
                false
            } else {
                switchBar.setMinAndMaxProgress(0.0f, 0.5f)
                switchBar.playAnimation()
                val editor = sharedPreferences.edit()
                editor.apply {
                    putBoolean(Constants.IS_RECEIVER_ON, false)
                }
                editor.apply()
                getServiceState(isSwitchOn)
                true
            }
        }

        swipeRefreshLayout.isRefreshing = true
        recyclerViewRecordings.layoutManager = LinearLayoutManager(this)
        recyclerViewRecordings.adapter = recordingAdaptar

        val userID = userAuth.uid.toString()
        val database = FirebaseFirestore.getInstance()
        val recordingRef = database.collection(Constants.COLLECTION_NAME)
            .document(Constants.DOCUMENT_NAME)
            .collection(userID).get()
        recordingRef.addOnCompleteListener {
            if (it.isSuccessful) {
                list = it.result!!.toObjects(RecordingModal::class.java)
                recordingAdaptar.recoredItem = list
                recordingAdaptar.notifyDataSetChanged()
                swipeRefreshLayout.isRefreshing = false
            } else {
                Toast.makeText(this, "No Recording found", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            swipeRefreshLayout.isRefreshing = false
            Log.e("Error Loading Data", it.message.toString())
        }

    }

    private fun getServiceState(isSwitchOn: Boolean) {
        if (isSwitchOn) {
            val filter = IntentFilter()
            filter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
            filter.addAction("android.intent.action.NEW_OUTGOING_CALL")
            registerReceiver(phoneStateReceiver, filter)
        } else {
            try {
                unregisterReceiver(phoneStateReceiver)
            } catch (e: IllegalArgumentException) {
                Log.e("Argument Exception", "Receiver is not registered")
            }
        }
    }

    private fun checkPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                RECORD_AUDIO_CODE
            )
        } else if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                REQUEST_READ_EXTERNAL_STORAGE
            )
        } else if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_WRITE_EXTERNAL_STORAGE
            )
        } else if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CALL_LOG
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_CALL_LOG),
                REQUEST_CALL_LOG
            )
        } else if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_PHONE_STATE),
                REQUEST_PHONE_STATE
            )
        } else if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.INTERNET
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.INTERNET),
                REQUEST_INTERNET
            )
        } else {
            checkBatteryOptimization()
        }
    }

    private fun checkBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                val builder: AlertDialog.Builder = AlertDialog.Builder(this)
                builder.setTitle("Warning")
                builder.setMessage("Battery optimization is enabled. It can interrupt running background services.")
                builder.setPositiveButton(
                    "Disable",
                    DialogInterface.OnClickListener { dialogInterface, i ->
                        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        this@MainActivity.startActivityForResult(
                            intent,
                            REQUEST_CODE_BATTERY_OPTIMIZATION
                        )
                    })
                builder.setNegativeButton(
                    "Cancel",
                    DialogInterface.OnClickListener { dialogInterface, i ->
                        dialogInterface.dismiss()
                        Toast.makeText(
                            this,
                            "Please disable battery optimization",
                            Toast.LENGTH_SHORT
                        ).show()
                        checkBatteryOptimization()
                    })
                builder.create().show()
            } else {
                loadRecordings()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        if (requestCode == RECORD_AUDIO_CODE) {
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permission Failed", Toast.LENGTH_SHORT).show()
            }
            checkPermission()
        } else if (requestCode == REQUEST_READ_EXTERNAL_STORAGE) {
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permission Failed", Toast.LENGTH_SHORT).show()
            }
            checkPermission()
        } else if (requestCode == REQUEST_WRITE_EXTERNAL_STORAGE) {
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permission Failed", Toast.LENGTH_SHORT).show()
            }
            checkPermission()
        } else if (requestCode == REQUEST_CALL_LOG) {
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permission Failed", Toast.LENGTH_SHORT).show()
            }
            checkPermission()
        } else if (requestCode == REQUEST_PHONE_STATE) {
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT)
                    .show()
            } else {
                Toast.makeText(this, "Permission Failed", Toast.LENGTH_SHORT).show()
            }
            checkPermission()
        } else if (requestCode == REQUEST_INTERNET) {
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT)
                    .show()
            } else {
                Toast.makeText(this, "Permission Failed", Toast.LENGTH_SHORT)
                    .show()
            }
            checkPermission()
        } else if (requestCode == REQUEST_CODE_BATTERY_OPTIMIZATION) {
            loadRecordings()
        }
    }

    override fun initiateMediaPlayer(
        recordingModal: RecordingModal,
        holder: RecordingAdaptar.ViewHolder
    ) {
        val intent = Intent(android.content.Intent.ACTION_VIEW)
        intent.setDataAndType(Uri.parse(recordingModal.downloadLink), "audio/mp3")
        startActivity(intent)
    }
}