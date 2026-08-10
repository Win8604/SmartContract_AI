@file:Suppress("Deprecation")

package com.smartcontractai

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
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
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch

import android.app.NotificationChannel
import android.app.NotificationManager
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import com.google.firebase.messaging.FirebaseMessaging
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

class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private val callbackManager: CallbackManager = CallbackManager.Factory.create()
    private var facebookSuccessCallback: (() -> Unit)? = null
    private var googleSuccessCallback: (() -> Unit)? = null

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
                            saveGoogleUserToDb()
                            Toast.makeText(
                                this,
                                "Đăng nhập Google thành công",
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

    private fun saveGoogleUserToDb() {
        val user = auth.currentUser
        if (user != null) {
            val dbHelper = UserDatabaseHelper(this)
            val userEmail = user.email ?: ""
            if (userEmail.isNotEmpty()) {
                val userName = user.displayName ?: "Google User"
                val userInfo = UserInfo(
                    fullName = userName,
                    phoneNumber = "",
                    email = userEmail,
                    password = "GOOGLE_AUTH_USER",
                    authType = "GOOGLE"
                )
                UserFileManager.saveUser(this, userInfo)
                if (!dbHelper.isEmailExists(userEmail)) {
                    dbHelper.registerUser(
                        User(
                            fullName = userName,
                            phoneNumber = "",
                            email = userEmail,
                            password = "GOOGLE_AUTH_USER",
                            authType = "GOOGLE"
                        )
                    )
                }
            }
        }
    }

    // ==================== GOOGLE LOGIN ====================
    fun signInWithGoogle(onSuccess: () -> Unit) {
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
                                saveGoogleUserToDb()
                                Toast.makeText(
                                    this@MainActivity,
                                    "Đăng nhập Google thành công",
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
                    signInWithGoogleLegacy(onSuccess)
                }
            } catch (_: GetCredentialCancellationException) {
                Toast.makeText(
                    this@MainActivity,
                    "Bạn đã hủy đăng nhập Google",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (_: Exception) {
                // Tự động chuyển sang GoogleSignInIntent fallback nếu CredentialManager không khả dụng hoặc trả về NoCredentialException
                signInWithGoogleLegacy(onSuccess)
            }
        }
    }

    fun signInWithGoogleLegacy(onSuccess: () -> Unit) {
        googleSuccessCallback = onSuccess
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

    private fun saveFacebookUserToDb(name: String? = null, email: String? = null) {
        val user = auth.currentUser
        val dbHelper = UserDatabaseHelper(this)
        val userEmail = email ?: user?.email ?: if (user != null) "${user.uid}@facebook.com" else ""
        val userName = name ?: user?.displayName ?: "Facebook User"

        if (userEmail.isNotEmpty()) {
            val userInfo = UserInfo(
                fullName = userName,
                phoneNumber = "",
                email = userEmail,
                password = "FACEBOOK_AUTH_USER",
                authType = "FACEBOOK"
            )
            UserFileManager.saveUser(this, userInfo)
            if (!dbHelper.isEmailExists(userEmail)) {
                dbHelper.registerUser(
                    User(
                        fullName = userName,
                        phoneNumber = "",
                        email = userEmail,
                        password = "FACEBOOK_AUTH_USER",
                        authType = "FACEBOOK"
                    )
                )
            }
        }
    }

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
                                saveFacebookUserToDb()
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
                                                saveFacebookUserToDb()
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
                        val email = jsonObject?.optString("email")
                        val name = jsonObject?.optString("name")
                        if (!email.isNullOrEmpty() || !name.isNullOrEmpty()) {
                            saveFacebookUserToDb(name, email)
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
    fun signInWithFacebook(onSuccess: () -> Unit) {
        facebookSuccessCallback = onSuccess
        LoginManager.getInstance().logOut()
        LoginManager.getInstance().logInWithReadPermissions(
            this,
            callbackManager,
            listOf("public_profile", "email")
        )
    }
}

// Bảng màu giao diện
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
    object Home : Screen()
    object Register : Screen()
    object Login : Screen()
    object Dashboard : Screen()
}

@Composable
fun AppNavigation() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

    when (currentScreen) {
        is Screen.Home -> MainScreen(
            onNavigateToRegister = { currentScreen = Screen.Register }
        )
        is Screen.Register -> RegisterScreen(
            onBack = { currentScreen = Screen.Home },
            onRegisterSuccess = { currentScreen = Screen.Dashboard },
            onNavigateToLogin = { currentScreen = Screen.Login }
        )
        is Screen.Login -> LoginScreen(
            onBack = { currentScreen = Screen.Home },
            onNavigateToRegister = { currentScreen = Screen.Register },
            onLoginSuccess = { currentScreen = Screen.Dashboard }
        )
        is Screen.Dashboard -> DashboardScreen(
            onLogoutClick = { currentScreen = Screen.Home }
        )
    }
}

