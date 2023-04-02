package com.joeyseph.obdometer

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.ktx.Firebase

var auth: FirebaseAuth? = null

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val email: EditText = findViewById(R.id.email)
        val password: EditText = findViewById(R.id.password)
        val register: Button = findViewById(R.id.register)
        val login: Button = findViewById(R.id.login)

        auth = FirebaseAuth.getInstance()

        register.setOnClickListener {
            val txt_email = email.text.toString()
            val txt_password = password.text.toString()
            register(txt_email, txt_password)
        }
        login.setOnClickListener {
            val txt_email = email.text.toString()
            val txt_password = password.text.toString()
            login(txt_email, txt_password)
        }
    }

    fun register(email: String, password: String) {
        auth?.createUserWithEmailAndPassword(email, password)?.addOnCompleteListener(this) { task ->
            if(task.isSuccessful) {
                finish()
            } else {
                Toast.makeText(this, "Error creating account.", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun login(email: String, password: String) {
        auth?.signInWithEmailAndPassword(email, password)?.addOnCompleteListener(this) { task ->
            if(task.isSuccessful) {
                finish()
            } else {
                Toast.makeText(this, "Login failed.", Toast.LENGTH_LONG).show()
            }
        }
    }
}