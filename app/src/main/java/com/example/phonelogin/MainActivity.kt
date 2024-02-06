package com.example.phonelogin

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private val auth = FirebaseAuth.getInstance()
    private var verificationId = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            VerifyPhoneNumber()
        }
    }
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun VerifyPhoneNumber() {
        val context = LocalContext.current
        var phoneNumber by remember { mutableStateOf("") }
        var transPhone by remember { mutableStateOf("") }
        var clickCertificationNumber by remember { mutableStateOf(false) }
        val leftTime = remember { mutableStateOf("") }
        var certificationNumber by remember { mutableStateOf("") }
        var checkCredentialClick by remember { mutableStateOf(false) }
        val result = remember { mutableStateOf(false) }
        Column(Modifier.padding(all = 10.dp)) {
            TextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Phone),
                label = { Text(text = "핸드폰 번호 입력")}
            )
            Button(onClick = {
                clickCertificationNumber = true
                val phoneNumberSubString = phoneNumber.substring(3)
                when(phoneNumber.substring(0, 3)) {
                    "010" -> transPhone = "+8210$phoneNumberSubString"
                    "011" -> transPhone = "+8211$phoneNumberSubString"
                    "016" -> transPhone = "+8216$phoneNumberSubString"
                    "017" -> transPhone = "+8217$phoneNumberSubString"
                    "018" -> transPhone = "+8218$phoneNumberSubString"
                    "019" -> transPhone = "+8219$phoneNumberSubString"
                    "106" -> transPhone = "+82106$phoneNumberSubString"
                }
                val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    override fun onVerificationCompleted(p0: PhoneAuthCredential) {
                        println("onVerificationCompleted $p0")
                    }

                    override fun onVerificationFailed(p0: FirebaseException) {
                        println("onVerificationFailed $p0")
                    }

                    override fun onCodeSent(p0: String, p1: PhoneAuthProvider.ForceResendingToken) {
                        super.onCodeSent(p0, p1)
                        this@MainActivity.verificationId = p0
                    }
                }
                val optionsCompat = PhoneAuthOptions.newBuilder(auth)
                    .setPhoneNumber(transPhone)
                    .setTimeout(60L, TimeUnit.SECONDS)
                    .setActivity(context as Activity)
                    .setCallbacks(callbacks)
                    .build()
                PhoneAuthProvider.verifyPhoneNumber(optionsCompat)
                auth.setLanguageCode("kr")
                timer(leftTime, result)
                clickCertificationNumber = true
            }) {
                Text(text = "인증 번호 발송")
            }
            if(clickCertificationNumber) {
                val text = if(leftTime.value == "00:00") "" else leftTime.value
                Text(text = text)
                Row {
                    TextField(
                        value = certificationNumber,
                        onValueChange = { certificationNumber = it },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                        label = { Text(text = "인증 번호 입력")}
                    )
                    Button(onClick = {
                        checkCredentialClick = true
                        val credential = PhoneAuthProvider.getCredential(verificationId, certificationNumber)
                        signInWithPhoneAuthCredential(credential, result) }) {
                        Text(text = "확인")
                    }
                }
            }
            if(checkCredentialClick) {
                AlertDialog(
                    onDismissRequest = { checkCredentialClick = false },
                    text = {
                        Text(
                            text = if(result.value) "인증 성공" else "인증 실패",
                            style = MaterialTheme.typography.labelMedium.copy(if(result.value) Color.Blue else Color.Red)
                        )
                    },
                    confirmButton = { Text(text = "확인", modifier = Modifier.clickable { checkCredentialClick = false }) },
                    dismissButton = { Text(text = "취소", modifier = Modifier.clickable { checkCredentialClick = false })},
                    containerColor = Color.White,
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                    ) 
            }
        }
    }

    private fun timer(leftTime: MutableState<String>, result: MutableState<Boolean>) {
        val timer = Timer()
        timer.scheduleAtFixedRate(object : TimerTask() {
            var seconds = 60
            override fun run() {
                seconds--
                when (seconds) {
                    60 -> {
                        leftTime.value = "01:00"
                    }
                    in 10..59 -> {
                        leftTime.value = "00:$seconds"
                    }
                    in 1 .. 9 -> {
                        leftTime.value = "00:0$seconds"
                    }
                    else -> {
                        leftTime.value = "인증 시간 초과 입니다. 다시 시도 해 주세요."
                        timer.cancel()
                    }
                }
                if(result.value) timer.cancel()
                println("leftTime.value : ${leftTime.value}")
            }
        }, 0, 1000)
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential, result: MutableState<Boolean>) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if(task.isSuccessful) {
                    println("인증 성공")
                    result.value = true
                } else {
                    println("인증 실패")
                    result.value = false
                }
            }
    }
}