// ==================== MÀN HÌNH 1 & 2: TRANG CHỦ ====================
@Composable
fun MainScreen(onNavigateToRegister: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.White)
                .verticalScroll(rememberScrollState())
        ) {
            TopAnnouncementBanner()
            HeaderSection(onStartClick = onNavigateToRegister)
            HeroSection(
                onTrialClick = onNavigateToRegister
            )
            FeaturesSection()
            PopularTemplatesSection()
            BottomCtaSection(onStartClick = onNavigateToRegister)
            FooterSection()
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun TopAnnouncementBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.TopBannerGradient)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Thế hệ hợp đồng thông minh tiếp theo",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun HeaderSection(onStartClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = "Logo",
                tint = Color.Black,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "SmartContract AI",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.Black
            )
        }

        // Hình 1: Nút "Bắt đầu ngay"
        Button(
            onClick = onStartClick,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
            shape = RoundedCornerShape(20.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Bắt đầu ngay",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun HeroSection(onTrialClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Ký kết thông minh với ",
                fontSize = 13.sp,
                color = Color(0xFF334155),
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "AI Copilot",
                fontSize = 13.sp,
                color = Color(0xFF0284C7),
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Tối ưu quy trình pháp lý với AI hỗ trợ soạn thảo và ký kết App-to-App. Đảm bảo tính pháp lý, bảo mật và tốc độ vượt trội cho mọi giao dịch của bạn.",
            fontSize = 12.sp,
            color = AppColors.SubtitleGray,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Hình 2: Nút "Trải nghiệm miễn phí"
        Button(
            onClick = onTrialClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.PrimaryButtonBg),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            Text(
                text = "Trải nghiệm miễn phí",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = { },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = AppColors.SecondaryButtonBg),
            border = null,
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.PlayCircle,
                    contentDescription = null,
                    tint = Color(0xFF475569),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Xem demo",
                    color = Color(0xFF475569),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(1.dp, AppColors.BorderGray, RoundedCornerShape(16.dp))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Laptop,
                        contentDescription = "App Interface Mock",
                        tint = AppColors.PrimaryBlue,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "SmartContract AI Dashboard",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = "Giao diện soạn thảo và quản lý hợp đồng thông minh",
                        fontSize = 10.sp,
                        color = AppColors.SubtitleGray
                    )
                }
            }
        }
    }
}

@Composable
fun FeaturesSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = "Tính năng đột phá",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A)
        )
        Text(
            text = "Công cụ chuyên nghiệp cho kỷ nguyên số",
            fontSize = 12.sp,
            color = AppColors.SubtitleGray,
            modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = AppColors.DarkCardBg)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0284C7).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFF38B6FF),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "AI hỗ trợ soạn thảo",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Copilot phân tích ngữ cảnh và tự động đề xuất các điều khoản phù hợp với từng loại hình kinh doanh, giảm thiểu 80% thời gian soạn thảo thủ công.",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = AppColors.LightGrayBg),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF2563EB).copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Difference,
                        contentDescription = null,
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Kho mẫu tái sử dụng",
                    color = Color(0xFF0F172A),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Hàng ngàn mẫu hợp đồng chuẩn pháp lý được phân loại theo ngành nghề, sẵn sàng để tùy chỉnh và ký kết ngay lập tức.",
                    color = AppColors.SubtitleGray,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CategoryChip(text = "Dịch vụ")
                    CategoryChip(text = "Lao động")
                    CategoryChip(text = "Thuê nhà")
                }
            }
        }
    }
}

