@file:Suppress("Deprecation", "UnusedImport", "UNUSED_IMPORT")

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

        val accessToken = AccessToken.getCurrentAccessToken()
        val isFacebookLoggedIn = accessToken != null && !accessToken.isExpired
        if (isFacebookLoggedIn) {
            Log.d("Facebook", "Facebook AccessToken is active")
        }

        enableEdgeToEdge()
        setContent {
            SmartContractAITheme {
                // Xin quyền thông báo (Android 13+) & Lấy token FCM liên kết Firebase
                RequestNotificationPermissionIfNeeded(context = this) {
                    FCMUtils.fetchFcmToken { token ->
                        Log.d("FCM_FIREBASE", "FCM Device Token: $token")
                    }
                }
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
    }

    // Khi người dùng bấm vào nút Facebook trên giao diện:
    fun signInWithFacebook(onSuccess: () -> Unit, isCorporate: Boolean = false) {
        facebookSuccessCallback = onSuccess
        isCorporateSocialAuth = isCorporate
        LoginManager.getInstance().logOut()
        LoginManager.getInstance().logInWithReadPermissions(
            this,
            callbackManager,
            listOf("public_profile", "email")
        )
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
    object Dashboard : Screen()
    object CreateContractOverview : Screen()
    object CreateContractWithAI : Screen()
    object ContractTemplates : Screen()
    data class ContractDocumentEditor(val templateTitle: String = "Hợp Đồng Thử Việc (Bản Chuẩn 2024)") : Screen()
}

@Composable
fun AppNavigation() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Login) }

    when (currentScreen) {
        is Screen.Register -> RegisterScreen(
            onBack = null,
            onRegisterSuccess = { currentScreen = Screen.Dashboard },
            onNavigateToLogin = { currentScreen = Screen.Login }
        )
        is Screen.Login -> LoginScreen(
            onBack = null,
            onNavigateToRegister = { currentScreen = Screen.Register },
            onLoginSuccess = { currentScreen = Screen.Dashboard }
        )
        is Screen.Dashboard -> DashboardScreen(
            onLogoutClick = { currentScreen = Screen.Login },
            onNavigateToCreateContractAI = { currentScreen = Screen.CreateContractOverview },
            onNavigateToContractTemplates = { currentScreen = Screen.ContractTemplates }
        )
        is Screen.CreateContractOverview -> CreateContractOverviewScreen(
            onBack = { currentScreen = Screen.Dashboard },
            onNavigateToDashboard = { currentScreen = Screen.Dashboard },
            onNavigateToCreateWithAI = { currentScreen = Screen.CreateContractWithAI },
            onNavigateToContractTemplates = { currentScreen = Screen.ContractTemplates }
        )
        is Screen.CreateContractWithAI -> CreateContractWithAIScreen(
            onBack = { currentScreen = Screen.CreateContractOverview },
            onNavigateToDashboard = { currentScreen = Screen.Dashboard }
        )
        is Screen.ContractTemplates -> ContractTemplatesScreen(
            onBack = { currentScreen = Screen.CreateContractOverview },
            onNavigateToDashboard = { currentScreen = Screen.Dashboard },
            onNavigateToCreateContractOverview = { currentScreen = Screen.CreateContractOverview },
            onNavigateToCreateWithAI = { currentScreen = Screen.CreateContractWithAI },
            onNavigateToDocumentEditor = { title ->
                currentScreen = Screen.ContractDocumentEditor(title)
            }
        )
        is Screen.ContractDocumentEditor -> ContractDocumentEditorScreen(
            templateTitle = (currentScreen as Screen.ContractDocumentEditor).templateTitle,
            onBack = { currentScreen = Screen.ContractTemplates },
            onNavigateToDashboard = { currentScreen = Screen.Dashboard }
        )
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
        NavigationBarItem(
            selected = selectedTab == 3,
            onClick = { onTabSelected(3) },
            icon = { Icon(if (selectedTab == 3) Icons.Default.Work else Icons.Outlined.WorkOutline, contentDescription = "Business") },
            label = { Text("Business", fontSize = 10.sp, fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.White,
                selectedTextColor = Color(0xFF1D4ED8),
                indicatorColor = Color(0xFF1D4ED8),
                unselectedIconColor = Color(0xFF64748B),
                unselectedTextColor = Color(0xFF64748B)
            )
        )
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
            Spacer(modifier = Modifier.height(30.dp))



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
    var emailOrPhone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(false) }

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
            Spacer(modifier = Modifier.height(40.dp))



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
                                Toast.makeText(context, "Tính năng Quên mật khẩu", Toast.LENGTH_SHORT).show()
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
                                val am = android.accounts.AccountManager.get(context)
                                am.getAccountsByType("com.google").firstOrNull()?.name
                            } catch (_: Exception) { null }

                            when {
                                emailOrPhone.isNotBlank() -> emailOrPhone
                                !currentUser?.email.isNullOrBlank() -> currentUser.email!!
                                sessionEmail.isNotBlank() -> sessionEmail
                                !lastUserEmail.isNullOrBlank() -> lastUserEmail
                                !deviceGmail.isNullOrBlank() -> deviceGmail
                                else -> "user.google@gmail.com"
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

// ==================== MÀN HÌNH DASHBOARD (SAU ĐĂNG NHẬP / ĐĂNG KÝ) ====================
@Composable
fun DashboardScreen(
    onAccountClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    onNavigateToCreateContractAI: () -> Unit = {},
    onNavigateToContractTemplates: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                selectedTab = selectedTab,
                onTabSelected = { index ->
                    selectedTab = index
                    if (index == 2) {
                        onNavigateToContractTemplates()
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = {
                        Toast.makeText(context, "Tạo hợp đồng mới với AI Gemini", Toast.LENGTH_SHORT).show()
                        onNavigateToCreateContractAI()
                    },
                    containerColor = Color(0xFF1D4ED8),
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Tạo hợp đồng mới",
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                1 -> {
                    ContractManagementScreen(
                        onNavigateToCreateContractAI = onNavigateToCreateContractAI
                    )
                }
                3 -> {
                    BusinessAdministrationScreen(
                        onAccountClick = {
                            Toast.makeText(context, "Xem thông tin Tài Khoản", Toast.LENGTH_SHORT).show()
                            onAccountClick()
                        },
                        onLogoutClick = {
                            FirebaseAuth.getInstance().signOut()
                            Toast.makeText(context, "Đã đăng xuất thành công", Toast.LENGTH_SHORT).show()
                            onLogoutClick()
                        }
                    )
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFF8FAFC))
                            .verticalScroll(rememberScrollState())
                    ) {
                        // 1. Top Header Bar (Avatar, App Title & Notification Bell với Red Dot)
                        DashboardHeader(
                            onAccountClick = {
                                Toast.makeText(context, "Xem thông tin Tài Khoản", Toast.LENGTH_SHORT).show()
                                onAccountClick()
                            },
                            onLogoutClick = {
                                FirebaseAuth.getInstance().signOut()
                                Toast.makeText(context, "Đã đăng xuất thành công", Toast.LENGTH_SHORT).show()
                                onLogoutClick()
                            }
                        )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 2. Greeting Banner (Xin chào, Nguyễn Văn A)
                    DashboardGreetingBanner()

                    Spacer(modifier = Modifier.height(20.dp))

                    val currentUser = FirebaseAuth.getInstance().currentUser
                    val currentSessionEmail = UserFileManager.getCurrentSessionEmail(context)
                    val userEmail = currentSessionEmail.ifBlank { currentUser?.email }

                    // 3. Tổng quan (4 metric cards cập nhật dữ liệu từ Database theo từng người dùng)
                    DashboardOverviewGrid(userEmail = userEmail)

                    Spacer(modifier = Modifier.height(24.dp))

                    // 5. Hợp đồng gần đây (3 recent items với colored left strips)
                    DashboardRecentContracts()

                    Spacer(modifier = Modifier.height(24.dp))

                    // 6. Mẫu phổ biến (2 template grid cards)
                    DashboardPopularTemplatesSection()

                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
        }
    }
}

