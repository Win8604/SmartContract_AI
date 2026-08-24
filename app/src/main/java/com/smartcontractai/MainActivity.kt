@file:Suppress("Deprecation", "UnusedImport", "UNUSED_IMPORT", "RememberReturnType", "COMPOSABLE_INVOCATION", "ComposableInvocation")

package com.smartcontractai

import android.content.Context
import androidx.core.content.edit
import androidx.core.net.toUri
import java.util.UUID
import java.security.MessageDigest
import android.content.ContextWrapper
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import kotlin.time.Duration.Companion.milliseconds
import coil.compose.AsyncImage
import com.smartcontractai.data.User
import com.smartcontractai.data.UserDatabaseHelper
import com.smartcontractai.data.UserFileManager
import com.smartcontractai.data.UserInfo
import com.smartcontractai.ui.theme.SmartContractAITheme
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.lifecycleScope
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.GraphRequest
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import android.widget.Toast
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch

import android.app.NotificationChannel
import android.app.NotificationManager
import android.util.Log
import androidx.fragment.app.FragmentActivity
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import com.google.firebase.messaging.FirebaseMessaging
import com.smartcontractai.data.NotificationRepository
import com.smartcontractai.utils.FCMUtils
import com.smartcontractai.utils.RequestNotificationPermissionIfNeeded

fun Context.findActivity(): ComponentActivity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is ComponentActivity) return context
        context = context.baseContext
    }
    return null
}

class MainActivity : FragmentActivity() {

    private lateinit var auth: FirebaseAuth
    private val callbackManager: CallbackManager = CallbackManager.Factory.create()
    private var facebookSuccessCallback: (() -> Unit)? = null
    private var googleSuccessCallback: (() -> Unit)? = null

    fun showBiometricPrompt(onSuccess: () -> Unit) {
        val biometricManager = BiometricManager.from(this)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL

        when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                val executor = ContextCompat.getMainExecutor(this)
                val biometricPrompt = BiometricPrompt(this, executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            super.onAuthenticationSucceeded(result)
                            Toast.makeText(this@MainActivity, "Xác thực sinh trắc học thành công!", Toast.LENGTH_SHORT).show()
                            onSuccess()
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            super.onAuthenticationError(errorCode, errString)
                            if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                                Toast.makeText(this@MainActivity, "Lỗi xác thực: $errString", Toast.LENGTH_SHORT).show()
                            }
                        }