@Composable
fun CategoryChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(AppColors.ChipBg)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            color = Color(0xFF475569),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun PopularTemplatesSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = "Mẫu hợp đồng phổ biến",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A)
        )
        Text(
            text = "Bắt đầu nhanh với các tài liệu được tin dùng nhất.",
            fontSize = 12.sp,
            color = AppColors.SubtitleGray,
            modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
        )

        TemplateCard(
            badgeColor = AppColors.LightBlueBadge,
            iconColor = Color(0xFF0284C7),
            icon = Icons.Default.Diamond,
            title = "Hợp đồng Dịch vụ",
            description = "Dành cho freelancer và doanh nghiệp cung cấp dịch vụ chuyên nghiệp.",
            usageCount = "Sử dụng bởi 4.2k+ người"
        )

        Spacer(modifier = Modifier.height(12.dp))

        TemplateCard(
            badgeColor = AppColors.LightOrangeBadge,
            iconColor = Color(0xFFEA580C),
            icon = Icons.Default.Home,
            title = "Hợp đồng Thuê nhà",
            description = "Mẫu chuẩn theo quy định hiện hành, đầy đủ các điều khoản bảo vệ chủ nhà và khách.",
            usageCount = "Sử dụng bởi 3.5k+ người"
        )

        Spacer(modifier = Modifier.height(12.dp))

        TemplateCard(
            badgeColor = AppColors.LightGreenBadge,
            iconColor = Color(0xFF16A34A),
            icon = Icons.Default.Work,
            title = "Hợp đồng Lao động",
            description = "Mẫu hợp đồng nhân sự tối ưu cho startup và doanh nghiệp vừa và nhỏ.",
            usageCount = "Sử dụng bởi 5.1k+ người"
        )
    }
}

@Composable
fun TemplateCard(
    badgeColor: Color,
    iconColor: Color,
    icon: ImageVector,
    title: String,
    description: String,
    usageCount: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(badgeColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = Color(0xFF0F172A)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = description,
                fontSize = 11.sp,
                color = AppColors.SubtitleGray,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = usageCount,
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8)
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Go",
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun BottomCtaSection(onStartClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.DarkHeaderBg)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 28.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Sẵn sàng để ký kết thông minh hơn?",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Tham gia cùng hàng ngàn doanh nghiệp đang thay đổi cách họ quản lý pháp lý.",
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onStartClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "Bắt đầu ngay miễn phí",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun FooterSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "SmartContract AI",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(text = "Về chúng tôi", fontSize = 11.sp, color = Color(0xFF475569))
            Text(text = "Điều khoản", fontSize = 11.sp, color = Color(0xFF475569))
            Text(text = "Bảo mật", fontSize = 11.sp, color = Color(0xFF475569))
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(text = "Hỗ trợ", fontSize = 11.sp, color = Color(0xFF475569))

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "© 2024 SmartContract AI. Toàn bộ quyền được bảo lưu.",
            fontSize = 10.sp,
            color = Color(0xFF94A3B8),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun BottomNavigationBar(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = selectedTab == 0,
            onClick = { onTabSelected(0) },
            icon = { Icon(Icons.Default.Home, contentDescription = "Trang chủ") },
            label = { Text("Trang chủ", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF0084C7),
                selectedTextColor = Color(0xFF0084C7),
                indicatorColor = Color(0xFFE0F2FE)
            )
        )
        NavigationBarItem(
            selected = selectedTab == 1,
            onClick = { onTabSelected(1) },
            icon = { Icon(Icons.Outlined.Description, contentDescription = "Hợp đồng") },
            label = { Text("Hợp đồng", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF0084C7),
                selectedTextColor = Color(0xFF0084C7),
                indicatorColor = Color(0xFFE0F2FE)
            )
        )
        NavigationBarItem(
            selected = selectedTab == 2,
            onClick = { onTabSelected(2) },
            icon = { Icon(Icons.Outlined.ChevronLeft, contentDescription = "Mẫu") },
            label = { Text("Mẫu", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF0084C7),
                selectedTextColor = Color(0xFF0084C7),
                indicatorColor = Color(0xFFE0F2FE)
            )
        )
        NavigationBarItem(
            selected = selectedTab == 3,
            onClick = { onTabSelected(3) },
            icon = { Icon(Icons.Outlined.Settings, contentDescription = "Cài đặt") },
            label = { Text("Cài đặt", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF0084C7),
                selectedTextColor = Color(0xFF0084C7),
                indicatorColor = Color(0xFFE0F2FE)
            )
        )
    }
}