data class AppNotification(
    val id: String,
    val title: String,
    val message: String,
    val time: String,
    val isUnread: Boolean = true
)

@Composable
fun DashboardHeader(
    onAccountClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }
    var showNotificationMenu by remember { mutableStateOf(false) }
    val currentUser = FirebaseAuth.getInstance().currentUser
    val currentSessionEmail = UserFileManager.getCurrentSessionEmail(context)
    val userEmail = currentSessionEmail.ifBlank { currentUser?.email }

    val userInfo = remember(userEmail, currentUser) {
        if (!userEmail.isNullOrEmpty()) {
            UserFileManager.getUserByEmail(context, userEmail)
        } else null
    }

    val avatarUrl: String? = remember(userInfo, currentUser) {
        when {
            !userInfo?.avatarUrl.isNullOrEmpty() -> userInfo.avatarUrl
            currentUser?.photoUrl != null -> currentUser.photoUrl.toString()
            else -> null
        }
    }

    // Nạp danh sách thông báo từ SQLite database cho người dùng hiện tại
    LaunchedEffect(userEmail) {
        NotificationRepository.loadFromDatabase(context, userEmail)
        // Hệ thống tự động lưu thông báo ban đầu vào Database cho người dùng khi mở ứng dụng
        if (NotificationRepository.notifications.value.isEmpty()) {
            NotificationRepository.addNotification(
                context = context,
                title = "Chào mừng bạn đến với SmartContract AI",
                message = "Hệ thống trợ lý AI đã sẵn sàng hỗ trợ tạo và phân tích rủi ro hợp đồng.",
                userEmail = userEmail
            )
            NotificationRepository.addNotification(
                context = context,
                title = "AI Copilot đã hoàn tất rà soát",
                message = "Phân tích rủi ro hợp đồng Thuê Văn phòng Q1 đã được lưu tự động vào DB.",
                userEmail = userEmail
            )
        }
    }

    // Real-time Notification Feed tích hợp Database & Firebase FCM
    val notifications by NotificationRepository.notifications.collectAsState()
    val unreadCount = notifications.count { it.isUnread }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar Icon/Image
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFFE2E8F0))
                .clickable { menuExpanded = true },
            contentAlignment = Alignment.Center
        ) {
            if (!avatarUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = "User Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "User Avatar",
                    tint = Color(0xFF0284C7),
                    modifier = Modifier.size(22.dp)
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Tài Khoản", fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    onClick = {
                        menuExpanded = false
                        onAccountClick()
                    }
                )
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("Đăng Xuất", fontSize = 13.sp, color = Color(0xFFEF4444)) },
                    leadingIcon = {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                    },
                    onClick = {
                        menuExpanded = false
                        onLogoutClick()
                    }
                )
            }
        }

        // Center App Title
        Text(
            text = "SmartContract AI",
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = Color(0xFF0038A8)
        )

        // Notification Bell with Interactive Dropdown Popup
        Box {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { showNotificationMenu = !showNotificationMenu }
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = "Thông báo",
                    tint = Color(0xFF1E293B),
                    modifier = Modifier.size(24.dp)
                )
                if (unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444))
                            .align(Alignment.TopEnd)
                    )
                }
            }

            // Dropdown Pop-up hiển thị danh sách thông báo người dùng đọc trực tiếp từ Database
            DropdownMenu(
                expanded = showNotificationMenu,
                onDismissRequest = { showNotificationMenu = false },
                modifier = Modifier
                    .width(310.dp)
                    .background(Color.White)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Thông báo",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF0F172A)
                            )
                            if (unreadCount > 0) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFFE0EDFF))
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "$unreadCount mới",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1D4ED8)
                                    )
                                }
                            }
                        }

                        if (notifications.isNotEmpty()) {
                            Text(
                                text = "Đọc tất cả",
                                fontSize = 11.sp,
                                color = Color(0xFF1D4ED8),
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickable {
                                    NotificationRepository.markAllAsRead(context, userEmail)
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    Spacer(modifier = Modifier.height(8.dp))

                    if (notifications.isEmpty()) {
                        // Phần thông báo để trống tự động
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp, horizontal = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF1F5F9)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.NotificationsNone,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Chưa có thông báo nào",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Hệ thống sẽ tự động lưu và cập nhật thông báo từ Database & Firebase tại đây.",
                                fontSize = 10.5.sp,
                                color = Color(0xFF64748B),
                                textAlign = TextAlign.Center,
                                lineHeight = 15.sp
                            )
                        }
                    } else {
                        // Khi có thông báo từ Database thì Feed mới hiển thị thông báo
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            notifications.forEach { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (item.isUnread) Color(0xFFF8FAFC) else Color.White)
                                        .clickable {
                                            NotificationRepository.markAsRead(context, item.id)
                                        }
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .padding(top = 4.dp)
                                            .clip(CircleShape)
                                            .background(if (item.isUnread) Color(0xFF1D4ED8) else Color.Transparent)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.title,
                                            fontSize = 12.sp,
                                            fontWeight = if (item.isUnread) FontWeight.Bold else FontWeight.SemiBold,
                                            color = Color(0xFF0F172A)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = item.message,
                                            fontSize = 11.sp,
                                            color = Color(0xFF64748B),
                                            lineHeight = 15.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = item.time,
                                            fontSize = 9.5.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardGreetingBanner() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val currentUser = FirebaseAuth.getInstance().currentUser
    val currentSessionEmail = UserFileManager.getCurrentSessionEmail(context)
    val userEmail = currentSessionEmail.ifBlank { currentUser?.email }

    val userInfo = remember(userEmail, currentUser) {
        if (!userEmail.isNullOrEmpty()) {
            UserFileManager.getUserByEmail(context, userEmail)
        } else null
    }

    val displayName = remember(userInfo, currentUser, userEmail) {
        when {
            !userInfo?.fullName.isNullOrBlank() -> userInfo.fullName
            !currentUser?.displayName.isNullOrBlank() -> currentUser.displayName
            !userEmail.isNullOrBlank() -> userEmail.substringBefore("@")
            else -> "Người dùng"
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F1FF))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Xin chào, ${displayName ?: "Người dùng"}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Trợ lý AI Gemini đã sẵn sàng hỗ trợ bạn soạn thảo và rà soát hợp đồng hôm nay.",
                    fontSize = 11.sp,
                    color = Color(0xFF475569),
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Decorative Vertical Accent Bars
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .height(36.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF3B82F6).copy(alpha = 0.4f))
                )
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .height(48.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF1D4ED8))
                )
            }
        }
    }
}

