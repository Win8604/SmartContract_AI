@file:Suppress("Deprecation", "UnusedImport", "UNUSED_IMPORT")

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

import com.facebook.Profile
import com.facebook.AccessToken
import com.facebook.GraphRequest
import com.bumptech.glide.Glide
import org.json.JSONException
import com.smartcontractai.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var callbackManager: CallbackManager
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        callbackManager = CallbackManager.Factory.create()

        // Sự kiện khi bấm nút Đăng nhập Facebook
        // (Hoặc dùng Nút mặc định com.facebook.login.widget.LoginButton)
        LoginManager.getInstance().registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(result: LoginResult) {
                // Đăng nhập Facebook thành công -> Xác thực với Firebase
                handleFacebookAccessToken(result.accessToken.token)
                loadFacebookAvatar()
                fetchFacebookUserInfo(result.accessToken)
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

    // Cách 1: Dùng Profile.getCurrentProfile()
    fun loadFacebookAvatar() {
        val currentProfile = Profile.getCurrentProfile()
        if (currentProfile != null) {
            val avatarUri = currentProfile.getProfilePictureUri(500, 500)
            // Tải ảnh bằng Glide/Coil vào ImageView
            Glide.with(this)
                .load(avatarUri)
                .placeholder(R.drawable.ic_placeholder)
                .error(R.drawable.ic_error)
                .circleCrop()
                .into(binding.imgAvatar)
        }
    }

    // Cách 2: Lấy qua GraphRequest
    fun fetchFacebookUserInfo(accessToken: AccessToken) {
        val request = GraphRequest.newMeRequest(accessToken) { jsonObject, _ ->
            try {
                if (jsonObject != null) {
                    val userId = jsonObject.optString("id")
                    val name = jsonObject.optString("name")

                    // Bóc tách URL ảnh avatar từ JSON trả về
                    val pictureObj = jsonObject.optJSONObject("picture")
                    val dataObj = pictureObj?.optJSONObject("data")
                    val avatarUrl = dataObj?.optString("url")

                    // Hiển thị ảnh đại diện lên ImageView
                    if (!avatarUrl.isNullOrEmpty()) {
                        Glide.with(this@LoginActivity)
                            .load(avatarUrl)
                            .circleCrop()
                            .into(binding.imgAvatar)
                    }
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }

        val parameters = Bundle().apply {
            putString("fields", "id,name,email,picture.width(500).height(500)")
        }
        request.parameters = parameters
        request.executeAsync()
    }

    // Cách 3: Dùng URL Graph API trực tiếp qua userId
    fun loadFacebookAvatarDirectUrl() {
        val userId = AccessToken.getCurrentAccessToken()?.userId
        if (userId != null) {
            val avatarUrl = "https://graph.facebook.com/$userId/picture?type=large"
            Glide.with(this)
                .load(avatarUrl)
                .circleCrop()
                .into(binding.imgAvatar)
        }
    }

    @Deprecated("Deprecated in Java")
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