// ==================== MÀN HÌNH 3: ĐĂNG KÝ TÀI KHOẢN ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onBack: () -> Unit,
    onRegisterSuccess: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var fullName by remember { mutableStateOf("Nguyễn Văn A") }
    var phoneNumber by remember { mutableStateOf("090 123 4567") }
    var email by remember { mutableStateOf("name@company.com") }
    var password by remember { mutableStateOf("12345678") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isAgreed by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "SmartContract AI",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.White)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Tiêu đề trang
            Text(
                text = "Tạo tài khoản mới",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Bắt đầu hành trình số hóa hợp đồng của bạn ngay hôm nay.",
                fontSize = 13.sp,
                color = AppColors.SubtitleGray,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Trường 1: Họ và tên
            InputFieldLabel(text = "Họ và tên")
            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = customTextFieldColors(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Trường 2: Số điện thoại
            InputFieldLabel(text = "Số điện thoại")
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                colors = customTextFieldColors(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Trường 3: Email công việc
            InputFieldLabel(text = "Email công việc")
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                colors = customTextFieldColors(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Trường 4: Mật khẩu
            InputFieldLabel(text = "Mật khẩu")
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
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
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tối thiểu 8 ký tự bao gồm chữ cái và số.",
                fontSize = 11.sp,
                color = Color(0xFF94A3B8)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Checkbox Đồng ý điều khoản
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = isAgreed,
                    onCheckedChange = { isAgreed = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color.Black
                    ),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row {
                        Text(text = "Tôi đồng ý với ", fontSize = 11.sp, color = Color(0xFF475569))
                        Text(
                            text = "Điều khoản dịch vụ",
                            fontSize = 11.sp,
                            color = AppColors.TextBlue,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(text = " và ", fontSize = 11.sp, color = Color(0xFF475569))
                    }
                    Row {
                        Text(
                            text = "Chính sách bảo mật",
                            fontSize = 11.sp,
                            color = AppColors.TextBlue,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(text = " của SmartContract AI.", fontSize = 11.sp, color = Color(0xFF475569))
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Nút Đăng ký tài khoản
            Button(
                onClick = {
                    if (fullName.isBlank() || phoneNumber.isBlank() || email.isBlank() || password.isBlank()) {
                        Toast.makeText(context, "Vui lòng nhập đầy đủ thông tin!", Toast.LENGTH_SHORT).show()
                    } else if (!isAgreed) {
                        Toast.makeText(context, "Bạn cần đồng ý với Điều khoản dịch vụ!", Toast.LENGTH_SHORT).show()
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
                                authType = "NORMAL"
                            )
                            val userInfo = UserInfo(
                                fullName = fullName,
                                phoneNumber = phoneNumber,
                                email = email,
                                password = password,
                                authType = "NORMAL"
                            )
                            UserFileManager.saveUser(context, userInfo)
                            val isSuccess = dbHelper.registerUser(newUser)
                            if (isSuccess) {
                                Toast.makeText(context, "Đăng Ký Thành Công", Toast.LENGTH_SHORT).show()
                                onRegisterSuccess()
                            } else {
                                Toast.makeText(context, "Đăng ký thất bại, vui lòng thử lại!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Text(
                    text = "Đăng ký tài khoản",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

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

            Spacer(modifier = Modifier.height(20.dp))

            // Nút Google & Facebook
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        (context.findActivity() as? MainActivity)?.signInWithGoogle(onRegisterSuccess)
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
                            fontSize = 13.sp
                        )
                    }
                }

                OutlinedButton(
                    onClick = {
                        (context.findActivity() as? MainActivity)?.signInWithFacebook(onRegisterSuccess)
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
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Facebook",
                            color = Color(0xFF1E293B),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
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
                    color = AppColors.TextBlue,
                    modifier = Modifier.clickable { onNavigateToLogin() }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
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
    onBack: () -> Unit,
    onNavigateToRegister: () -> Unit = {},
    onLoginSuccess: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var email by remember { mutableStateOf("email@example.com") }
    var password by remember { mutableStateOf("12345678") }
    var passwordVisible by remember { mutableStateOf(false) }

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

            // Top Bar với nút Quay lại
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Quay lại",
                        tint = Color(0xFF334155),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = "Quay lại",
                    fontSize = 12.sp,
                    color = Color(0xFF475569),
                    modifier = Modifier.clickable { onBack() }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

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
                    // Black Shield Logo
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Logo",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Chào mừng quay trở lại",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Vui lòng đăng nhập để tiếp tục quản lý\nhợp đồng thông minh",
                        fontSize = 12.sp,
                        color = AppColors.SubtitleGray,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Email Field
                    Column(modifier = Modifier.fillMaxWidth()) {
                        InputFieldLabel(text = "Email")
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Email,
                                    contentDescription = null,
                                    tint = Color(0xFF475569),
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            colors = customTextFieldColors(),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Password Field
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            InputFieldLabel(text = "Mật khẩu")
                            Text(
                                text = "Quên mật khẩu?",
                                fontSize = 11.sp,
                                color = AppColors.TextBlue,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickable { }
                            )
                        }

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Lock,
                                    contentDescription = null,
                                    tint = Color(0xFF475569),
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

                    Spacer(modifier = Modifier.height(22.dp))

                    // Button Đăng nhập
                    Button(
                        onClick = {
                            if (email.isBlank() || password.isBlank()) {
                                Toast.makeText(context, "Vui lòng nhập đầy đủ Email và Mật khẩu!", Toast.LENGTH_SHORT).show()
                            } else {
                                val dbHelper = UserDatabaseHelper(context)
                                val isFileMatched = UserFileManager.checkNormalLogin(context, email, password)
                                val isDbMatched = dbHelper.checkUserLogin(email, password)
                                if (isFileMatched || isDbMatched) {
                                    // Khớp thông tin với file lưu trữ đăng ký
                                    Toast.makeText(context, "Đăng Nhập Thành Công", Toast.LENGTH_SHORT).show()
                                    onLoginSuccess()
                                } else {
                                    // Không khớp thông tin với file/db đăng ký
                                    Toast.makeText(context, "Tài Khoản Không Tồn Tại", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
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

                    Spacer(modifier = Modifier.height(24.dp))

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

                    Spacer(modifier = Modifier.height(20.dp))

                    // Google & Facebook Buttons
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
                            color = AppColors.TextBlue,
                            modifier = Modifier.clickable { onNavigateToRegister() }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Badges: SECURE SSL & AI AUDITED
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.VerifiedUser,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "SECURE SSL",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Shield,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "AI AUDITED",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Footer Copyright
            Text(
                text = "© 2024 SmartContract AI. All rights reserved.",
                fontSize = 10.sp,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ==================== MÀN HÌNH DASHBOARD (SAU ĐĂNG NHẬP / ĐĂNG KÝ) ====================
@Composable
fun DashboardScreen(
    onAccountClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { },
                containerColor = Color(0xFF38BDF8),
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Tạo mới",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8FAFC))
                .verticalScroll(rememberScrollState())
        ) {
            // 1. Top Header Bar (Menu Icon, App Title, Notification Bell & Avatar với Menu)
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

            // 2. Hero Section (AI Copilot Banner, Title, Subtitle, Action Buttons & Code/AI Card)
            DashboardHeroSection()

            Spacer(modifier = Modifier.height(24.dp))

            // 3. Tổng quan tài khoản (Account Overview Stats & Professional Plan Card)
            AccountOverviewSection()

            Spacer(modifier = Modifier.height(24.dp))

            // 4. Hợp đồng gần đây (Recent Contracts List)
            RecentContractsSection()

            Spacer(modifier = Modifier.height(24.dp))

            // 5. Mẫu phổ biến (Popular Templates Section)
            DashboardPopularTemplatesSection()

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
fun DashboardHeader(
    onAccountClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val currentUser = FirebaseAuth.getInstance().currentUser
    val avatarUrl = currentUser?.photoUrl?.toString()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Menu",
                tint = Color.Black,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "SmartContract AI",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color(0xFF0F172A)
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Notification Bell with Badge
            Box {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = "Thông báo",
                    tint = Color(0xFF334155),
                    modifier = Modifier.size(22.dp)
                )
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444))
                        .align(Alignment.TopEnd)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // User Avatar Icon (Hiển thị ảnh Facebook/Google URL nếu có, ngược lại dùng Icon mặc định)
            Box {
                Box(
                    modifier = Modifier
                        .size(34.dp)
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
                }

                // Menu sổ xuống
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Tài Khoản", fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
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
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onLogoutClick()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardHeroSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // AI Copilot Badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF38BDF8))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "AI COPILOT ĐÃ SẴN SÀNG",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Hero Title
        Text(
            text = "Biến ý tưởng\nthành Hợp đồng\nPháp lý trong\ngiây lát.",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A),
            lineHeight = 34.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Hero Description
        Text(
            text = "Sử dụng trí tuệ nhân tạo để soạn thảo, rà soát và phân tích rủi ro hợp đồng tự động. Tăng tốc quy trình pháp lý của bạn lên gấp 10 lần.",
            fontSize = 12.sp,
            color = AppColors.SubtitleGray,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Action Buttons: Tạo hợp đồng mới & Phân tích AI
        Button(
            onClick = { },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AddCircleOutline,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Tạo hợp đồng mới",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = { },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            Text(
                text = "Phân tích AI",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Mock Document & AI Analysis Window Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Window Control Dots & Filename
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFEF4444)))
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFF59E0B)))
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF10B981)))
                    }
                    Text(
                        text = "contract_v2.docx",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFFF1F5F9))
                Spacer(modifier = Modifier.height(12.dp))

                // Skeleton Content Lines
                Box(modifier = Modifier.fillMaxWidth(0.65f).height(10.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFE2E8F0)))
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth(0.9f).height(10.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFE2E8F0)))

                Spacer(modifier = Modifier.height(12.dp))

                // AI Copilot Analysis Highlighted Box
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F2FE))
                ) {
                    Row(modifier = Modifier.padding(12.dp)) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "AI Copilot Analysis",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0369A1)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Điều khoản bảo mật (Section 4.2) đang thiếu quy định về thời gian hiệu lực sau khi chấm dứt hợp đồng. Gợi ý thêm: \"có hiệu lực 02 năm\".",
                                fontSize = 10.sp,
                                color = Color(0xFF0F172A),
                                lineHeight = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Box(modifier = Modifier.fillMaxWidth(0.55f).height(10.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFE2E8F0)))
            }
        }
    }
}