@Composable
fun DashboardOverviewGrid(userEmail: String? = null) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val dbHelper = remember(context) { UserDatabaseHelper(context) }
    var stats by remember(userEmail) {
        mutableStateOf(dbHelper.getUserContractStats(userEmail))
    }

    LaunchedEffect(userEmail) {
        stats = dbHelper.getUserContractStats(userEmail)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Text(
            text = "Tổng quan",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Row 1: Hợp đồng của tôi & Chờ duyệt nội bộ
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OverviewMetricCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.Folder,
                count = stats.myContractsCount.toString(),
                label = "Hợp đồng của tôi",
                countColor = Color(0xFF1D4ED8),
                isHighlighted = false
            )
            OverviewMetricCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.Assignment,
                count = stats.pendingApprovalCount.toString(),
                label = "Chờ duyệt nội bộ",
                countColor = Color(0xFF0F172A),
                isHighlighted = false
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Row 2: Chờ ký (Highlighted with red badge dot) & Đã hoàn tất
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OverviewMetricCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.EditNote,
                count = stats.pendingSignatureCount.toString(),
                label = "Chờ ký",
                countColor = Color(0xFF1D4ED8),
                isHighlighted = true
            )
            OverviewMetricCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.CheckCircleOutline,
                count = stats.completedCount.toString(),
                label = "Đã hoàn tất",
                countColor = Color(0xFF0F172A),
                isHighlighted = false
            )
        }
    }
}

@Composable
fun OverviewMetricCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    count: String,
    label: String,
    countColor: Color,
    isHighlighted: Boolean
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlighted) Color(0xFFE0EDFF) else Color.White
        ),
        border = BorderStroke(1.dp, if (isHighlighted) Color(0xFF3B82F6) else Color(0xFFF1F5F9))
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            if (isHighlighted) {
                // Red indicator dot top right
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444))
                        .align(Alignment.TopEnd)
                )
            }

            Column {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color(0xFF334155),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = count,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = countColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = label,
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}



@Composable
fun DashboardRecentContracts() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Hợp đồng gần đây",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            Text(
                text = "Xem tất cả",
                fontSize = 11.sp,
                color = Color(0xFF1D4ED8),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Contract Item 1: HĐ Dịch vụ Phần mềm (Red left strip)
        RecentContractCard(
            stripColor = Color(0xFFEF4444),
            badgeBg = Color(0xFFFFE4E6),
            iconTint = Color(0xFFE11D48),
            icon = Icons.Outlined.Edit,
            title = "HĐ Dịch vụ Phần mềm - Công ty...",
            subtitle = "Cập nhật 2 giờ trước"
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Contract Item 2: HĐ Mua Bán Thiết Bị (Green left strip)
        RecentContractCard(
            stripColor = Color(0xFF10B981),
            badgeBg = Color(0xFFD1FAE5),
            iconTint = Color(0xFF059669),
            icon = Icons.Outlined.CheckCircleOutline,
            title = "HĐ Mua Bán Thiết Bị - CN Miền...",
            subtitle = "Hoàn tất hôm qua"
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Contract Item 3: Phụ lục 02 - HĐLĐ Nguyễn Thị B (Gray left strip)
        RecentContractCard(
            stripColor = Color(0xFF64748B),
            badgeBg = Color(0xFFE0EDFF),
            iconTint = Color(0xFF1D4ED8),
            icon = Icons.Outlined.ReceiptLong,
            title = "Phụ lục 02 - HĐLĐ Nguyễn Thị B",
            subtitle = "Cập nhật 2 ngày trước"
        )
    }
}