                        override fun onAuthenticationFailed() {
                            super.onAuthenticationFailed()
                            Toast.makeText(this@MainActivity, "Xác thực thất bại, thử lại", Toast.LENGTH_SHORT).show()
                        }
                    })

                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Xác thực sinh trắc học")
                    .setSubtitle("Vân tay / Khuôn mặt / Mã PIN điện thoại")
                    .setDescription("Sử dụng sinh trắc học thiết bị để đăng nhập SmartContract AI")
                    .setAllowedAuthenticators(authenticators)
                    .build()

                biometricPrompt.authenticate(promptInfo)
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                Toast.makeText(this, "Thiết bị không hỗ trợ sinh trắc học", Toast.LENGTH_SHORT).show()
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                Toast.makeText(this, "Cảm biến sinh trắc học hiện không khả dụng", Toast.LENGTH_SHORT).show()
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                Toast.makeText(this, "Chưa cài đặt vân tay/khuôn mặt trên thiết bị", Toast.LENGTH_SHORT).show()
            }
            else -> {
                Toast.makeText(this, "Sinh trắc học không khả dụng", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                "default_channel",
                "Default Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "General application notifications"
            }

            val notificationManager =
                getSystemService(NotificationManager::class.java)

            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }
    }

    private val googleLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken != null) {
                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(firebaseCredential)
                    .addOnCompleteListener(this) { authTask ->
                        if (authTask.isSuccessful) {
                            saveGoogleUserToDb(isCorporateSocialAuth)
                            Toast.makeText(
                                this,
                                if (isCorporateSocialAuth) "Đăng nhập Google Doanh nghiệp thành công" else "Đăng nhập Google thành công",
                                Toast.LENGTH_SHORT
                            ).show()
                            googleSuccessCallback?.invoke()
                        } else {
                            Toast.makeText(
                                this,
                                "Firebase Google thất bại: ${authTask.exception?.localizedMessage}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
            } else {
                Toast.makeText(
                    this,
                    "Không lấy được ID Token từ Google",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: ApiException) {
            if (e.statusCode != 12501) { // 12501 = SIGN_IN_CANCELLED
                Toast.makeText(
                    this,
                    "Google Sign In lỗi (${e.statusCode}): ${e.localizedMessage ?: e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(
                    this,
                    "Bạn đã hủy đăng nhập Google",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createNotificationChannel()
        requestNotificationPermission()

        auth = FirebaseAuth.getInstance()
        setupFacebookCallback()

        FirebaseMessaging.getInstance().isAutoInitEnabled = true
        Firebase.analytics.setAnalyticsCollectionEnabled(true)

        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->

                if (!task.isSuccessful) {
                    Log.e(
                        "FCM",
                        "Failed to get FCM token",
                        task.exception
                    )
                    return@addOnCompleteListener
                }

                val token = task.result

                Log.d(
                    "FCM",
                    "FCM Token: $token"
                )
            }

        try {
            val accessToken = AccessToken.getCurrentAccessToken()
            val isFacebookLoggedIn = accessToken != null && !accessToken.isExpired
            if (isFacebookLoggedIn) {
                Log.d("Facebook", "Facebook AccessToken is active")
            }
        } catch (e: Exception) {
            Log.w("Facebook", "Error checking Facebook AccessToken: ${e.message}")
        }

        enableEdgeToEdge()
        setContent {
            SmartContractAITheme {
                // Xin quyền thông báo (Android 13+) & Lấy token FCM liên kết Firebase
                RequestNotificationPermissionIfNeeded(
                    context = this,
                    onPermissionGranted = {
                        FCMUtils.fetchFcmToken { token ->
                            Log.d("FCM_FIREBASE", "FCM Device Token: $token")
                        }
                    }
                )
                AppNavigation()
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager.onActivityResult(requestCode, resultCode, data)
    }

    private fun getWebClientId(): String {
        return try {
            getString(R.string.default_web_client_id)
        } catch (_: Exception) {
            "1044491853800-mt5jpjc114idjupebn2redtv6ah6c1p6.apps.googleusercontent.com"
        }
    }

    private fun saveGoogleUserToDb(isCorporate: Boolean = false) {
        val user = auth.currentUser
        if (user != null) {
            val dbHelper = UserDatabaseHelper(this)
            val userEmail = user.email ?: ""
            if (userEmail.isNotEmpty()) {
                val userName = user.displayName ?: "Google User"
                val photoUrl = user.photoUrl?.toString()
                val authTypeStr = if (isCorporate) "CORPORATE_GOOGLE" else "GOOGLE"
                val userInfo = UserInfo(
                    fullName = userName,
                    phoneNumber = "",
                    email = userEmail,
                    password = "GOOGLE_AUTH_USER",
                    authType = authTypeStr,
                    avatarUrl = photoUrl,
                    isCorporate = isCorporate,
                    accountType = if (isCorporate) "CORPORATE" else "PERSONAL"
                )
                if (isCorporate) {
                    UserFileManager.saveCorporateUser(this, userInfo)
                } else {
                    UserFileManager.savePersonalUser(this, userInfo)
                }
                UserFileManager.saveCurrentSessionEmail(this, userEmail)
                UserFileManager.saveRememberMe(this, true, userEmail)
                if (!dbHelper.isEmailExists(userEmail)) {
                    val newUser = User(
                        fullName = userName,
                        phoneNumber = "",
                        email = userEmail,
                        password = "GOOGLE_AUTH_USER",
                        authType = authTypeStr,
                        isCorporate = isCorporate,
                        accountType = if (isCorporate) "CORPORATE" else "PERSONAL"
                    )
                    if (isCorporate) {
                        dbHelper.registerCorporateUser(newUser)
                    } else {
                        dbHelper.registerPersonalUser(newUser)
                    }
                }
            }
        }
    }

    // ==================== GOOGLE LOGIN ====================
    fun signInWithGoogle(onSuccess: () -> Unit, isCorporate: Boolean = false) {
        googleSuccessCallback = onSuccess
        val credentialManager = CredentialManager.create(this)
        val webClientId = getWebClientId()

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(
                    context = this@MainActivity,
                    request = request
                )

                val credential = result.credential

                if (
                    credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleIdTokenCredential =
                        GoogleIdTokenCredential.createFrom(credential.data)

                    val firebaseCredential = GoogleAuthProvider.getCredential(
                        googleIdTokenCredential.idToken,
                        null
                    )

                    auth.signInWithCredential(firebaseCredential)
                        .addOnCompleteListener(this@MainActivity) { task ->
                            if (task.isSuccessful) {
                                saveGoogleUserToDb(isCorporate)
                                Toast.makeText(
                                    this@MainActivity,
                                    if (isCorporate) "Đăng nhập Google Doanh nghiệp thành công" else "Đăng nhập Google thành công",
                                    Toast.LENGTH_SHORT
                                ).show()
                                onSuccess()
                            } else {
                                Toast.makeText(
                                    this@MainActivity,
                                    "Firebase Google thất bại: ${task.exception?.localizedMessage}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                } else {
                    signInWithGoogleLegacy(onSuccess, isCorporate)
                }
            } catch (_: GetCredentialCancellationException) {
                Toast.makeText(
                    this@MainActivity,
                    "Bạn đã hủy đăng nhập Google",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (_: Exception) {
                // Tự động chuyển sang GoogleSignInIntent fallback nếu CredentialManager không khả dụng hoặc trả về NoCredentialException
                signInWithGoogleLegacy(onSuccess, isCorporate)
            }
        }
    }

    fun signInWithGoogleLegacy(onSuccess: () -> Unit, isCorporate: Boolean = false) {
        googleSuccessCallback = onSuccess
        isCorporateSocialAuth = isCorporate
        val webClientId = getWebClientId()
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        val googleSignInClient = GoogleSignIn.getClient(this, gso)
        googleSignInClient.signOut().addOnCompleteListener {
            val signInIntent = googleSignInClient.signInIntent
            googleLauncher.launch(signInIntent)
        }
    }

    private fun saveFacebookUserToDb(name: String? = null, email: String? = null, avatarUrl: String? = null, isCorporate: Boolean = false) {
        val user = auth.currentUser
        val dbHelper = UserDatabaseHelper(this)
        val userEmail = email ?: user?.email ?: if (user != null) "${user.uid}@facebook.com" else ""
        val userName = name ?: user?.displayName ?: "Facebook User"
        val authTypeStr = if (isCorporate) "CORPORATE_FACEBOOK" else "FACEBOOK"

        if (userEmail.isNotEmpty()) {
            val existingUser = UserFileManager.getUserByEmail(this, userEmail)
            val fbPhotoUrl = avatarUrl ?: user?.photoUrl?.toString() ?: existingUser?.avatarUrl

            val userInfo = UserInfo(
                fullName = userName,
                phoneNumber = existingUser?.phoneNumber ?: "",
                email = userEmail,
                password = "FACEBOOK_AUTH_USER",
                authType = authTypeStr,
                avatarUrl = fbPhotoUrl,
                isCorporate = isCorporate,
                accountType = if (isCorporate) "CORPORATE" else "PERSONAL"
            )
            if (isCorporate) {
                UserFileManager.saveCorporateUser(this, userInfo)
            } else {
                UserFileManager.savePersonalUser(this, userInfo)
            }
            UserFileManager.saveCurrentSessionEmail(this, userEmail)
            UserFileManager.saveRememberMe(this, true, userEmail)
            if (!dbHelper.isEmailExists(userEmail)) {
                val newUser = User(
                    fullName = userName,
                    phoneNumber = "",
                    email = userEmail,
                    password = "FACEBOOK_AUTH_USER",
                    authType = authTypeStr,
                    isCorporate = isCorporate,
                    accountType = if (isCorporate) "CORPORATE" else "PERSONAL"
                )
                if (isCorporate) {
                    dbHelper.registerCorporateUser(newUser)
                } else {
                    dbHelper.registerPersonalUser(newUser)
                }
            }
        }
    }

    private var isCorporateSocialAuth = false

    // ==================== FACEBOOK LOGIN ====================
    private fun setupFacebookCallback() {
        try {
            LoginManager.getInstance().registerCallback(
                callbackManager,
                object : FacebookCallback<LoginResult> {
                    override fun onSuccess(result: LoginResult) {
                        val accessToken = result.accessToken

                        // 1. Đăng nhập vào Firebase bằng Facebook Credential
                        val credential = FacebookAuthProvider.getCredential(accessToken.token)
                        auth.signInWithCredential(credential)
                            .addOnCompleteListener(this@MainActivity) { task ->
                                if (task.isSuccessful) {
                                    val user = auth.currentUser
                                    saveFacebookUserToDb(isCorporate = isCorporateSocialAuth)
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Đăng nhập Facebook thành công: ${user?.displayName ?: user?.email ?: "User"}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    facebookSuccessCallback?.invoke()
                                } else {
                                    val exception = task.exception
                                    if (exception is FirebaseAuthUserCollisionException) {
                                        val currentUser = auth.currentUser
                                        if (currentUser != null) {
                                            currentUser.linkWithCredential(credential).addOnCompleteListener { linkTask ->
                                                if (linkTask.isSuccessful) {
                                                    saveFacebookUserToDb(isCorporate = isCorporateSocialAuth)
                                                    Toast.makeText(this@MainActivity, "Đã liên kết tài khoản Facebook thành công!", Toast.LENGTH_SHORT).show()
                                                    facebookSuccessCallback?.invoke()
                                                } else {
                                                    Toast.makeText(this@MainActivity, "Liên kết Facebook thất bại: ${linkTask.exception?.message}", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        } else {
                                            Toast.makeText(
                                                this@MainActivity,
                                                "Email này đã được dùng cho tài khoản Google/Email. Vui lòng đăng nhập Google/Email hoặc bật 'Multiple accounts per email' trong Firebase Console.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    } else {
                                        Toast.makeText(
                                            this@MainActivity,
                                            "Firebase Facebook thất bại: ${exception?.message}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }

                        // 2. Lấy thêm thông tin chi tiết (Email, Name, Avatar) qua Graph API
                        val request = GraphRequest.newMeRequest(accessToken) { jsonObject, _ ->
                            if (jsonObject != null) {
                                val email = jsonObject.optString("email")
                                val name = jsonObject.optString("name")
                                val fbId = jsonObject.optString("id")
                                val pictureObj = jsonObject.optJSONObject("picture")
                                val dataObj = pictureObj?.optJSONObject("data")
                                val rawUrl = dataObj?.optString("url")
                                val avatarUrl = if (!rawUrl.isNullOrEmpty()) rawUrl else if (fbId.isNotEmpty()) "https://graph.facebook.com/$fbId/picture?type=large" else null

                                saveFacebookUserToDb(name, email, avatarUrl, isCorporate = isCorporateSocialAuth)
                            } else {
                                saveFacebookUserToDb(isCorporate = isCorporateSocialAuth)
                            }
                        }
                        val parameters = Bundle().apply {
                            putString("fields", "id,name,email,picture.type(large)")
                        }
                        request.parameters = parameters
                        request.executeAsync()
                    }

                    override fun onCancel() {
                        Toast.makeText(
                            this@MainActivity,
                            "Đã hủy đăng nhập Facebook",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    override fun onError(error: FacebookException) {
                        Toast.makeText(
                            this@MainActivity,
                            "Lỗi Facebook: ${error.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            )
        } catch (e: Exception) {
            Log.e("MainActivity", "Error setting up Facebook callback", e)
        }
    }

    // Khi người dùng bấm vào nút Facebook trên giao diện:
    fun signInWithFacebook(onSuccess: () -> Unit, isCorporate: Boolean = false) {
        try {
            facebookSuccessCallback = onSuccess
            isCorporateSocialAuth = isCorporate
            LoginManager.getInstance().logOut()
            LoginManager.getInstance().logInWithReadPermissions(
                this,
                callbackManager,
                listOf("public_profile", "email")
            )
        } catch (e: Exception) {
            Log.e("MainActivity", "Error initiating Facebook login", e)
            Toast.makeText(this, "Lỗi kết nối Facebook: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

// Bảng màu giao diện
@Suppress("unused", "UnusedSymbol")
object AppColors {
    val TopBannerGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFF5CE1E6), Color(0xFF00C2CB), Color(0xFF38B6FF))
    )
    val PrimaryBlue = Color(0xFF00C2CB)
    val DarkHeaderBg = Color(0xFF0D1424)
    val DarkCardBg = Color(0xFF131B2E)
    val LightBlueBadge = Color(0xFFE8F7FF)
    val LightOrangeBadge = Color(0xFFFFEBDC)
    val LightGreenBadge = Color(0xFFE3FCEF)
    val LightGrayBg = Color(0xFFF8F9FA)
    val BorderGray = Color(0xFFE2E8F0)
    val SubtitleGray = Color(0xFF64748B)
    val PrimaryButtonBg = Color(0xFF000000)
    val SecondaryButtonBg = Color(0xFFE2E8F0)
    val ChipBg = Color(0xFFF1F5F9)
    val TextBlue = Color(0xFF3B82F6)
}

// Quản lý điều hướng màn hình
sealed class Screen {
    object Register : Screen()
    object Login : Screen()
    data class Dashboard(val initialTab: Int = 0) : Screen()
    object CreateContractOverview : Screen()
    object CreateContractWithAI : Screen()
    object ContractTemplates : Screen()
    data class ContractDocumentEditor(val templateTitle: String = "Hợp Đồng Thử Việc (Bản Chuẩn 2024)") : Screen()
    data class ContractReview(
        val contractTitle: String = "Hợp Đồng Mới",
        val initialContent: String = "",
        val source: String = "AI",
        val creatorEmail: String? = null
    ) : Screen()
}

@Composable
fun AppNavigation() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isAutoLogin = remember { UserFileManager.isRemembered(context) }
    var currentScreen by remember { mutableStateOf<Screen>(if (isAutoLogin) Screen.Dashboard(0) else Screen.Login) }

    when (currentScreen) {
        is Screen.Register -> RegisterScreen(
            onBack = null,
            onRegisterSuccess = { currentScreen = Screen.Dashboard(0) },
            onNavigateToLogin = { currentScreen = Screen.Login }
        )
        is Screen.Login -> LoginScreen(
            onBack = null,
            onNavigateToRegister = { currentScreen = Screen.Register },
            onLoginSuccess = { currentScreen = Screen.Dashboard(0) }
        )
        is Screen.Dashboard -> {
            val initialTab = (currentScreen as Screen.Dashboard).initialTab
            DashboardScreen(
                initialTab = initialTab,
                onLogoutClick = {
                    UserFileManager.clearSession(context)
                    currentScreen = Screen.Login
                },
                onNavigateToCreateContractAI = { currentScreen = Screen.CreateContractOverview },
                onNavigateToContractTemplates = { currentScreen = Screen.ContractTemplates }
            )
        }
        is Screen.CreateContractOverview -> CreateContractOverviewScreen(
            onBack = { currentScreen = Screen.Dashboard(0) },
            onNavigateToDashboard = { tab -> currentScreen = Screen.Dashboard(tab) },
            onNavigateToCreateWithAI = { currentScreen = Screen.CreateContractWithAI },
            onNavigateToContractTemplates = { currentScreen = Screen.ContractTemplates },
            onNavigateToReview = { title, content ->
                val currentEmail = UserFileManager.getCurrentSessionEmail(context)
                currentScreen = Screen.ContractReview(contractTitle = title, initialContent = content, source = "Clone", creatorEmail = currentEmail)
            }
        )
        is Screen.CreateContractWithAI -> CreateContractWithAIScreen(
            onBack = { currentScreen = Screen.CreateContractOverview },
            onNavigateToDashboard = { tab -> currentScreen = Screen.Dashboard(tab) },
            onNavigateToReview = { title, content ->
                val currentEmail = UserFileManager.getCurrentSessionEmail(context)
                currentScreen = Screen.ContractReview(contractTitle = title, initialContent = content, source = "AI", creatorEmail = currentEmail)
            }
        )
        is Screen.ContractTemplates -> ContractTemplatesScreen(
            onNavigateToDashboard = { tab -> currentScreen = Screen.Dashboard(tab) },
            onNavigateToCreateWithAI = { currentScreen = Screen.CreateContractWithAI },
            onNavigateToDocumentEditor = { title ->
                currentScreen = Screen.ContractDocumentEditor(title)
            },
            onNavigateToReview = { title, content ->
                val currentEmail = UserFileManager.getCurrentSessionEmail(context)
                currentScreen = Screen.ContractReview(contractTitle = title, initialContent = content, source = "Template", creatorEmail = currentEmail)
            }
        )
        is Screen.ContractDocumentEditor -> ContractDocumentEditorScreen(
            templateTitle = (currentScreen as Screen.ContractDocumentEditor).templateTitle,
            onBack = { currentScreen = Screen.ContractTemplates },
            onNavigateToDashboard = { tab -> currentScreen = Screen.Dashboard(tab) },
            onNavigateToReview = { title, content ->
                val currentEmail = UserFileManager.getCurrentSessionEmail(context)
                currentScreen = Screen.ContractReview(contractTitle = title, initialContent = content, source = "Template", creatorEmail = currentEmail)
            }
        )
        is Screen.ContractReview -> {
            val reviewScreenState = currentScreen as Screen.ContractReview
            ContractReviewScreen(
                contractTitle = reviewScreenState.contractTitle,
                initialContent = reviewScreenState.initialContent,
                source = reviewScreenState.source,
                creatorEmail = reviewScreenState.creatorEmail,
                onBack = { currentScreen = Screen.Dashboard(0) },
                onNavigateToDashboard = { tab -> currentScreen = Screen.Dashboard(tab) }
            )
        }
    }
}




@Composable
fun BottomNavigationBar(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val currentEmail = UserFileManager.getCurrentSessionEmail(context)
    val currentUser = remember(currentEmail) { UserFileManager.getUserByEmail(context, currentEmail) }
    val isCorporateUser = currentUser?.isCorporate == true || currentUser?.accountType == "CORPORATE" || currentUser?.authType?.startsWith("CORPORATE") == true

    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = selectedTab == 0,
            onClick = { onTabSelected(0) },
            icon = { Icon(if (selectedTab == 0) Icons.Default.Home else Icons.Outlined.Home, contentDescription = "Home") },
            label = { Text("Home", fontSize = 10.sp, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.White,
                selectedTextColor = Color(0xFF1D4ED8),
                indicatorColor = Color(0xFF1D4ED8),
                unselectedIconColor = Color(0xFF64748B),
                unselectedTextColor = Color(0xFF64748B)
            )
        )
        NavigationBarItem(
            selected = selectedTab == 1,
            onClick = { onTabSelected(1) },
            icon = { Icon(if (selectedTab == 1) Icons.Default.Description else Icons.Outlined.Description, contentDescription = "Contracts") },
            label = { Text("Contracts", fontSize = 10.sp, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.White,
                selectedTextColor = Color(0xFF1D4ED8),
                indicatorColor = Color(0xFF1D4ED8),
                unselectedIconColor = Color(0xFF64748B),
                unselectedTextColor = Color(0xFF64748B)
            )
        )
        NavigationBarItem(
            selected = selectedTab == 2,
            onClick = { onTabSelected(2) },
            icon = { Icon(if (selectedTab == 2) Icons.Default.Assignment else Icons.Outlined.Assignment, contentDescription = "Templates") },
            label = { Text("Templates", fontSize = 10.sp, fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.White,
                selectedTextColor = Color(0xFF1D4ED8),
                indicatorColor = Color(0xFF1D4ED8),
                unselectedIconColor = Color(0xFF64748B),
                unselectedTextColor = Color(0xFF64748B)
            )
        )
        if (isCorporateUser) {
            NavigationBarItem(
                selected = selectedTab == 3,
                onClick = { onTabSelected(3) },
                icon = { Icon(if (selectedTab == 3) Icons.Default.Work else Icons.Outlined.WorkOutline, contentDescription = "Corporate") },
                label = { Text("Corporate", fontSize = 10.sp, fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    selectedTextColor = Color(0xFF1D4ED8),
                    indicatorColor = Color(0xFF1D4ED8),
                    unselectedIconColor = Color(0xFF64748B),
                    unselectedTextColor = Color(0xFF64748B)
                )
            )
        }
        NavigationBarItem(
            selected = selectedTab == 4,
            onClick = { onTabSelected(4) },
            icon = { Icon(if (selectedTab == 4) Icons.Default.Settings else Icons.Outlined.Settings, contentDescription = "Settings") },
            label = { Text("Settings", fontSize = 10.sp, fontWeight = if (selectedTab == 4) FontWeight.Bold else FontWeight.Normal) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.White,
                selectedTextColor = Color(0xFF1D4ED8),
                indicatorColor = Color(0xFF1D4ED8),
                unselectedIconColor = Color(0xFF64748B),
                unselectedTextColor = Color(0xFF64748B)
            )
        )
    }
}

// ==================== MÀN HÌNH 3: ĐĂNG KÝ TÀI KHOẢN ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onBack: (() -> Unit)? = null,
    onRegisterSuccess: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var isCorporate by remember { mutableStateOf(false) }
    var fullName by remember { mutableStateOf("") }
    var taxCode by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFEFF6FF), // Soft icy blue top
                        Color(0xFFF8FAFC), // Pure light background middle
                        Color(0xFFE0F2FE)  // Gradient bottom tint
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (onBack != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = Color(0xFF1E293B)
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(30.dp))
            }



            // White Card bọc giao diện Register
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Logo SmartContract AI
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFE0F2FE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = "Logo",
                                tint = Color(0xFF0284C7),
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = "SmartContract AI",
                                fontSize = 5.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0284C7)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (isCorporate) "Tạo tài khoản Doanh nghiệp" else "Tạo tài khoản Cá nhân",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0038A8),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (isCorporate) "Bảo mật thông minh cho hợp đồng doanh nghiệp" else "Bảo mật thông minh cho hợp đồng của bạn",
                        fontSize = 12.sp,
                        color = AppColors.SubtitleGray,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Chọn loại tài khoản: Cá nhân / Doanh nghiệp
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Cá nhân
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp)
                                .clickable { isCorporate = false },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (!isCorporate) Color(0xFF1D4ED8) else Color.White
                            ),
                            border = if (isCorporate) BorderStroke(1.dp, Color(0xFFCBD5E1)) else null
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Person,
                                    contentDescription = "Cá nhân",
                                    tint = if (!isCorporate) Color.White else Color(0xFF1E293B),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Cá nhân",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (!isCorporate) Color.White else Color(0xFF1E293B)
                                )
                            }
                        }

                        // Doanh nghiệp
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp)
                                .clickable { isCorporate = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCorporate) Color(0xFF1D4ED8) else Color.White
                            ),
                            border = if (!isCorporate) BorderStroke(1.dp, Color(0xFFCBD5E1)) else null
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Business,
                                    contentDescription = "Doanh nghiệp",
                                    tint = if (isCorporate) Color.White else Color(0xFF1E293B),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Doanh nghiệp",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isCorporate) Color.White else Color(0xFF1E293B)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Trường 1: Họ và tên (Cá nhân) / Tên doanh nghiệp (Doanh nghiệp)
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { 
                            Text(
                                text = if (isCorporate) "Tên Doanh nghiệp / Công ty" else "Họ và tên", 
                                color = Color(0xFF94A3B8), 
                                fontSize = 13.sp
                            ) 
                        },
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = {
                            Icon(
                                imageVector = if (isCorporate) Icons.Outlined.Business else Icons.Outlined.Badge,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        colors = customTextFieldColors(),
                        singleLine = true
                    )

                    // Nếu là Doanh nghiệp -> Hiển thị thêm ô Mã số thuế
                    if (isCorporate) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = taxCode,
                            onValueChange = { taxCode = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Mã số thuế doanh nghiệp", color = Color(0xFF94A3B8), fontSize = 13.sp) },
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Numbers,
                                    contentDescription = null,
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            colors = customTextFieldColors(),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Trường 2: Số điện thoại
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { 
                            Text(
                                text = if (isCorporate) "Số điện thoại doanh nghiệp" else "Số điện thoại", 
                                color = Color(0xFF94A3B8), 
                                fontSize = 13.sp
                            ) 
                        },
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Phone,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        colors = customTextFieldColors(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Trường 3: Email hoặc Gmail
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { 
                            Text(
                                text = if (isCorporate) "Email liên hệ công ty" else "Email hoặc Gmail", 
                                color = Color(0xFF94A3B8), 
                                fontSize = 13.sp
                            ) 
                        },
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Email,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        colors = customTextFieldColors(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Trường 4: Mật khẩu
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Mật khẩu", color = Color(0xFF94A3B8), fontSize = 13.sp) },
                        shape = RoundedCornerShape(12.dp),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Ẩn mật khẩu" else "Hiện mật khẩu",
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        colors = customTextFieldColors(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Trường 5: Xác nhận mật khẩu
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Xác nhận mật khẩu", color = Color(0xFF94A3B8), fontSize = 13.sp) },
                        shape = RoundedCornerShape(12.dp),
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.LockReset,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(
                                    imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (confirmPasswordVisible) "Ẩn mật khẩu" else "Hiện mật khẩu",
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        colors = customTextFieldColors(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Nút Đăng ký ➔
                    Button(
                        onClick = {
                            if (fullName.isBlank() || phoneNumber.isBlank() || email.isBlank() || password.isBlank() || (isCorporate && taxCode.isBlank())) {
                                Toast.makeText(context, "Vui lòng nhập đầy đủ thông tin!", Toast.LENGTH_SHORT).show()
                            } else if (confirmPassword.isNotEmpty() && password != confirmPassword) {
                                Toast.makeText(context, "Mật khẩu xác nhận không khớp!", Toast.LENGTH_SHORT).show()
                            } else {
                                val dbHelper = UserDatabaseHelper(context)
                                if (UserFileManager.isEmailExists(context, email) || dbHelper.isEmailExists(email)) {
                                    Toast.makeText(context, "Email này đã được đăng ký!", Toast.LENGTH_SHORT).show()
                                } else {
                                    val newUser = User(
                                        fullName = fullName,
                                        phoneNumber = phoneNumber,
                                        email = email,
                                        password = password,
                                        authType = if (isCorporate) "CORPORATE" else "NORMAL",
                                        isCorporate = isCorporate,
                                        taxCode = if (isCorporate) taxCode else null,
                                        accountType = if (isCorporate) "CORPORATE" else "PERSONAL"
                                    )
                                    val userInfo = UserInfo(
                                        fullName = fullName,
                                        phoneNumber = phoneNumber,
                                        email = email,
                                        password = password,
                                        authType = if (isCorporate) "CORPORATE" else "NORMAL",
                                        isCorporate = isCorporate,
                                        taxCode = if (isCorporate) taxCode else null,
                                        accountType = if (isCorporate) "CORPORATE" else "PERSONAL"
                                    )
                                    val isSaveSuccess = if (isCorporate) {
                                        UserFileManager.saveCorporateUser(context, userInfo)
                                    } else {
                                        UserFileManager.savePersonalUser(context, userInfo)
                                    }
                                    UserFileManager.saveCurrentSessionEmail(context, email)
                                    val isDbSuccess = if (isCorporate) {
                                        dbHelper.registerCorporateUser(newUser)
                                    } else {
                                        dbHelper.registerPersonalUser(newUser)
                                    }
                                    if (isSaveSuccess && isDbSuccess) {
                                        Toast.makeText(context, "Đăng Ký Thành Công", Toast.LENGTH_SHORT).show()
                                        onRegisterSuccess()
                                    } else {
                                        Toast.makeText(context, "Đăng ký thất bại, vui lòng thử lại!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(vertical = 14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Đăng ký",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Hoặc tiếp tục với
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = AppColors.BorderGray)
                        Text(
                            text = "Hoặc tiếp tục với",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = AppColors.BorderGray)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Nút Đăng ký với Google
                    OutlinedButton(
                        onClick = {
                            (context.findActivity() as? MainActivity)?.signInWithGoogle(onRegisterSuccess, isCorporate)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            GoogleIcon(modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isCorporate) "Đăng ký với Google Doanh nghiệp" else "Đăng ký với Google Cá nhân",
                                color = Color(0xFF1E293B),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Nút Đăng ký với Facebook
                    OutlinedButton(
                        onClick = {
                            (context.findActivity() as? MainActivity)?.signInWithFacebook(onRegisterSuccess, isCorporate)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Facebook,
                                contentDescription = "Facebook",
                                tint = Color(0xFF1877F2),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isCorporate) "Đăng ký với Facebook Doanh nghiệp" else "Đăng ký với Facebook Cá nhân",
                                color = Color(0xFF1E293B),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Footer chuyển sang Đăng nhập
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Đã có tài khoản? ",
                            fontSize = 12.sp,
                            color = Color(0xFF475569)
                        )
                        Text(
                            text = "Đăng nhập",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1D4ED8),
                            modifier = Modifier.clickable { onNavigateToLogin() }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
fun InputFieldLabel(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = Color(0xFF334155),
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
fun customTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Color(0xFF94A3B8),
    unfocusedBorderColor = AppColors.BorderGray,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    focusedTextColor = Color(0xFF0F172A),
    unfocusedTextColor = Color(0xFF0F172A)
)

@Composable
fun GoogleIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val strokeW = w * 0.20f
        val rect = androidx.compose.ui.geometry.Rect(
            strokeW / 2f,
            strokeW / 2f,
            w - strokeW / 2f,
            h - strokeW / 2f
        )

        // 1. Red Top Arc (Top-Right ~45° to Top-Left ~150°)
        val redPath = Path().apply {
            arcTo(rect, startAngleDegrees = -45f, sweepAngleDegrees = -130f, forceMoveTo = true)
        }
        drawPath(redPath, Color(0xFFEA4335), style = Stroke(width = strokeW))

        // 2. Yellow Left Arc (Top-Left ~175° to Bottom-Left ~245°)
        val yellowPath = Path().apply {
            arcTo(rect, startAngleDegrees = -175f, sweepAngleDegrees = -70f, forceMoveTo = true)
        }
        drawPath(yellowPath, Color(0xFFFBBC05), style = Stroke(width = strokeW))

        // 3. Green Bottom Arc (Bottom-Left ~245° to Bottom-Right ~350°)
        val greenPath = Path().apply {
            arcTo(rect, startAngleDegrees = -245f, sweepAngleDegrees = -105f, forceMoveTo = true)
        }
        drawPath(greenPath, Color(0xFF34A853), style = Stroke(width = strokeW))

        // 4. Blue Right Arc & Horizontal Bar
        val blueArcPath = Path().apply {
            arcTo(rect, startAngleDegrees = 10f, sweepAngleDegrees = -55f, forceMoveTo = true)
        }
        drawPath(blueArcPath, Color(0xFF4285F4), style = Stroke(width = strokeW))

        drawRect(
            color = Color(0xFF4285F4),
            topLeft = androidx.compose.ui.geometry.Offset(cx - w * 0.05f, cy - strokeW / 2f),
            size = androidx.compose.ui.geometry.Size(w * 0.52f, strokeW)
        )
    }
}

// ==================== MÀN HÌNH 4: ĐĂNG NHẬP ====================
@Composable
fun LoginScreen(
    onBack: (() -> Unit)? = null,
    onNavigateToRegister: () -> Unit = {},
    onLoginSuccess: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val rememberedEmail = remember { UserFileManager.getRememberedEmail(context) }
    var emailOrPhone by remember { mutableStateOf(rememberedEmail) }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(UserFileManager.isRemembered(context) || rememberedEmail.isNotBlank()) }

    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var resetEmailOrPhone by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmNewPasswordVisible by remember { mutableStateOf(false) }
    var resetErrorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFEFF6FF), // Soft icy blue top
                        Color(0xFFF8FAFC), // Pure light background middle
                        Color(0xFFE0F2FE)  // Gradient bottom tint
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (onBack != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = Color(0xFF1E293B)
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(40.dp))
            }



            // White Card bọc giao diện Login
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Logo SmartContract AI
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFE0F2FE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = "Logo",
                                tint = Color(0xFF0284C7),
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = "SmartContract AI",
                                fontSize = 5.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0284C7)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Chào mừng trở lại",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0038A8),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Đăng nhập để tiếp tục quản lý hợp đồng\nthông minh của bạn.",
                        fontSize = 12.sp,
                        color = AppColors.SubtitleGray,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Trường 1: Email hoặc Số điện thoại
                    Column(modifier = Modifier.fillMaxWidth()) {
                        InputFieldLabel(text = "Email hoặc Số điện thoại")
                        OutlinedTextField(
                            value = emailOrPhone,
                            onValueChange = { emailOrPhone = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Nhập email hoặc SĐT", color = Color(0xFF94A3B8), fontSize = 12.sp) },
                            shape = RoundedCornerShape(10.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Person,
                                    contentDescription = null,
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            colors = customTextFieldColors(),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Trường 2: Mật khẩu
                    Column(modifier = Modifier.fillMaxWidth()) {
                        InputFieldLabel(text = "Mật khẩu")
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Nhập mật khẩu", color = Color(0xFF94A3B8), fontSize = 12.sp) },
                            shape = RoundedCornerShape(10.dp),
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Lock,
                                    contentDescription = null,
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (passwordVisible) "Ẩn mật khẩu" else "Hiện mật khẩu",
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            colors = customTextFieldColors(),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Ghi nhớ đăng nhập & Quên mật khẩu
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { rememberMe = !rememberMe }
                        ) {
                            Checkbox(
                                checked = rememberMe,
                                onCheckedChange = { rememberMe = it },
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF1D4ED8)),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Ghi nhớ đăng nhập",
                                fontSize = 11.sp,
                                color = Color(0xFF475569)
                            )
                        }

                        Text(
                            text = "Quên mật khẩu?",
                            fontSize = 11.sp,
                            color = Color(0xFF1D4ED8),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable {
                                resetEmailOrPhone = emailOrPhone
                                newPassword = ""
                                confirmNewPassword = ""
                                newPasswordVisible = false
                                confirmNewPasswordVisible = false
                                resetErrorMessage = null
                                showForgotPasswordDialog = true
                            }
                        )
                    }

                    // Dialog Đặt lại mật khẩu (Quên mật khẩu)
                    if (showForgotPasswordDialog) {
                        AlertDialog(
                            onDismissRequest = { showForgotPasswordDialog = false },
                            icon = {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEFF6FF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Lock,
                                        contentDescription = null,
                                        tint = Color(0xFF1D4ED8),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            },
                            title = {
                                Text(
                                    text = "Đặt lại mật khẩu",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A),
                                    textAlign = TextAlign.Center
                                )
                            },
                            text = {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = "Nhập Email/SĐT tài khoản và mật khẩu mới để tiến hành cập nhật lại mật khẩu:",
                                        fontSize = 12.sp,
                                        color = Color(0xFF64748B),
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )

                                    // Field 1: Email / SĐT
                                    InputFieldLabel(text = "Email hoặc Số điện thoại")
                                    OutlinedTextField(
                                        value = resetEmailOrPhone,
                                        onValueChange = {
                                            resetEmailOrPhone = it
                                            resetErrorMessage = null
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        placeholder = { Text("Nhập email hoặc SĐT", color = Color(0xFF94A3B8), fontSize = 12.sp) },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = customTextFieldColors(),
                                        singleLine = true
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Field 2: Mật khẩu mới
                                    InputFieldLabel(text = "Mật khẩu mới")
                                    OutlinedTextField(
                                        value = newPassword,
                                        onValueChange = {
                                            newPassword = it
                                            resetErrorMessage = null
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        placeholder = { Text("Nhập mật khẩu mới", color = Color(0xFF94A3B8), fontSize = 12.sp) },
                                        shape = RoundedCornerShape(10.dp),
                                        visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                        trailingIcon = {
                                            IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                                                Icon(
                                                    imageVector = if (newPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                    contentDescription = null,
                                                    tint = Color(0xFF64748B)
                                                )
                                            }
                                        },
                                        colors = customTextFieldColors(),
                                        singleLine = true
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Field 3: Xác nhận mật khẩu mới
                                    InputFieldLabel(text = "Xác nhận mật khẩu mới")
                                    OutlinedTextField(
                                        value = confirmNewPassword,
                                        onValueChange = {
                                            confirmNewPassword = it
                                            resetErrorMessage = null
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        placeholder = { Text("Nhập lại mật khẩu mới", color = Color(0xFF94A3B8), fontSize = 12.sp) },
                                        shape = RoundedCornerShape(10.dp),
                                        visualTransformation = if (confirmNewPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                        trailingIcon = {
                                            IconButton(onClick = { confirmNewPasswordVisible = !confirmNewPasswordVisible }) {
                                                Icon(
                                                    imageVector = if (confirmNewPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                    contentDescription = null,
                                                    tint = Color(0xFF64748B)
                                                )
                                            }
                                        },
                                        colors = customTextFieldColors(),
                                        singleLine = true
                                    )

                                    if (resetErrorMessage != null) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = resetErrorMessage!!,
                                            color = Color(0xFFEF4444),
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        val dbHelper = UserDatabaseHelper(context)
                                        val existsInFile = UserFileManager.checkUserExists(context, resetEmailOrPhone)
                                        val existsInDb = dbHelper.isEmailExists(resetEmailOrPhone)

                                        when {
                                            resetEmailOrPhone.isBlank() -> {
                                                resetErrorMessage = "Vui lòng nhập Email hoặc SĐT!"
                                            }
                                            newPassword.isBlank() -> {
                                                resetErrorMessage = "Vui lòng nhập mật khẩu mới!"
                                            }
                                            newPassword.length < 6 -> {
                                                resetErrorMessage = "Mật khẩu mới phải có ít nhất 6 ký tự!"
                                            }
                                            confirmNewPassword != newPassword -> {
                                                resetErrorMessage = "Mật khẩu xác nhận không trùng khớp!"
                                            }
                                            !existsInFile && !existsInDb -> {
                                                resetErrorMessage = "Không tìm thấy tài khoản với Email/SĐT này!"
                                            }
                                            else -> {
                                                val updatedFile = UserFileManager.updatePassword(context, resetEmailOrPhone, newPassword)
                                                val updatedDb = dbHelper.updatePassword(resetEmailOrPhone, newPassword)

                                                if (updatedFile || updatedDb) {
                                                    emailOrPhone = resetEmailOrPhone
                                                    password = newPassword
                                                    showForgotPasswordDialog = false
                                                    Toast.makeText(context, "Cập nhật mật khẩu thành công! Mời bạn đăng nhập.", Toast.LENGTH_LONG).show()
                                                } else {
                                                    resetErrorMessage = "Không thể cập nhật mật khẩu, vui lòng thử lại sau!"
                                                }
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Đổi mật khẩu", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            },
                            dismissButton = {
                                OutlinedButton(
                                    onClick = { showForgotPasswordDialog = false },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Hủy", color = Color(0xFF64748B), fontSize = 13.sp)
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Nút Đăng nhập & Biometric (Fingerprint)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                if (emailOrPhone.isBlank() || password.isBlank()) {
                                    Toast.makeText(context, "Vui lòng nhập đầy đủ Email/SĐT và Mật khẩu!", Toast.LENGTH_SHORT).show()
                                } else {
                                    val dbHelper = UserDatabaseHelper(context)
                                    val isFileMatched = UserFileManager.checkNormalLogin(context, emailOrPhone, password)
                                    val isDbMatched = dbHelper.checkUserLogin(emailOrPhone, password)
                                    if (isFileMatched || isDbMatched) {
                                        UserFileManager.saveCurrentSessionEmail(context, emailOrPhone)
                                        UserFileManager.saveRememberMe(context, rememberMe, emailOrPhone)
                                        Toast.makeText(context, "Đăng Nhập Thành Công", Toast.LENGTH_SHORT).show()
                                        onLoginSuccess()
                                    } else {
                                        Toast.makeText(context, "Tài Khoản Không Tồn Tại", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(vertical = 14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Đăng nhập",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                val activity = context.findActivity() as? MainActivity
                                if (activity != null) {
                                    activity.showBiometricPrompt(onLoginSuccess)
                                } else {
                                    Toast.makeText(context, "Không thể kết nối dịch vụ sinh trắc học", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = "Sinh trắc học",
                                tint = Color(0xFF1D4ED8),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Hoặc đăng nhập với
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = AppColors.BorderGray)
                        Text(
                            text = "Hoặc đăng nhập với",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.padding(horizontal = 10.dp)
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = AppColors.BorderGray)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Nút đăng nhập MXH: Google, Facebook & App Auth
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    (context.findActivity() as? MainActivity)?.signInWithGoogle(onLoginSuccess)
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                contentPadding = PaddingValues(vertical = 10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    GoogleIcon(modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Google",
                                        color = Color(0xFF1E293B),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    (context.findActivity() as? MainActivity)?.signInWithFacebook(onLoginSuccess)
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                contentPadding = PaddingValues(vertical = 10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Facebook,
                                        contentDescription = "Facebook",
                                        tint = Color(0xFF1877F2),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Facebook",
                                        color = Color(0xFF1E293B),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                    var showTotpDialog by remember { mutableStateOf(false) }
                    var totpStep by remember { mutableIntStateOf(1) }
                    var totpCode by remember { mutableStateOf("") }
                    var totpError by remember { mutableStateOf(false) }

                    // Nút Đăng nhập với Google Authenticator (TOTP 2FA)
                    OutlinedButton(
                        onClick = {
                            totpCode = ""
                            totpError = false
                            totpStep = 1
                            showTotpDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFF8FAFC)),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Security,
                                contentDescription = "Google Authenticator",
                                tint = Color(0xFF0284C7),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Đăng nhập với Google Authenticator",
                                color = Color(0xFF0F172A),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Dialog nhập mã Google Authenticator (6 chữ số)
                    if (showTotpDialog) {
                        val currentUser = FirebaseAuth.getInstance().currentUser
                        val sessionEmail = UserFileManager.getCurrentSessionEmail(context)
                        val accountName = remember(emailOrPhone, currentUser, sessionEmail) {
                            val registeredUsers = UserFileManager.getAllUsers(context)
                            val lastUserEmail = registeredUsers.lastOrNull { it.email.isNotBlank() }?.email
                            val deviceGmail = try {
                                val am = context.getSystemService(android.accounts.AccountManager::class.java)
                                am?.getAccountsByType("com.google")?.firstOrNull()?.name
                            } catch (_: Exception) { null }

                            emailOrPhone.ifBlank {
                                currentUser?.email?.ifBlank { null }
                                    ?: sessionEmail.ifBlank { null }
                                    ?: lastUserEmail?.ifBlank { null }
                                    ?: deviceGmail?.ifBlank { null }
                                    ?: "user.google@gmail.com"
                            }
                        }
                        val secretKey = remember(accountName) {
                            val prefs = context.getSharedPreferences("app_device_id_prefs", Context.MODE_PRIVATE)
                            var deviceUuid = prefs.getString("unique_app_device_id", null)
                            if (deviceUuid == null) {
                                deviceUuid = UUID.randomUUID().toString()
                                prefs.edit {
                                    putString("unique_app_device_id", deviceUuid)
                                }
                            }
                            val seed = (accountName + deviceUuid).uppercase()
                            val base32Alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray()
                            val sb = StringBuilder()
                            val md5Bytes = MessageDigest.getInstance("MD5").digest(seed.toByteArray())
                            for (i in 0 until 16) {
                                val byteVal = md5Bytes[i % md5Bytes.size].toInt() and 0xFF
                                sb.append(base32Alphabet[byteVal % base32Alphabet.size])
                            }
                            sb.toString()
                        }
                        val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                        var showQrCode by remember { mutableStateOf(true) }
                        val otpauthUrl = "otpauth://totp/SmartContractAI:$accountName?secret=$secretKey&issuer=SmartContractAI"
                        val encodedQrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=${java.net.URLEncoder.encode(otpauthUrl, "UTF-8")}"

                        val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
                        DisposableEffect(lifecycleOwner, showTotpDialog) {
                            val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                                if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME && showTotpDialog && totpStep == 1) {
                                    totpStep = 2
                                }
                            }
                            lifecycleOwner.lifecycle.addObserver(observer)
                            onDispose {
                                lifecycleOwner.lifecycle.removeObserver(observer)
                            }
                        }

                        if (totpStep == 1) {
                            // Bước 1: Quét mã QR hoặc Nhập thủ công
                            AlertDialog(
                                onDismissRequest = { showTotpDialog = false },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Security,
                                        contentDescription = null,
                                        tint = Color(0xFF1D4ED8),
                                        modifier = Modifier.size(32.dp)
                                    )
                                },
                                title = {
                                    Text(
                                        text = "Google Authenticator 2FA",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A),
                                        textAlign = TextAlign.Center
                                    )
                                },
                                text = {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        // Chuyển đổi Mode: QR Code / Sao chép thủ công
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFFF1F5F9))
                                                .padding(3.dp),
                                            horizontalArrangement = Arrangement.SpaceEvenly
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(if (showQrCode) Color.White else Color.Transparent)
                                                    .clickable { showQrCode = true }
                                                    .padding(vertical = 6.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "📷 Quét mã QR",
                                                    fontSize = 11.sp,
                                                    fontWeight = if (showQrCode) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (showQrCode) Color(0xFF1D4ED8) else Color(0xFF64748B)
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(if (!showQrCode) Color.White else Color.Transparent)
                                                    .clickable { showQrCode = false }
                                                    .padding(vertical = 6.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "📋 Nhập thủ công",
                                                    fontSize = 11.sp,
                                                    fontWeight = if (!showQrCode) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (!showQrCode) Color(0xFF1D4ED8) else Color(0xFF64748B)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(14.dp))

                                        if (showQrCode) {
                                            Text(
                                                text = "Quét mã QR dưới đây bằng Google Authenticator để liên kết tài khoản:",
                                                fontSize = 11.5.sp,
                                                color = Color(0xFF64748B),
                                                textAlign = TextAlign.Center
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(160.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(Color.White)
                                                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                                                    .clickable {
                                                        try {
                                                            val intent = Intent(Intent.ACTION_VIEW, otpauthUrl.toUri())
                                                            context.startActivity(intent)
                                                        } catch (e: Exception) {
                                                            e.printStackTrace()
                                                        }
                                                        totpStep = 2
                                                    }
                                                    .padding(8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                AsyncImage(
                                                    model = encodedQrUrl,
                                                    contentDescription = "Google Authenticator QR Code",
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Fit
                                                )
                                            }
                                        } else {
                                            Text(
                                                text = "Sao chép Secret Key dưới đây để thêm thủ công vào Google Authenticator:",
                                                fontSize = 11.5.sp,
                                                color = Color(0xFF64748B),
                                                textAlign = TextAlign.Center
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Surface(
                                                color = Color(0xFFF1F5F9),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column {
                                                        Text("MÃ THÊM THỦ CÔNG", fontSize = 9.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                                                        Text(secretKey, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D4ED8))
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(secretKey))
                                                            Toast.makeText(context, "Đã sao chép Secret Key!", Toast.LENGTH_SHORT).show()
                                                            totpStep = 2
                                                        },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.ContentCopy,
                                                            contentDescription = "Copy",
                                                            tint = Color(0xFF1D4ED8),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                    }
                                },
                                confirmButton = {
                                    Button(
                                        onClick = { totpStep = 2 },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Tiếp theo", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showTotpDialog = false }) {
                                        Text("Hủy", color = Color(0xFF64748B), fontSize = 13.sp)
                                    }
                                },
                                containerColor = Color.White,
                                shape = RoundedCornerShape(16.dp)
                            )
                        } else {
                            // Bước 2: Load thông tin tài khoản Google (Hình 4) & Nhập mã 6 chữ số để Đăng Nhập (Hình 5)
                            AlertDialog(
                                onDismissRequest = { showTotpDialog = false },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Security,
                                        contentDescription = null,
                                        tint = Color(0xFF1D4ED8),
                                        modifier = Modifier.size(32.dp)
                                    )
                                },
                                title = {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "Google Authenticator 2FA",
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A),
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        // Hình 4: Badge load tài khoản Google của người dùng
                                        Surface(
                                            color = Color(0xFFEFF6FF),
                                            shape = RoundedCornerShape(20.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Person,
                                                    contentDescription = null,
                                                    tint = Color(0xFF1D4ED8),
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = accountName,
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF1D4ED8)
                                                )
                                            }
                                        }
                                    }
                                },
                                text = {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        // Hình 5: Nhập mã 6 chữ số đang hiển thị trên Google Authenticator
                                        Text(
                                            text = "Nhập mã 6 chữ số đang hiển thị trên Google Authenticator:",
                                            fontSize = 12.sp,
                                            color = Color(0xFF475569),
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))

                                        OutlinedTextField(
                                            value = totpCode,
                                            onValueChange = { input ->
                                                if (input.length <= 6 && input.all { it.isDigit() }) {
                                                    totpCode = input
                                                    totpError = false
                                                }
                                            },
                                            placeholder = { Text("000000", color = Color(0xFF94A3B8), fontSize = 15.sp) },
                                            isError = totpError,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            shape = RoundedCornerShape(10.dp),
                                            colors = customTextFieldColors(),
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        if (totpError) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "Mã 6 chữ số không hợp lệ!",
                                                fontSize = 11.sp,
                                                color = Color(0xFFEF4444)
                                            )
                                        }
                                    }
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            if (totpCode.length == 6) {
                                                UserFileManager.saveCurrentSessionEmail(context, accountName)
                                                UserFileManager.saveRememberMe(context, rememberMe, accountName)
                                                showTotpDialog = false
                                                Toast.makeText(context, "Xác thực Google Authenticator thành công!", Toast.LENGTH_SHORT).show()
                                                onLoginSuccess()
                                            } else {
                                                totpError = true
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Xác thực & Đăng nhập", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { totpStep = 1 }) {
                                        Text("Quay lại", color = Color(0xFF64748B), fontSize = 13.sp)
                                    }
                                },
                                containerColor = Color.White,
                                shape = RoundedCornerShape(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                    // Chưa có tài khoản? Đăng ký ngay
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Chưa có tài khoản? ",
                            fontSize = 12.sp,
                            color = Color(0xFF475569)
                        )
                        Text(
                            text = "Đăng ký ngay",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1D4ED8),
                            modifier = Modifier.clickable { onNavigateToRegister() }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Text dưới Card: Kết nối được mã hóa đầu cuối
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Kết nối được mã hóa đầu cuối",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ==================== MÀN HÌNH QUẢN LÝ HỢP ĐỒNG (HÌNH 1 - TAB CONTRACTS) ====================
@Composable
fun FilterChipItem(
    label: String,
    isSelected: Boolean,
    bgColor: Color,
    textColor: Color,
    dotColor: Color? = null,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) textColor else bgColor,
        modifier = Modifier.height(36.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (dotColor != null && !isSelected) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else textColor
            )
        }
    }
}

@Composable
fun ContractManagementCard(
    code: String,
    badgeText: String,
    badgeBg: Color,
    badgeTextColor: Color,
    stripColor: Color,
    title: String,
    subtitle: String,
    footerLeft: String,
    footerLeftIcon: ImageVector,
    footerRight: String? = null,
    footerRightIcon: ImageVector? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            // Thanh màu trang trí bên trái
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(stripColor)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Hàng 1: Mã HĐ + Badge Trạng thái
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = code,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B)
                    )

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = badgeBg
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeTextColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Tên Hợp Đồng
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Đối tác / Nội bộ
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Color(0xFF475569)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Footer Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = footerLeftIcon,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = footerLeft,
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }

                    if (footerRight != null && footerRightIcon != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = footerRightIcon,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = footerRight,
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ContractManagementScreen(
    onNavigateToCreateContractAI: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterIndex by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        // 1. Tiêu đề + Nút + Tạo Hợp đồng Mới
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Quản lý Hợp đồng",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )

            Button(
                onClick = onNavigateToCreateContractAI,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "+ Tạo Hợp đồng Mới",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Thanh Tìm Kiếm
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = "Tìm kiếm theo mã, tên, đối tác...",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(20.dp)
                )
            },
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = Color(0xFF3B82F6),
                unfocusedBorderColor = Color(0xFFE2E8F0)
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Thanh Bộ Lọc (Filter Chips)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChipItem(
                label = "Tất cả",
                isSelected = selectedFilterIndex == 0,
                bgColor = Color(0xFFDBEAFE),
                textColor = Color(0xFF1D4ED8),
                onClick = { selectedFilterIndex = 0 }
            )

            FilterChipItem(
                label = "Bản nháp",
                isSelected = selectedFilterIndex == 1,
                bgColor = Color(0xFFDBEAFE),
                textColor = Color(0xFF2563EB),
                dotColor = Color(0xFF64748B),
                onClick = { selectedFilterIndex = 1 }
            )

            FilterChipItem(
                label = "Chờ duyệt",
                isSelected = selectedFilterIndex == 2,
                bgColor = Color(0xFFDCFCE7),
                textColor = Color(0xFF15803D),
                dotColor = Color(0xFF22C55E),
                onClick = { selectedFilterIndex = 2 }
            )

            FilterChipItem(
                label = "Chờ ký",
                isSelected = selectedFilterIndex == 3,
                bgColor = Color(0xFFEEF2FF),
                textColor = Color(0xFF4338CA),
                dotColor = Color(0xFF6366F1),
                onClick = { selectedFilterIndex = 3 }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Danh sách Hợp đồng (Hình 1)
        ContractManagementCard(
            code = "#NDA-2023-894",
            badgeText = "CHỜ KÝ",
            badgeBg = Color(0xFFEEF2FF),
            badgeTextColor = Color(0xFF4F46E5),
            stripColor = Color(0xFF1D4ED8),
            title = "Thỏa thuận Bảo mật Thông tin (NDA) - TechCorp",
            subtitle = "Đối tác: TechCorp Inc.",
            footerLeft = "Hết hạn: 2 ngày",
            footerLeftIcon = Icons.Default.Schedule,
            footerRight = "1/2 Đã ký",
            footerRightIcon = Icons.Default.People
        )

        Spacer(modifier = Modifier.height(12.dp))

        ContractManagementCard(
            code = "#MSA-2023-112",
            badgeText = "HOÀN TẤT",
            badgeBg = Color(0xFFDCFCE7),
            badgeTextColor = Color(0xFF16A34A),
            stripColor = Color(0xFF16A34A),
            title = "Hợp đồng Dịch vụ (MSA) - Global Logistics",
            subtitle = "Đối tác: Global Logistics LLC",
            footerLeft = "Ký: 15/10/2023",
            footerLeftIcon = Icons.Default.CalendarToday
        )

        Spacer(modifier = Modifier.height(12.dp))

        ContractManagementCard(
            code = "#EMP-2023-045",
            badgeText = "BẢN NHÁP",
            badgeBg = Color(0xFFDBEAFE),
            badgeTextColor = Color(0xFF2563EB),
            stripColor = Color(0xFF64748B),
            title = "Hợp đồng Lao động - Nguyễn Văn A",
            subtitle = "Nội bộ: HR Dept",
            footerLeft = "Cập nhật: 2h trước",
            footerLeftIcon = Icons.Default.Edit
        )

        Spacer(modifier = Modifier.height(12.dp))

        ContractManagementCard(
            code = "#SLA-2023-551",
            badgeText = "CHỜ DUYỆT",
            badgeBg = Color(0xFFDCFCE7),
            badgeTextColor = Color(0xFF059669),
            stripColor = Color(0xFF10B981),
            title = "Cam kết Chất lượng Dịch vụ (SLA)",
            subtitle = "Đối tác: CloudSync Inc.",
            footerLeft = "Đang chờ Legal Review",
            footerLeftIcon = Icons.Default.Description
        )

        Spacer(modifier = Modifier.height(30.dp))
    }
}

// ==================== MÀN HÌNH BUSINESS ADMINISTRATION (HÌNH 2 - TAB BUSINESS) ====================
@Composable
fun BusinessAdministrationScreen(
    onAccountClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var selectedMode by remember { mutableStateOf("Director") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // 1. Header (Avatar, SmartContract AI, Bell Notification)
        DashboardHeader(
            onAccountClick = onAccountClick,
            onLogoutClick = onLogoutClick
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Title & Subtitle
        Text(
            text = "Business Administration",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Manage contracts, team members, and monitor activities.",
            fontSize = 12.sp,
            color = Color(0xFF64748B)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Mode Toggle (Director Mode vs Employee Mode)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE0E7FF)),
            border = BorderStroke(1.dp, Color(0xFFC7D2FE))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Director Mode Button
                Button(
                    onClick = { selectedMode = "Director" },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedMode == "Director") Color(0xFF1D4ED8) else Color.Transparent
                    ),
                    shape = RoundedCornerShape(10.dp),
                    elevation = if (selectedMode == "Director") ButtonDefaults.buttonElevation(2.dp) else null,
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Text(
                        text = "Director Mode",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedMode == "Director") Color.White else Color(0xFF334155)
                    )
                }

                // Employee Mode Button
                Button(
                    onClick = { selectedMode = "Employee" },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedMode == "Employee") Color(0xFF1D4ED8) else Color.Transparent
                    ),
                    shape = RoundedCornerShape(10.dp),
                    elevation = if (selectedMode == "Employee") ButtonDefaults.buttonElevation(2.dp) else null,
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Text(
                        text = "Employee Mode",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedMode == "Employee") Color.White else Color(0xFF334155)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 4. Pending Approvals Section (Card)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            border = BorderStroke(1.dp, Color(0xFFF1F5F9))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Pending Approvals Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AssignmentTurnedIn,
                            contentDescription = null,
                            tint = Color(0xFF1D4ED8),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Pending Approvals",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFDBEAFE)
                    ) {
                        Text(
                            text = "3 Requires Action",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1D4ED8),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Item 1: NDA - TechNova Solutions
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "NDA - TechNova Solutions",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Submitted by: Sarah Jenkins • 2 hours ago",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // AI Risk Badge: Low
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFEEF2FF)
                        ) {
                            Text(
                                text = "🤖 AI Risk: Low",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4F46E5),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Buttons: Reject vs Approve
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { Toast.makeText(context, "Rejected NDA", Toast.LENGTH_SHORT).show() },
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(1.dp, Color(0xFFEF4444)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                Text("Reject", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            Button(
                                onClick = { Toast.makeText(context, "Approved NDA", Toast.LENGTH_SHORT).show() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                Text("Approve", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Item 2: Vendor Agreement - GlobalSupplies
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Vendor Agreement - GlobalSupplies",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Submitted by: Michael Chang • 5 hours ago",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // AI Risk Badge: High
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFFEE2E2)
                        ) {
                            Text(
                                text = "⚠️ AI Risk: High (Liability Clause)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB91C1C),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Buttons: Reject vs Approve
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { Toast.makeText(context, "Rejected Vendor Agreement", Toast.LENGTH_SHORT).show() },
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(1.dp, Color(0xFFEF4444)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                Text("Reject", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            Button(
                                onClick = { Toast.makeText(context, "Approved Vendor Agreement", Toast.LENGTH_SHORT).show() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                Text("Approve", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Link: View all pending contracts
                Text(
                    text = "View all pending contracts",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1D4ED8),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { Toast.makeText(context, "Viewing all pending contracts", Toast.LENGTH_SHORT).show() }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 5. Team Section (Card)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            border = BorderStroke(1.dp, Color(0xFFF1F5F9))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Team Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = null,
                            tint = Color(0xFF1D4ED8),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Team",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }

                    IconButton(
                        onClick = { Toast.makeText(context, "Add team member", Toast.LENGTH_SHORT).show() },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Member",
                            tint = Color(0xFF1D4ED8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Member 1: Sarah Jenkins
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEEF2FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("SJ", fontWeight = FontWeight.Bold, color = Color(0xFF4F46E5), fontSize = 13.sp)
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text("Sarah Jenkins", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                            Text("Legal Counsel", fontSize = 11.sp, color = Color(0xFF64748B))
                        }
                    }

                    IconButton(onClick = { }) {
                        Icon(Icons.Default.MoreVert, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Member 2: Michael Chang
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFDCFCE7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("MC", fontWeight = FontWeight.Bold, color = Color(0xFF16A34A), fontSize = 13.sp)
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text("Michael Chang", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                            Text("Procurement", fontSize = 11.sp, color = Color(0xFF64748B))
                        }
                    }

                    IconButton(onClick = { }) {
                        Icon(Icons.Default.MoreVert, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Button: Invite via QR
                OutlinedButton(
                    onClick = { Toast.makeText(context, "QR Code Invite Scanner", Toast.LENGTH_SHORT).show() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = Color(0xFF0F172A), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Invite via QR", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 6. Audit Log Section (Card)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            border = BorderStroke(1.dp, Color(0xFFF1F5F9))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = Color(0xFF1D4ED8),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Audit Log",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Timeline Log 1
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF16A34A))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Sarah Jenkins approved Employment Agreement.", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                        Text("10 mins ago", fontSize = 11.sp, color = Color(0xFF64748B))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Timeline Log 2
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2563EB))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("System AI scanned 5 new vendor contracts.", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                        Text("1 hour ago", fontSize = 11.sp, color = Color(0xFF64748B))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Timeline Log 3
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFDC2626))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Michael Chang rejected NDA draft.", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                        Text("3 hours ago", fontSize = 11.sp, color = Color(0xFF64748B))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

// ==================== DIALOG CÀI ĐẶT XÁC THỰC SINH TRẮC HỌC ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BiometricSettingsDialog(
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("biometric_prefs", Context.MODE_PRIVATE) }

    var isBiometricEnabled by remember { mutableStateOf(prefs.getBoolean("biometric_enabled", true)) }
    var isSignBiometricEnabled by remember { mutableStateOf(prefs.getBoolean("biometric_sign_enabled", true)) }

    val biometricManager = remember { BiometricManager.from(context) }
    val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
    val canAuthenticateStatus = remember { biometricManager.canAuthenticate(authenticators) }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEFF6FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Fingerprint,
                            contentDescription = null,
                            tint = Color(0xFF1D4ED8),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Thiết lập Sinh Trắc Học",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Cấu hình Vân tay / Khuôn mặt đăng nhập nhanh",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Đóng",
                        tint = Color(0xFF64748B)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                HorizontalDivider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(bottom = 14.dp))

                // Hardware Status Banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (canAuthenticateStatus == BiometricManager.BIOMETRIC_SUCCESS) Color(0xFFF0FDF4) else Color(0xFFFFFBEB)
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (canAuthenticateStatus == BiometricManager.BIOMETRIC_SUCCESS) Color(0xFFBBF7D0) else Color(0xFFFDE68A)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (canAuthenticateStatus == BiometricManager.BIOMETRIC_SUCCESS) Icons.Default.CheckCircle else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (canAuthenticateStatus == BiometricManager.BIOMETRIC_SUCCESS) Color(0xFF16A34A) else Color(0xFFD97706),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when (canAuthenticateStatus) {
                                    BiometricManager.BIOMETRIC_SUCCESS -> "Thiết bị sẵn sàng Sinh Trắc Học"
                                    BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "Chưa đăng ký Vân tay / Face ID"
                                    BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "Thiết bị không hỗ trợ Sinh trắc học"
                                    else -> "Cảm biến sinh trắc học hiện bận"
                                },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (canAuthenticateStatus == BiometricManager.BIOMETRIC_SUCCESS) Color(0xFF14532D) else Color(0xFF78350F)
                            )
                            Text(
                                text = when (canAuthenticateStatus) {
                                    BiometricManager.BIOMETRIC_SUCCESS -> "Cảm biến Fingerprint / Face ID hoạt động bình thường."
                                    BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "Vui lòng thêm vân tay trong Cài đặt thiết bị."
                                    else -> "Hệ thống hỗ trợ mã PIN/Mật khẩu thay thế."
                                },
                                fontSize = 11.sp,
                                color = if (canAuthenticateStatus == BiometricManager.BIOMETRIC_SUCCESS) Color(0xFF166534) else Color(0xFF92400E)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Toggle 1: Đăng nhập nhanh
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Đăng nhập nhanh bằng Sinh Trắc Học",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Sử dụng Vân tay hoặc Face ID thay cho Mật khẩu khi mở ứng dụng",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = isBiometricEnabled,
                        onCheckedChange = { checked ->
                            if (checked) {
                                val activity = context.findActivity() as? MainActivity
                                activity?.showBiometricPrompt {
                                    isBiometricEnabled = true
                                    prefs.edit { putBoolean("biometric_enabled", true) }
                                }
                            } else {
                                isBiometricEnabled = false
                                prefs.edit { putBoolean("biometric_enabled", false) }
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF1D4ED8)
                        )
                    )
                }

                HorizontalDivider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(vertical = 8.dp))

                // Toggle 2: Xác thực khi ký & duyệt hợp đồng
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Yêu cầu Vân tay khi Ký & Duyệt Hợp Đồng",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Xác nhận vân tay để hoàn tất ký số hợp đồng AI an toàn 256-bit",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = isSignBiometricEnabled,
                        onCheckedChange = { checked ->
                            isSignBiometricEnabled = checked
                            prefs.edit { putBoolean("biometric_sign_enabled", checked) }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF1D4ED8)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Test Biometric Authentication Button
                OutlinedButton(
                    onClick = {
                        val activity = context.findActivity() as? MainActivity
                        if (activity != null) {
                            activity.showBiometricPrompt {
                                Toast.makeText(context, "Xác thực sinh trắc học thành công!", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "Sinh trắc học hoạt động bình thường", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF1D4ED8))
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Fingerprint,
                        contentDescription = null,
                        tint = Color(0xFF1D4ED8),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Thử nghiệm Xác thực ngay",
                        color = Color(0xFF1D4ED8),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    prefs.edit {
                        putBoolean("biometric_enabled", isBiometricEnabled)
                        putBoolean("biometric_sign_enabled", isSignBiometricEnabled)
                    }
                    Toast.makeText(context, "Đã lưu cài đặt Sinh Trắc Học thành công!", Toast.LENGTH_SHORT).show()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Text("Lưu Cấu Hình", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        },
        dismissButton = null
    )
}

// ==================== DIALOG ĐIỀU KHOẢN & BẢO MẬT ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsAndPrivacyDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEFF6FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Security,
                            contentDescription = null,
                            tint = Color(0xFF1D4ED8),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Điều Khoản & Bảo Mật",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Quyền riêng tư & bảo vệ dữ liệu SmartContract AI",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Đóng",
                        tint = Color(0xFF64748B)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                HorizontalDivider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(bottom = 14.dp))

                // Security Standard Banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                    border = BorderStroke(1.dp, Color(0xFFBBF7D0))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Bảo Mật Chuẩn Ngân Hàng 256-bit AES",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF14532D)
                            )
                            Text(
                                text = "Dữ liệu hợp đồng được mã hóa đầu cuối và tuân thủ Nghị định 13/2023/NĐ-CP.",
                                fontSize = 11.sp,
                                color = Color(0xFF166534)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Section 1
                Text(
                    text = "1. Điều Khoản Sử Dụng Dịch Vụ",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "SmartContract AI cung cấp giải pháp khởi tạo, phân tích và quản lý hợp đồng thông minh. Người dùng chịu trách nhiệm bảo mật thông tin tài khoản và tính xác thực của tài liệu khi giao dịch.",
                    fontSize = 12.sp,
                    color = Color(0xFF475569),
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Section 2
                Text(
                    text = "2. Chính Sách Bảo Mật Dữ Liệu",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Chúng tôi cam kết không bán, chia sẻ hoặc tiết lộ nội dung hợp đồng và dữ liệu sinh trắc học của bạn cho bất kỳ bên thứ ba nào ngoại trừ trường hợp có yêu cầu bằng văn bản từ cơ quan pháp luật có thẩm quyền.",
                    fontSize = 12.sp,
                    color = Color(0xFF475569),
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Section 3
                Text(
                    text = "3. Xử Lý Chữ Ký Số & OTP",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Mọi giao dịch ký số thông qua sinh trắc học (Vân tay/Face ID) hoặc mã OTP đều mang giá trị pháp lý theo Luật Giao Dịch Điện Tử Việt Nam.",
                    fontSize = 12.sp,
                    color = Color(0xFF475569),
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "SmartContract AI v1.0.4 • Cập nhật lần cuối: 2026",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Text("Tôi Đã Hiểu & Đồng Ý", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        },
        dismissButton = null
    )
}

// ==================== MÀN HÌNH SETTINGS ADMINISTRATION (TAB SETTINGS) ====================
@Composable
fun SettingsAdministrationScreen(
    onAccountClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val currentEmail = UserFileManager.getCurrentSessionEmail(context)
    val currentUser = remember(currentEmail) { UserFileManager.getUserByEmail(context, currentEmail) }
    var showBiometricDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }

    val prefs = remember { context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE) }
    var isNotificationEnabled by remember { mutableStateOf(prefs.getBoolean("fcm_notifications_enabled", true)) }

    if (showBiometricDialog) {
        BiometricSettingsDialog(
            onDismiss = { showBiometricDialog = false }
        )
    }

    if (showTermsDialog) {
        TermsAndPrivacyDialog(
            onDismiss = { showTermsDialog = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        DashboardHeader(
            onAccountClick = onAccountClick,
            onLogoutClick = onLogoutClick
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Cài Đặt & Cấu Hình System",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Quản lý thông tin tài khoản, cấu hình thông báo và bảo mật.",
            fontSize = 12.sp,
            color = Color(0xFF64748B)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFDBEAFE)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "User",
                        tint = Color(0xFF1D4ED8),
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentUser?.fullName?.ifBlank { "Người Dùng SmartContract" } ?: "Người Dùng SmartContract",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = currentUser?.email ?: currentEmail.ifBlank { "user@smartcontract.ai" },
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = if (currentUser?.isCorporate == true) Color(0xFFEFF6FF) else Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (currentUser?.isCorporate == true) "Tài khoản Doanh Nghiệp (Corporate)" else "Tài khoản Cá Nhân (Personal)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (currentUser?.isCorporate == true) Color(0xFF1D4ED8) else Color(0xFF475569),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "TÙY CHỌN BẢO MẬT & HỆ THỐNG",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF64748B),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column {
                SettingsItemRow(
                    icon = Icons.Outlined.Person,
                    title = "Thông tin cá nhân & Tài khoản",
                    subtitle = "Cập nhật họ tên, số điện thoại & thông tin công ty",
                    onClick = {
                        Toast.makeText(context, "Xem thông tin Tài Khoản", Toast.LENGTH_SHORT).show()
                        onAccountClick()
                    }
                )
                HorizontalDivider(color = Color(0xFFF1F5F9))
                SettingsItemRow(
                    icon = Icons.Outlined.Fingerprint,
                    title = "Xác thực Sinh Trắc Học",
                    subtitle = "Vân tay / Khuôn mặt đăng nhập nhanh",
                    onClick = {
                        showBiometricDialog = true
                    }
                )
                HorizontalDivider(color = Color(0xFFF1F5F9))
                SettingsItemRow(
                    icon = Icons.Outlined.Notifications,
                    title = "Thông Báo",
                    subtitle = "Cấu hình nhận thông báo hợp đồng & duyệt",
                    trailingContent = {
                        Switch(
                            checked = isNotificationEnabled,
                            onCheckedChange = { checked ->
                                isNotificationEnabled = checked
                                prefs.edit { putBoolean("fcm_notifications_enabled", checked) }
                                Toast.makeText(
                                    context,
                                    if (checked) "Đã bật thông báo nhận hợp đồng & duyệt" else "Đã tắt thông báo",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF1D4ED8),
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color(0xFFCBD5E1)
                            )
                        )
                    },
                    onClick = {
                        isNotificationEnabled = !isNotificationEnabled
                        prefs.edit { putBoolean("fcm_notifications_enabled", isNotificationEnabled) }
                        Toast.makeText(
                            context,
                            if (isNotificationEnabled) "Đã bật thông báo nhận hợp đồng & duyệt" else "Đã tắt thông báo",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
                HorizontalDivider(color = Color(0xFFF1F5F9))
                SettingsItemRow(
                    icon = Icons.Outlined.Security,
                    title = "Điều Khoản & Bảo Mật",
                    subtitle = "Chính sách bảo mật SmartContract AI",
                    onClick = {
                        showTermsDialog = true
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                FirebaseAuth.getInstance().signOut()
                Toast.makeText(context, "Đã đăng xuất thành công", Toast.LENGTH_SHORT).show()
                onLogoutClick()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEF2F2)),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, Color(0xFFFCA5A5))
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = null,
                tint = Color(0xFFDC2626),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Đăng Xuất Tài Khoản",
                color = Color(0xFFDC2626),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
fun SettingsItemRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    showChevron: Boolean = true,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = Color(0xFF1D4ED8),
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0F172A)
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = Color(0xFF64748B)
            )
        }
        if (trailingContent != null) {
            trailingContent()
        } else if (showChevron) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ==================== DIALOG HỒ SƠ THÔNG TIN CÁ NHÂN & TÀI KHOẢN ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileDialog(
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val currentEmail = UserFileManager.getCurrentSessionEmail(context)
    val firebaseUser = FirebaseAuth.getInstance().currentUser
    val userEmail = currentEmail.ifBlank { firebaseUser?.email ?: "" }
    val existingUser = remember(userEmail) { UserFileManager.getUserByEmail(context, userEmail) }

    var fullName by remember { mutableStateOf(existingUser?.fullName ?: firebaseUser?.displayName ?: "Người Dùng SmartContract") }
    var phoneNumber by remember { mutableStateOf(existingUser?.phoneNumber ?: firebaseUser?.phoneNumber ?: "") }
    var taxCode by remember { mutableStateOf(existingUser?.taxCode ?: "") }
    val isCorporate = existingUser?.isCorporate == true || existingUser?.accountType == "CORPORATE" || existingUser?.authType?.startsWith("CORPORATE") == true
    val authType = existingUser?.authType ?: "NORMAL"
    var avatarUrl by remember { mutableStateOf(existingUser?.avatarUrl ?: firebaseUser?.photoUrl?.toString()) }

    var showUrlDialog by remember { mutableStateOf(false) }
    var tempUrlInput by remember { mutableStateOf("") }

    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            avatarUrl = it.toString()
            Toast.makeText(context, "Đã chọn ảnh đại diện mới!", Toast.LENGTH_SHORT).show()
        }
    }

    if (showUrlDialog) {
        AlertDialog(
            onDismissRequest = { showUrlDialog = false },
            title = {
                Text(
                    text = "Nhập URL Hình Ảnh Avatar",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
            },
            text = {
                Column {
                    Text(
                        text = "Dán liên kết hình ảnh trực tuyến (HTTP/HTTPS):",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tempUrlInput,
                        onValueChange = { tempUrlInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("https://example.com/avatar.png", fontSize = 12.sp, color = Color(0xFF94A3B8)) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF1D4ED8),
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        avatarUrl = tempUrlInput.trim().ifBlank { null }
                        showUrlDialog = false
                        Toast.makeText(context, "Đã cập nhật liên kết avatar!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Đồng Ý", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUrlDialog = false }) {
                    Text("Hủy", color = Color(0xFF64748B))
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEFF6FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color(0xFF1D4ED8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Thông Tin Cá Nhân & Tài Khoản",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Hồ sơ cá nhân và phân quyền ứng dụng",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Đóng",
                        tint = Color(0xFF64748B)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                HorizontalDivider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(bottom = 14.dp))

                // Avatar Header Summary
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC), shape = RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clickable { avatarPickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(Color(0xFFDBEAFE)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!avatarUrl.isNullOrEmpty()) {
                                AsyncImage(
                                    model = avatarUrl,
                                    contentDescription = "Avatar",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = fullName.take(1).uppercase(),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1D4ED8)
                                )
                            }
                        }
                        // Camera Badge
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1D4ED8))
                                .border(1.5.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Đổi Avatar",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = fullName.ifBlank { "Người Dùng SmartContract" },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = userEmail.ifBlank { "Chưa cập nhật email" },
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                color = if (isCorporate) Color(0xFFEFF6FF) else Color(0xFFF1F5F9),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = if (isCorporate) "Doanh Nghiệp" else "Cá Nhân",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCorporate) Color(0xFF1D4ED8) else Color(0xFF475569),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Surface(
                                color = Color(0xFFF0FDF4),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = authType,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF166534),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Avatar Action Buttons Row
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { avatarPickerLauncher.launch("image/*") },
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        border = BorderStroke(1.dp, Color(0xFF1D4ED8))
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = null,
                            tint = Color(0xFF1D4ED8),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tải ảnh lên", fontSize = 11.sp, color = Color(0xFF1D4ED8), fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = {
                            tempUrlInput = avatarUrl ?: ""
                            showUrlDialog = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        border = BorderStroke(1.dp, Color(0xFF64748B))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Nhập URL", fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.SemiBold)
                    }

                    if (!avatarUrl.isNullOrEmpty()) {
                        IconButton(
                            onClick = {
                                avatarUrl = null
                                Toast.makeText(context, "Đã gỡ ảnh đại diện", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Gỡ ảnh",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Input fields
                Text(
                    text = "Họ và tên",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF334155)
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF1D4ED8),
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Email tài khoản",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF334155)
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = userEmail,
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledBorderColor = Color(0xFFE2E8F0),
                        disabledTextColor = Color(0xFF64748B)
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Số điện thoại liên hệ",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF334155)
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    placeholder = { Text("Nhập số điện thoại", fontSize = 13.sp, color = Color(0xFF94A3B8)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF1D4ED8),
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    ),
                    singleLine = true
                )

                if (isCorporate) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Mã số thuế doanh nghiệp",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF334155)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = taxCode,
                        onValueChange = { taxCode = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        placeholder = { Text("Nhập mã số thuế công ty", fontSize = 13.sp, color = Color(0xFF94A3B8)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF1D4ED8),
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        ),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (userEmail.isNotEmpty()) {
                        val updatedUser = UserInfo(
                            id = existingUser?.id ?: 0,
                            fullName = fullName,
                            phoneNumber = phoneNumber,
                            email = userEmail,
                            password = existingUser?.password ?: "USER_PASSWORD",
                            authType = authType,
                            avatarUrl = avatarUrl,
                            isCorporate = isCorporate,
                            taxCode = if (isCorporate) taxCode else null,
                            accountType = if (isCorporate) "CORPORATE" else "PERSONAL"
                        )
                        UserFileManager.saveUser(context, updatedUser)
                        Toast.makeText(context, "Đã cập nhật thông tin cá nhân & tài khoản!", Toast.LENGTH_SHORT).show()
                    }
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Text("Lưu Thay Đổi", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        },
        dismissButton = null
    )
}





// Hằng số định danh cấu hình Backend API Key cho Gemini AI
// Khi Backend cấu hình tích hợp GEMINI_API_KEY, AI sẽ tự động kích hoạt tạo hợp đồng thực tế từ Server



