package com.smartcontractai

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException

class LoginActivity : AppCompatActivity() {

    private lateinit var callbackManager: CallbackManager
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        callbackManager = CallbackManager.Factory.create()

        // Sự kiện khi bấm nút Đăng nhập Facebook
        // (Hoặc dùng Nút mặc định com.facebook.login.widget.LoginButton)
        LoginManager.getInstance().registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(result: LoginResult) {
                // Đăng nhập Facebook thành công -> Xác thực với Firebase
                handleFacebookAccessToken(result.accessToken.token)
            }

            override fun onCancel() {
                Toast.makeText(this@LoginActivity, "Đã hủy đăng nhập", Toast.LENGTH_SHORT).show()
            }

            override fun onError(error: FacebookException) {
                Toast.makeText(this@LoginActivity, "Lỗi: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })

        // Ví dụ gán sự kiện cho Custom Button
        // btnFacebookLogin.setOnClickListener {
        //     LoginManager.getInstance().logInWithReadPermissions(this, listOf("email", "public_profile"))
        // }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Nhận kết quả từ Facebook SDK
        callbackManager.onActivityResult(requestCode, resultCode, data)
    }

    private fun handleFacebookAccessToken(token: String) {
        val credential = FacebookAuthProvider.getCredential(token)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    Toast.makeText(this, "Đăng nhập thành công: ${user?.displayName}", Toast.LENGTH_SHORT).show()
                    // Chuyển hướng sang MainActivity
                } else {
                    val exception = task.exception
                    if (exception is FirebaseAuthUserCollisionException) {
                        Toast.makeText(this, "Email Facebook này đã được tạo tài khoản bằng Google hoặc Email/Password. Vui lòng chọn đăng nhập Google/Email hoặc bật 'Multiple accounts per email address' trong Firebase Console.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "Lỗi Firebase Auth: ${exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }
}