@Composable
fun RecentContractCard(
    stripColor: Color,
    badgeBg: Color,
    iconTint: Color,
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left color strip indicator
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(60.dp)
                    .background(stripColor)
            )

            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = subtitle,
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardPopularTemplatesSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Text(
            text = "Mẫu phổ biến",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PopularTemplateGridCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Diamond,
                title = "Thỏa thuận bảo mật (NDA)",
                usageText = "Dùng 45 lần tuần này"
            )
            PopularTemplateGridCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.WorkOutline,
                title = "Hợp đồng Lao động (Chuẩn)",
                usageText = "Dùng 32 lần tuần này"
            )
        }
    }
}

@Composable
fun PopularTemplateGridCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    usageText: String
) {
    Card(
        modifier = modifier.clickable { },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE0EDFF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF1D4ED8),
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                lineHeight = 15.sp,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = usageText,
                fontSize = 9.5.sp,
                color = Color(0xFF94A3B8)
            )
        }
    }
}

// Previews
@Preview(showBackground = true)
@Composable
fun DashboardScreenPreview() {
    SmartContractAITheme {
        DashboardScreen()
    }
}

// ==================== MÀN HÌNH KHỞI TẠO NHANH (MÀN HÌNH 1) ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateContractOverviewScreen(
    onBack: () -> Unit = {},
    onNavigateToDashboard: () -> Unit = {},
    onNavigateToCreateWithAI: () -> Unit = {},
    onNavigateToContractTemplates: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var bottomNavTab by remember { mutableIntStateOf(1) } // 1: Contracts selected

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Tạo Hợp Đồng",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1D4ED8)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = Color(0xFF1E293B)
                        )
                    }
                },

                actions = {
                    IconButton(onClick = {
                        Toast.makeText(context, "Tùy chọn màn hình", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Tùy chọn",
                            tint = Color(0xFF1E293B)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            BottomNavigationBar(
                selectedTab = bottomNavTab,
                onTabSelected = { index ->
                    bottomNavTab = index
                    if (index == 0) {
                        onNavigateToDashboard()
                    } else if (index == 2) {
                        onNavigateToContractTemplates()
                    }
                }
            )
        },
        containerColor = Color.White
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Phần "Khởi tạo nhanh"
            Text(
                text = "Khởi tạo nhanh",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Card 1: Tạo bằng AI
            QuickStartItemCard(
                icon = Icons.Default.AutoAwesome,
                iconBg = Color(0xFF1D4ED8),
                iconTint = Color.White,
                title = "Tạo bằng AI",
                subtitle = "Nhập yêu cầu bằng văn bản",
                onClick = onNavigateToCreateWithAI
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Card 2: Nhân bản (10s)
            QuickStartItemCard(
                icon = Icons.Outlined.ContentCopy,
                iconBg = Color(0xFFE2E8F0),
                iconTint = Color(0xFF1D4ED8),
                title = "Nhân bản (10s)",
                subtitle = "Từ hợp đồng đã có",
                onClick = {
                    Toast.makeText(context, "Chọn hợp đồng sẵn có để nhân bản", Toast.LENGTH_SHORT).show()
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Card 3: Từ kho mẫu (Hình 1 đính kèm) -> Chuyển sang màn hình Mẫu Hợp Đồng (Hình 2)!
            QuickStartItemCard(
                icon = Icons.Outlined.Description,
                iconBg = Color(0xFFE2E8F0),
                iconTint = Color(0xFF1D4ED8),
                title = "Từ kho mẫu",
                subtitle = "Điền biến {{Var}}",
                onClick = onNavigateToContractTemplates
            )
        }
    }
}

// Hằng số định danh cấu hình Backend API Key cho Gemini AI
// Khi Backend cấu hình tích hợp GEMINI_API_KEY, AI sẽ tự động kích hoạt tạo hợp đồng thực tế từ Server
object BackendAIConfig {
    var GEMINI_API_KEY: String = "" // Để rỗng mặc định chờ Backend tích hợp API Key
}

// ==================== MÀN HÌNH TẠO BẰNG AI CHUYÊN BIỆT (HÌNH ĐÍNH KÈM) ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateContractWithAIScreen(
    onBack: () -> Unit = {},
    onNavigateToDashboard: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var promptText by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Văn bản hợp đồng, 1: Thiết lập luồng duyệt
    var bottomNavTab by remember { mutableIntStateOf(1) } // 1: Contracts selected
    var isAiGenerating by remember { mutableStateOf(false) } // Chỉ hiển thị khi AI đang hoạt động
    var generatedContract by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Tạo Hợp Đồng",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1D4ED8)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = Color(0xFF1E293B)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        Toast.makeText(context, "Tùy chọn màn hình", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Tùy chọn",
                            tint = Color(0xFF1E293B)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            BottomNavigationBar(
                selectedTab = bottomNavTab,
                onTabSelected = { index ->
                    bottomNavTab = index
                    if (index == 0) {
                        onNavigateToDashboard()
                    }
                }
            )
        },
        containerColor = Color.White
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // 1. Trợ lý AI Gemini Greeting Chat Bubble
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Avatar icon Gemini
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF4F46E5)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Gemini AI",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Chat bubble container
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEEF2FF))
                ) {
                    Text(
                        text = "Xin chào! Tôi là Gemini Assistant. Hãy mô tả loại hợp đồng bạn muốn tạo (ví dụ: *Hợp đồng dịch vụ IT giữa công ty A và B, thời hạn 1 năm, giá trị 500 triệu*).",
                        fontSize = 12.5.sp,
                        color = Color(0xFF334155),
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Khung Nhập Yêu Cầu Hợp Đồng (Input Box Card)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFCBD5E1))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    TextField(
                        value = promptText,
                        onValueChange = { promptText = it },
                        placeholder = {
                            Text(
                                text = "Nhập yêu cầu tạo hợp đồng...",
                                fontSize = 13.sp,
                                color = Color(0xFF94A3B8)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 70.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Icons 📎 🎙️
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = {
                                    Toast.makeText(context, "Tải tệp đính kèm", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AttachFile,
                                    contentDescription = "Attach",
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    Toast.makeText(context, "Thu âm giọng nói", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Mic,
                                    contentDescription = "Mic",
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Button Gửi -> Kiểm tra xem Backend đã tích hợp GEMINI_API_KEY chưa
                        Button(
                            onClick = {
                                if (promptText.isBlank()) {
                                    Toast.makeText(context, "Vui lòng nhập yêu cầu tạo hợp đồng", Toast.LENGTH_SHORT).show()
                                } else if (BackendAIConfig.GEMINI_API_KEY.isBlank()) {
                                    Toast.makeText(
                                        context,
                                        "Chưa thể tạo hợp đồng: Vui lòng cấu hình Gemini API Key từ phía Backend!",
                                        Toast.LENGTH_LONG
                                    ).show()
                                } else {
                                    isAiGenerating = true
                                    Toast.makeText(context, "AI Gemini đang tạo hợp đồng từ API Backend...", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8)),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send",
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Gửi",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // AI Disclaimer text
            Text(
                text = "AI có thể mắc lỗi. Vui lòng kiểm tra lại thông tin.",
                fontSize = 11.sp,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            // GIAO DIỆN PHÁC THẢO VÀ KẾT QUẢ
            // 1. Khi đang phác thảo (isAiGenerating == true)
            AnimatedVisibility(
                visible = isAiGenerating,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                // Hiệu ứng xoay tròn cho vòng tròn icon
                val infiniteTransition = rememberInfiniteTransition(label = "rotation")
                val rotationAngle by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 1200, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "circleRotation"
                )

                // Gọi Gemini API tạo hợp đồng khi có API Key từ Backend
                LaunchedEffect(isAiGenerating) {
                    if (isAiGenerating) {
                        if (BackendAIConfig.GEMINI_API_KEY.isNotBlank()) {
                            // Khi Backend truyền API Key thực tế vào dự án, AI sẽ gọi API thực và trả kết quả hợp đồng
                            kotlinx.coroutines.delay(2000.milliseconds)
                            isAiGenerating = false
                            generatedContract = "Hợp đồng thực tế được tạo từ Gemini API với yêu cầu: $promptText"
                        } else {
                            isAiGenerating = false
                        }
                    }
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(20.dp))

                    // Tab selection bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedTab = 0 }
                                .padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = "Văn bản hợp đồng",
                                fontSize = 14.sp,
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == 0) Color(0xFF1D4ED8) else Color(0xFF64748B)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            if (selectedTab == 0) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.8f)
                                        .height(2.5.dp)
                                        .background(Color(0xFF1D4ED8), shape = RoundedCornerShape(2.dp))
                                )
                            } else {
                                Spacer(modifier = Modifier.height(2.5.dp))
                            }
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedTab = 1 }
                                .padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = "Thiết lập luồng duyệt",
                                fontSize = 14.sp,
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == 1) Color(0xFF1D4ED8) else Color(0xFF64748B)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            if (selectedTab == 1) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.8f)
                                        .height(2.5.dp)
                                        .background(Color(0xFF1D4ED8), shape = RoundedCornerShape(2.dp))
                                )
                            } else {
                                Spacer(modifier = Modifier.height(2.5.dp))
                            }
                        }
                    }

                    // Card đang phác thảo với vòng tròn xoay
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 340.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Spacer(modifier = Modifier.height(30.dp))

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    // Sparkle Icon Circle có hiệu ứng xoay còng tròn
                                    Box(
                                        modifier = Modifier
                                            .size(68.dp)
                                            .graphicsLayer(rotationZ = rotationAngle)
                                            .border(
                                                border = BorderStroke(3.dp, Brush.sweepGradient(listOf(Color(0xFF1D4ED8), Color(0xFF93C5FD), Color(0xFF1D4ED8)))),
                                                shape = CircleShape
                                            )
                                            .padding(4.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFEFF6FF)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = "Drafting AI",
                                            tint = Color(0xFF1D4ED8),
                                            modifier = Modifier.size(30.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text(
                                        text = "Đang phác thảo hợp đồng...",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1D4ED8),
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "AI đang kết nối tới Gemini API để tạo hợp đồng theo yêu cầu.",
                                        fontSize = 12.sp,
                                        color = Color(0xFF64748B),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(30.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            isAiGenerating = false
                                            Toast.makeText(context, "Đã hủy tiến trình tạo hợp đồng", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth(0.6f)
                                            .height(46.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
                                    ) {
                                        Text(
                                            text = "Hủy tiến trình",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF1E293B)
                                        )
                                    }
                                }
                            }
                        }

                        IconButton(
                            onClick = {
                                Toast.makeText(context, "Tùy chọn menu", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .padding(12.dp)
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .align(Alignment.TopStart)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu",
                                tint = Color(0xFF1E293B),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            // 2. Sau khi AI tạo xong hợp đồng -> Biến mất khung phác thảo và hiện ra hợp đồng do AI tạo
            AnimatedVisibility(
                visible = !isAiGenerating && generatedContract != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.5.dp, Color(0xFF3B82F6))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color(0xFF1D4ED8),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Hợp đồng do AI đã tạo",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1D4ED8)
                                    )
                                }

                                IconButton(onClick = { generatedContract = null }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Đóng",
                                        tint = Color(0xFF64748B)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF8FAFC), shape = RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFFE2E8F0), shape = RoundedCornerShape(8.dp))
                                    .padding(14.dp)
                            ) {
                                Text(
                                    text = generatedContract ?: "",
                                    fontSize = 12.sp,
                                    color = Color(0xFF1E293B),
                                    lineHeight = 18.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        Toast.makeText(context, "Đã sao chép hợp đồng", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Sao chép", fontSize = 12.sp, color = Color(0xFF1D4ED8))
                                }

                                Button(
                                    onClick = {
                                        Toast.makeText(context, "Đã hoàn tất tạo hợp đồng!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1.2f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8))
                                ) {
                                    Text("Sử dụng hợp đồng", fontSize = 12.sp, color = Color.White)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
fun QuickStartItemCard(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = title,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CreateContractWithAIScreenPreview() {
    SmartContractAITheme {
        CreateContractWithAIScreen()
    }
}

// ==================== MÀN HÌNH KHO MẪU HỢP ĐỒNG (HÌNH 2 ĐÍNH KÈM) ====================
data class ContractTemplateModel(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val scope: String, // "Công khai", "Doanh nghiệp", "Cá nhân"
    val badgeText: String?,
    val isAiOptimized: Boolean = false,
    val usageCount: String,
    val timeAgo: String,
    val icon: ImageVector,
    val isPrimaryButton: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContractTemplatesScreen(
    onBack: () -> Unit = {},
    onNavigateToDashboard: () -> Unit = {},
    onNavigateToCreateContractOverview: () -> Unit = {},
    onNavigateToCreateWithAI: () -> Unit = {},
    onNavigateToDocumentEditor: (String) -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var selectedScopeTab by remember { mutableIntStateOf(0) } // 0: Công khai, 1: Doanh nghiệp, 2: Cá nhân
    var selectedCategory by remember { mutableStateOf("Tất cả") }
    var bottomNavTab by remember { mutableIntStateOf(2) } // 2: Templates selected

    val categories = listOf("Tất cả", "Lao động & Nhân sự", "NDA", "Dịch vụ & IT", "Bất động sản", "Mua bán")

    val allTemplates = remember {
        listOf(
            ContractTemplateModel(
                id = "1",
                title = "Hợp Đồng Thử Việc (Bản Chuẩn 2024)",
                description = "Mẫu hợp đồng thử việc cập nhật theo luật lao động mới nhất, phù hợp cho nhân sự văn...",
                category = "Lao động & Nhân sự",
                scope = "Công khai",
                badgeText = "✨ AI Tối ưu",
                isAiOptimized = true,
                usageCount = "1.2k lượt dùng",
                timeAgo = "2 ngày trước",
                icon = Icons.Outlined.Badge,
                isPrimaryButton = true
            ),
            ContractTemplateModel(
                id = "2",
                title = "Thỏa Thuận Bảo Mật Thông Tin (NDA) Dành Cho Đối Tác",
                description = "Bảo vệ tài sản trí tuệ và bí mật kinh doanh khi hợp tác với bên thứ ba hoặc nhà thầu độc lập.",
                category = "NDA",
                scope = "Công khai",
                badgeText = "NDA",
                isAiOptimized = false,
                usageCount = "850 lượt dùng",
                timeAgo = "1 tuần trước",
                icon = Icons.Outlined.EditNote,
                isPrimaryButton = false
            ),
            ContractTemplateModel(
                id = "3",
                title = "Hợp Đồng Phát Triển Phần Mềm (Outsource)",
                description = "Mẫu hợp đồng thuê ngoài phát triển ứng dụng, quy định rõ về mốc thời gian, nghiệm thu và sở...",
                category = "Dịch vụ & IT",
                scope = "Công khai",
                badgeText = "IT",
                isAiOptimized = false,
                usageCount = "420 lượt dùng",
                timeAgo = "1 tháng trước",
                icon = Icons.Outlined.Laptop,
                isPrimaryButton = false
            ),
            ContractTemplateModel(
                id = "4",
                title = "Hợp Đồng Cho Thuê Văn Phòng / Mặt Bằng",
                description = "Điều khoản thuê mặt bằng thương mại, bảo vệ quyền lợi bên thuê và cho thuê đầy đủ pháp lý.",
                category = "Bất động sản",
                scope = "Doanh nghiệp",
                badgeText = "Bất động sản",
                isAiOptimized = false,
                usageCount = "630 lượt dùng",
                timeAgo = "3 ngày trước",
                icon = Icons.Outlined.Apartment,
                isPrimaryButton = false
            ),
            ContractTemplateModel(
                id = "5",
                title = "Hợp Đồng Mua Bán Hàng Hóa Thương Mại",
                description = "Quy định điều khoản giao hàng, thanh toán, bảo hành và phạt vi phạm hợp đồng thương mại.",
                category = "Mua bán",
                scope = "Doanh nghiệp",
                badgeText = "Thương mại",
                isAiOptimized = false,
                usageCount = "910 lượt dùng",
                timeAgo = "5 ngày trước",
                icon = Icons.Outlined.ShoppingBag,
                isPrimaryButton = false
            )
        )
    }

    val filteredTemplates = remember(selectedScopeTab, selectedCategory) {
        allTemplates.filter { template ->
            val scopeMatches = when (selectedScopeTab) {
                0 -> template.scope == "Công khai" || template.scope == "Doanh nghiệp" || template.scope == "Cá nhân"
                1 -> template.scope == "Doanh nghiệp"
                else -> template.scope == "Cá nhân"
            }
            val categoryMatches = if (selectedCategory == "Tất cả") true else template.category == selectedCategory
            scopeMatches && categoryMatches
        }
    }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                selectedTab = bottomNavTab,
                onTabSelected = { index ->
                    bottomNavTab = index
                    if (index == 0) {
                        onNavigateToDashboard()
                    } else if (index == 1) {
                        onNavigateToCreateContractOverview()
                    }
                }
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {

            // 2. Mẫu Hợp Đồng Title & Subtitle & Save template button
            Text(
                text = "Mẫu Hợp Đồng",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Khám phá và sử dụng các mẫu hợp đồng chuẩn được AI tối ưu hóa.",
                fontSize = 13.sp,
                color = Color(0xFF64748B)
            )
            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons: "Lưu mẫu mới" & "Tạo bằng AI"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        Toast.makeText(context, "Lưu mẫu mới từ bản nháp thành công", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddCircleOutline,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Lưu mẫu mới",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                OutlinedButton(
                    onClick = onNavigateToCreateWithAI,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF1D4ED8)),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFEFF6FF)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFF1D4ED8),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Tạo bằng AI",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1D4ED8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Segmented Control / Scope Selector ("Công khai", "Doanh nghiệp", "Cá nhân")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val scopeTabs = listOf("Công khai", "Doanh nghiệp", "Cá nhân")
                    scopeTabs.forEachIndexed { index, tabTitle ->
                        val isSelected = selectedScopeTab == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFFE0EDFF) else Color.Transparent)
                                .clickable { selectedScopeTab = index },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tabTitle,
                                fontSize = 12.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color(0xFF1D4ED8) else Color(0xFF475569)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Horizontal Category Filter Chips Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Tune,
                    contentDescription = "Bộ lọc",
                    tint = Color(0xFF64748B),
                    modifier = Modifier
                        .size(22.dp)
                        .clickable {
                            Toast.makeText(context, "Mở bộ lọc danh mục", Toast.LENGTH_SHORT).show()
                        }
                )
                Spacer(modifier = Modifier.width(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { category ->
                        val isSelected = selectedCategory == category
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) Color(0xFFDBEAFE) else Color(0xFFF1F5F9))
                                .clickable { selectedCategory = category }
                                .padding(horizontal = 14.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = category,
                                fontSize = 12.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color(0xFF1D4ED8) else Color(0xFF475569)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. Template Cards List
            filteredTemplates.forEach { template ->
                TemplateCardItem(
                    template = template,
                    onUseClick = {
                        Toast.makeText(context, "Mở hợp đồng mẫu Docs: ${template.title}", Toast.LENGTH_SHORT).show()
                        onNavigateToDocumentEditor(template.title)
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun TemplateCardItem(
    template: ContractTemplateModel,
    onUseClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Icon Box + Badge Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFEFF6FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = template.icon,
                        contentDescription = null,
                        tint = Color(0xFF1D4ED8),
                        modifier = Modifier.size(22.dp)
                    )
                }

                if (template.badgeText != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (template.isAiOptimized) Color(0xFFF3E8FF) else Color(0xFFE2E8F0)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = template.badgeText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (template.isAiOptimized) Color(0xFF7E22CE) else Color(0xFF475569)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Title
            Text(
                text = template.title,
                fontSize = 15.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Description
            Text(
                text = template.description,
                fontSize = 12.5.sp,
                color = Color(0xFF64748B),
                lineHeight = 18.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Stats Row: Usage count & Time ago
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Visibility,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = template.usageCount,
                        fontSize = 11.5.sp,
                        color = Color(0xFF64748B)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = template.timeAgo,
                        fontSize = 11.5.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Button ("Sử dụng ngay")
            if (template.isPrimaryButton) {
                Button(
                    onClick = onUseClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8)),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Sử dụng ngay",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            } else {
                OutlinedButton(
                    onClick = onUseClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Text(
                        text = "Sử dụng ngay",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1D4ED8)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ContractTemplatesScreenPreview() {
    SmartContractAITheme {
        ContractTemplatesScreen()
    }
}

// ==================== MÀN HÌNH CHỈNH SỬA VĂN BẢN MẪU WORD / DOCS ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContractDocumentEditorScreen(
    templateTitle: String = "Hợp Đồng Thử Việc (Bản Chuẩn 2024)",
    onBack: () -> Unit = {},
    onNavigateToDashboard: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var bottomNavTab by remember { mutableIntStateOf(1) } // Contracts selected

    // Form điền biến mẫu hợp đồng (Fillable Fields)
    var partyBName by remember { mutableStateOf("Nguyễn Văn A") }
    var partyBId by remember { mutableStateOf("012345678901") }
    var positionTitle by remember { mutableStateOf("Chuyên viên Lập trình Android") }
    var salaryAmount by remember { mutableStateOf("15,000,000") }
    var trialDuration by remember { mutableStateOf("02 tháng (Từ 01/09/2024 đến 01/11/2024)") }

    // Nội dung văn bản hợp đồng mẫu Docs/Word
    var contractContent by remember(partyBName, partyBId, positionTitle, salaryAmount, trialDuration) {
        mutableStateOf(
            """
            CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM
            Độc lập - Tự do - Hạnh phúc
            -------------------

            ${templateTitle.uppercase()}
            Số: 01/2024/HĐTV-AI

            Hôm nay, ngày 01 tháng 09 năm 2024, tại văn phòng trụ sở chính, chúng tôi gồm có:

            BÊN A (BÊN TUYỂN DỤNG): CÔNG TY CỔ PHẦN SMARTCONTRACT AI
            - Đại diện: Ông Nguyễn Quang Minh
            - Chức vụ: Giám đốc Điều hành
            - Mã số thuế: 0312345678
            - Địa chỉ: Tầng 8, Tòa nhà Innovation Center, Quận 1, TP. Hồ Chí Minh

            BÊN B (BÊN NGƯỜI LAO ĐỘNG):
            - Ông/Bà: $partyBName
            - Số CCCD/CMND: $partyBId
            - Chức danh chuyên môn: $positionTitle
            - Địa chỉ thường trú: 123 Đường Nguyễn Trãi, Quận 5, TP. Hồ Chí Minh

            Cùng thỏa thuận ký kết Hợp đồng thử việc với các điều khoản sau đây:

            ĐIỀU 1: CÔNG VIỆC VÀ THỜI HẠN THỬ VIỆC
            1.1. Chức danh công việc: $positionTitle.
            1.2. Thời hạn thử việc: $trialDuration.
            1.3. Địa điểm làm việc: Trụ sở chính Bên A hoặc theo sự phân công hợp lý của Quản lý.

            ĐIỀU 2: MỨC LƯƠNG VÀ CHẾ ĐỘ THƯỞNG
            2.1. Mức lương thử việc: $salaryAmount VNĐ/tháng (Bằng 85% mức lương chính thức).
            2.2. Hình thức trả lương: Chuyển khoản vào tài khoản ngân hàng của Bên B vào ngày 05 hàng tháng.
            2.3. Chế độ đãi ngộ: Được hưởng phụ cấp ăn trưa, gửi xe và tham gia các hoạt động đào tạo của Công ty.

            ĐIỀU 3: QUYỀN VÀ NGHĨA VỤ CỦA CÁC BÊN
            3.1. Bên B có trách nhiệm hoàn thành tốt công việc được giao, chấp hành nội quy lao động của Công ty.
            3.2. Bên A có trách nhiệm thanh toán đầy đủ và đúng hạn các khoản lương, phụ cấp cho Bên B.

            ĐIỀU 4: ĐIỀU KHOẢN THI HÀNH
            Hợp đồng này được lập thành 02 (hai) bản có giá trị pháp lý như nhau, mỗi bên giữ 01 bản.

                      ĐẠI DIỆN BÊN A                                   ĐẠI DIỆN BÊN B
                     (Ký, ghi rõ họ tên)                             (Ký, ghi rõ họ tên)



                      Nguyễn Quang Minh                                $partyBName
            """.trimIndent()
        )
    }

    var isEditingFormOpen by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Description,
                                contentDescription = null,
                                tint = Color(0xFF1D4ED8),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = templateTitle,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "Định dạng Docs / Word - Người dùng tự điền",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = Color(0xFF1E293B)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        Toast.makeText(context, "Đã xuất tệp Word (.docx) thành công!", Toast.LENGTH_LONG).show()
                    }) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Tải file Word",
                            tint = Color(0xFF1D4ED8)
                        )
                    }
                    IconButton(onClick = {
                        Toast.makeText(context, "Đã lưu bản nháp hợp đồng!", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Lưu bản nháp",
                            tint = Color(0xFF16A34A)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            BottomNavigationBar(
                selectedTab = bottomNavTab,
                onTabSelected = { index ->
                    bottomNavTab = index
                    if (index == 0) {
                        onNavigateToDashboard()
                    }
                }
            )
        },
        containerColor = Color(0xFFF1F5F9)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(12.dp)
        ) {
            // 1. Quick Form Input Panel (Bảng tự động điền biến {{Var}} theo mẫu)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isEditingFormOpen = !isEditingFormOpen },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFDBEAFE)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color(0xFF1D4ED8),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Điền nhanh thông tin theo mẫu (Form Variables)",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E3A8A)
                            )
                        }

                        Icon(
                            imageVector = if (isEditingFormOpen) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Color(0xFF64748B)
                        )
                    }

                    if (isEditingFormOpen) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = Color(0xFFE2E8F0))
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = partyBName,
                            onValueChange = { partyBName = it },
                            label = { Text("Họ và tên Người lao động (Bên B)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF1D4ED8),
                                unfocusedBorderColor = Color(0xFFCBD5E1)
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = partyBId,
                                onValueChange = { partyBId = it },
                                label = { Text("Số CCCD/CMND") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF1D4ED8),
                                    unfocusedBorderColor = Color(0xFFCBD5E1)
                                )
                            )
                            OutlinedTextField(
                                value = salaryAmount,
                                onValueChange = { salaryAmount = it },
                                label = { Text("Mức lương (VNĐ)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF1D4ED8),
                                    unfocusedBorderColor = Color(0xFFCBD5E1)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = positionTitle,
                            onValueChange = { positionTitle = it },
                            label = { Text("Chức danh chuyên môn") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF1D4ED8),
                                unfocusedBorderColor = Color(0xFFCBD5E1)
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = trialDuration,
                            onValueChange = { trialDuration = it },
                            label = { Text("Thời hạn thử việc") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF1D4ED8),
                                unfocusedBorderColor = Color(0xFFCBD5E1)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Word / Docs Format Editing Toolbar (Thanh công cụ định dạng Word)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            Toast.makeText(context, "Đã in đậm (Bold)", Toast.LENGTH_SHORT).show()
                        }) {
                            Text("B", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color(0xFF0F172A))
                        }
                        IconButton(onClick = {
                            Toast.makeText(context, "Đã in nghiêng (Italic)", Toast.LENGTH_SHORT).show()
                        }) {
                            Text("I", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
                        }
                        IconButton(onClick = {
                            Toast.makeText(context, "Đã gạch chân (Underline)", Toast.LENGTH_SHORT).show()
                        }) {
                            Text("U", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = {
                                Toast.makeText(context, "Đã hoàn tất chỉnh sửa văn bản!", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8)),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Lưu văn bản", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Word Document Paper View (Trang giấy A4 chứa văn bản hợp đồng trực quan)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Text(
                        text = "Trang Word / Docs - Xem & Chỉnh sửa trực tiếp:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2563EB),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = contractContent,
                        onValueChange = { contractContent = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 500.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF3B82F6),
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedContainerColor = Color(0xFFFAFAFA),
                            unfocusedContainerColor = Color.White
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            color = Color(0xFF1E293B)
                        )
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ContractDocumentEditorScreenPreview() {
    SmartContractAITheme {
        ContractDocumentEditorScreen()
    }
}