@Composable
fun AccountOverviewSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Text(
            text = "Tổng quan tài khoản",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Stat Card 1: Hợp đồng của tôi
        StatCard(
            icon = Icons.Outlined.Description,
            iconTint = Color(0xFF0284C7),
            count = "12",
            label = "Hợp đồng của tôi"
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Stat Card 2: Đang chờ ký
        StatCard(
            icon = Icons.Outlined.EditNote,
            iconTint = Color(0xFFD97706),
            count = "03",
            label = "Đang chờ ký"
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Stat Card 3: Đã hoàn tất
        StatCard(
            icon = Icons.Outlined.CheckCircle,
            iconTint = Color(0xFF059669),
            count = "08",
            label = "Đã hoàn tất"
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Dark Card: Gói hiện tại Professional
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1424))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "GÓI HIỆN TẠI",
                    fontSize = 10.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Professional",
                    fontSize = 16.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { }
                ) {
                    Text(
                        text = "Nâng cấp",
                        fontSize = 12.sp,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StatCard(
    icon: ImageVector,
    iconTint: Color,
    count: String,
    label: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = count,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                color = AppColors.SubtitleGray
            )
        }
    }
}

@Composable
fun RecentContractsSection() {
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
                color = Color(0xFF0284C7),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Contract Item 1: Hợp đồng Tư vấn Thiết kế UI/UX (Đã ký)
        ContractItem(
            title = "Hợp đồng Tư vấn Thiết kế UI/UX",
            subtitle = "Cập nhật 2 giờ trước • 2.4 KB",
            statusText = "ĐÃ\nKÝ",
            statusBg = Color(0xFFDCFCE7),
            statusTextColor = Color(0xFF166534)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Contract Item 2: Thỏa thuận Bảo mật (NDA) - Project X (Đang rà soát)
        ContractItem(
            title = "Thỏa thuận Bảo mật (NDA) - Project X",
            subtitle = "Cập nhật hôm qua • 18 KB",
            statusText = "ĐANG\nRÀ\nSOÁT",
            statusBg = Color(0xFFFEF3C7),
            statusTextColor = Color(0xFF92400E)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Contract Item 3: Hợp đồng Thuê Văn phòng Q1 (Bản nháp)
        ContractItem(
            title = "Hợp đồng Thuê Văn phòng Q1",
            subtitle = "Cập nhật 3 ngày trước • 42 KB",
            statusText = "BẢN\nNHÁP",
            statusBg = Color(0xFFF1F5F9),
            statusTextColor = Color(0xFF475569)
        )
    }
}

@Composable
fun ContractItem(
    title: String,
    subtitle: String,
    statusText: String,
    statusBg: Color,
    statusTextColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF8FAFC)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Description,
                    contentDescription = null,
                    tint = Color(0xFF475569),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Status Chip Badge
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(statusBg)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = statusText,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusTextColor,
                    textAlign = TextAlign.Center,
                    lineHeight = 10.sp
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More",
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(16.dp)
            )
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

        // Template 1: Thỏa thuận Lao động
        DashboardTemplateCard(
            title = "Thỏa thuận Lao động",
            description = "Dành cho nhân viên Fulltime / Parttime theo luật VN."
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Template 2: Hợp đồng Cung cấp Dịch vụ
        DashboardTemplateCard(
            title = "Hợp đồng Cung cấp Dịch vụ",
            description = "Phù hợp cho Agency, Freelancer và Đối tác."
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Template 3: Biên bản Ghi nhớ (MOU)
        DashboardTemplateCard(
            title = "Biên bản Ghi nhớ (MOU)",
            description = "Xác lập các thỏa thuận hợp tác sơ bộ."
        )
    }
}

@Composable
fun DashboardTemplateCard(title: String, description: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                fontSize = 10.sp,
                color = Color(0xFF94A3B8)
            )
            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = { },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Text(
                    text = "Sử dụng mẫu",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
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