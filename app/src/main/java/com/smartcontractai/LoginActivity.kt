@file:Suppress("Deprecation", "UnusedImport", "UNUSED_IMPORT")

package com.smartcontractai

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.GraphRequest
import com.facebook.login.LoginResult
import com.facebook.login.widget.LoginButton
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.smartcontractai.databinding.ActivityLoginBinding
import org.json.JSONException

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var callbackManager: CallbackManager
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        val ivAvatar = binding.ivAvatar
        val btnFbLogin = binding.btnFbLogin

        callbackManager = CallbackManager.Factory.create()
        btnFbLogin.setPermissions("public_profile", "email")

        btnFbLogin.registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(result: LoginResult) {
                loadFacebookAvatar(result.accessToken, ivAvatar)
                handleFacebookAccessToken(result.accessToken.token)
                fetchFacebookUserInfo(result.accessToken)
            }

            override fun onCancel() {
                Toast.makeText(this@LoginActivity, "Đã hủy đăng nhập", Toast.LENGTH_SHORT).show()
            }

            override fun onError(error: FacebookException) {
                Toast.makeText(this@LoginActivity, error.message ?: "Lỗi Facebook Login", Toast.LENGTH_SHORT).show()
            }
        })

        // Nếu user đã đăng nhập từ trước, tự hiện avatar luôn
        AccessToken.getCurrentAccessToken()?.let { token ->
            if (!token.isExpired) {
                loadFacebookAvatar(token, ivAvatar)
                loadFacebookAvatarDirectUrl()
            }
        }
    }

    private fun loadFacebookAvatar(accessToken: AccessToken, imageView: ImageView) {
        val request = GraphRequest.newMeRequest(accessToken) { obj, _ ->
            val id = obj?.optString("id") ?: return@newMeRequest
            val avatarUrl = "https://graph.facebook.com/$id/picture?type=large&width=200&height=200"

            Glide.with(this@LoginActivity)
                .load(avatarUrl)
                .circleCrop()
                .placeholder(R.drawable.ic_avatar_placeholder)
                .error(R.drawable.ic_error)
                .into(imageView)
        }
        request.executeAsync()
    }

    // Cách xin field picture trực tiếp trong Graph API Request
    fun fetchFacebookUserInfo(accessToken: AccessToken) {
        val request = GraphRequest.newMeRequest(accessToken) { jsonObject, _ ->
            try {
                if (jsonObject != null) {
                    val userId = jsonObject.optString("id")
                    val name = jsonObject.optString("name")
                    Log.d("FacebookLogin", "Fetched Facebook User: $name ($userId)")

                    // Bóc tách URL ảnh avatar từ JSON trả về
                    val pictureObj = jsonObject.optJSONObject("picture")
                    val dataObj = pictureObj?.optJSONObject("data")
                    val rawUrl = dataObj?.optString("url")
                    val avatarUrl = if (!rawUrl.isNullOrEmpty()) rawUrl else if (userId.isNotEmpty()) "https://graph.facebook.com/$userId/picture?type=large" else null

                    // Hiển thị ảnh đại diện lên ImageView
                    if (!avatarUrl.isNullOrEmpty()) {
                        Glide.with(this@LoginActivity)
                            .load(avatarUrl)
                            .circleCrop()
                            .placeholder(R.drawable.ic_avatar_placeholder)
                            .into(binding.ivAvatar)
                    }
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }

        val parameters = Bundle().apply {
            putString("fields", "id,name,email,picture.type(large)")
        }
        request.parameters = parameters
        request.executeAsync()
    }

    // Lấy URL Graph API trực tiếp từ userId
    @Suppress("unused")
    fun loadFacebookAvatarDirectUrl() {
        val userId = AccessToken.getCurrentAccessToken()?.userId
        if (userId != null) {
            val avatarUrl = "https://graph.facebook.com/$userId/picture?type=large"
            Glide.with(this)
                .load(avatarUrl)
                .circleCrop()
                .placeholder(R.drawable.ic_avatar_placeholder)
                .into(binding.ivAvatar)
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

                    // Lấy ID Token để đồng bộ với PostgreSQL Backend
                    user?.getIdToken(true)?.addOnCompleteListener { tokenTask ->
                        if (tokenTask.isSuccessful) {
                            val idToken = tokenTask.result?.token
                            if (idToken != null) {
                                com.smartcontractai.network.ApiClient.syncUserWithBackend(
                                    idToken = idToken,
                                    fullName = user.displayName,
                                    provider = "facebook"
                                ) { success, responseMessage ->
                                    runOnUiThread {
                                        if (success) {
                                            Toast.makeText(this, "Đã đồng bộ User vào Database!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Log.e("BackendSync", "Lỗi đồng bộ: $responseMessage")
                                        }
                                    }
                                }
                            }
                        }
                    }
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
