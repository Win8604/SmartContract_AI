@file:Suppress("Deprecation", "UnusedImport", "UNUSED_IMPORT")

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
                val photoUrl = user.photoUrl?.toString()
                val userInfo = UserInfo(
                    fullName = userName,
                    phoneNumber = "",
                    email = userEmail,
                    password = "GOOGLE_AUTH_USER",
                    authType = "GOOGLE",
                    avatarUrl = photoUrl
                )
                UserFileManager.saveUser(this, userInfo)
                UserFileManager.saveCurrentSessionEmail(this, userEmail)
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

    private fun saveFacebookUserToDb(name: String? = null, email: String? = null, avatarUrl: String? = null) {
        val user = auth.currentUser
        val dbHelper = UserDatabaseHelper(this)
        val userEmail = email ?: user?.email ?: if (user != null) "${user.uid}@facebook.com" else ""
        val userName = name ?: user?.displayName ?: "Facebook User"

        if (userEmail.isNotEmpty()) {
            val existingUser = UserFileManager.getUserByEmail(this, userEmail)
            val fbPhotoUrl = avatarUrl ?: user?.photoUrl?.toString() ?: existingUser?.avatarUrl

            val userInfo = UserInfo(
                fullName = userName,
                phoneNumber = existingUser?.phoneNumber ?: "",
                email = userEmail,
                password = "FACEBOOK_AUTH_USER",
                authType = "FACEBOOK",
                avatarUrl = fbPhotoUrl
            )
            UserFileManager.saveUser(this, userInfo)
            UserFileManager.saveCurrentSessionEmail(this, userEmail)
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
                        if (jsonObject != null) {
                            val email = jsonObject.optString("email")
                            val name = jsonObject.optString("name")
                            val fbId = jsonObject.optString("id")
                            val pictureObj = jsonObject.optJSONObject("picture")
                            val dataObj = pictureObj?.optJSONObject("data")
                            val rawUrl = dataObj?.optString("url")
                            val avatarUrl = if (!rawUrl.isNullOrEmpty()) rawUrl else if (fbId.isNotEmpty()) "https://graph.facebook.com/$fbId/picture?type=large" else null

                            saveFacebookUserToDb(name, email, avatarUrl)
                        } else {
                            saveFacebookUserToDb()
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
            onLogoutClick = { currentScreen = Screen.Login }
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
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.White,
                selectedTextColor = Color(0xFF1D4ED8),
                indicatorColor = Color(0xFF1D4ED8)
            )
        )
        NavigationBarItem(
            selected = selectedTab == 1,
            onClick = { onTabSelected(1) },
            icon = { Icon(Icons.Outlined.Description, contentDescription = "Contracts") },
            label = { Text("Contracts", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF1D4ED8),
                selectedTextColor = Color(0xFF1D4ED8),
                indicatorColor = Color(0xFFE0EDFF)
            )
        )
        NavigationBarItem(
            selected = selectedTab == 2,
            onClick = { onTabSelected(2) },
            icon = { Icon(Icons.Outlined.Assignment, contentDescription = "Templates") },
            label = { Text("Templates", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF1D4ED8),
                selectedTextColor = Color(0xFF1D4ED8),
                indicatorColor = Color(0xFFE0EDFF)
            )
        )
        NavigationBarItem(
            selected = selectedTab == 3,
            onClick = { onTabSelected(3) },
            icon = { Icon(Icons.Outlined.WorkOutline, contentDescription = "Business") },
            label = { Text("Business", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF1D4ED8),
                selectedTextColor = Color(0xFF1D4ED8),
                indicatorColor = Color(0xFFE0EDFF)
            )
        )
        NavigationBarItem(
            selected = selectedTab == 4,
            onClick = { onTabSelected(4) },
            icon = { Icon(Icons.Outlined.Settings, contentDescription = "Settings") },
            label = { Text("Settings", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF1D4ED8),
                selectedTextColor = Color(0xFF1D4ED8),
                indicatorColor = Color(0xFFE0EDFF)
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

            // Top Bar với nút Quay lại (nếu có)
            if (onBack != null) {
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
            }

            Spacer(modifier = Modifier.height(10.dp))

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
                        text = "Tạo tài khoản mới",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0038A8),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Bảo mật thông minh cho hợp đồng của bạn",
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

                    // Trường 1: Họ và tên
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Họ và tên", color = Color(0xFF94A3B8), fontSize = 13.sp) },
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Badge,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        colors = customTextFieldColors(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Trường 2: Số điện thoại
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Số điện thoại", color = Color(0xFF94A3B8), fontSize = 13.sp) },
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
                        placeholder = { Text("Email hoặc Gmail", color = Color(0xFF94A3B8), fontSize = 13.sp) },
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
                            if (fullName.isBlank() || phoneNumber.isBlank() || email.isBlank() || password.isBlank()) {
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
                                        authType = if (isCorporate) "CORPORATE" else "NORMAL"
                                    )
                                    val userInfo = UserInfo(
                                        fullName = fullName,
                                        phoneNumber = phoneNumber,
                                        email = email,
                                        password = password,
                                        authType = if (isCorporate) "CORPORATE" else "NORMAL"
                                    )
                                    UserFileManager.saveUser(context, userInfo)
                                    UserFileManager.saveCurrentSessionEmail(context, email)
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
                            (context.findActivity() as? MainActivity)?.signInWithGoogle(onRegisterSuccess)
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
                                text = "Đăng ký với Google",
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
                            (context.findActivity() as? MainActivity)?.signInWithFacebook(onRegisterSuccess)
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
                                text = "Đăng ký với Facebook",
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

            // Top Bar với nút Quay lại (nếu có)
            if (onBack != null) {
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
                                Toast.makeText(context, "Xác thực sinh trắc học (Fingerprint)", Toast.LENGTH_SHORT).show()
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

                    // Nút đăng nhập MXH: Google & Facebook
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

                    Spacer(modifier = Modifier.height(10.dp))

                    // Nút App Auth
                    OutlinedButton(
                        onClick = {
                            Toast.makeText(context, "Xác thực với App Auth", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "App Auth",
                                tint = Color(0xFF0038A8),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "App Auth",
                                color = Color(0xFF1E293B),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
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
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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

            Spacer(modifier = Modifier.height(20.dp))

            // 4. Action Cards (Tạo với Mẫu, Tạo với AI Gemini, Nhân bản)
            DashboardActionCards()

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
fun DashboardActionCards() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Card 1: Tạo với Mẫu
        ActionCardItem(
            icon = Icons.Outlined.Description,
            badgeBg = Color(0xFFE0EDFF),
            iconTint = Color(0xFF1D4ED8),
            title = "Tạo với Mẫu",
            subtitle = "Từ thư viện chuẩn",
            tag = null
        )

        // Card 2: Tạo với AI Gemini (Featured MỚI)
        ActionCardItem(
            icon = Icons.Default.AutoAwesome,
            badgeBg = Color(0xFF4F46E5),
            iconTint = Color.White,
            title = "Tạo với AI Gemini",
            titleColor = Color(0xFF4338CA),
            subtitle = "Soạn thảo thông minh",
            tag = "MỚI",
            tagBg = Color(0xFFE0E7FF),
            tagTextColor = Color(0xFF4338CA),
            cardBorderColor = Color(0xFFC7D2FE)
        )

        // Card 3: Nhân bản (10s)
        ActionCardItem(
            icon = Icons.Outlined.ContentCopy,
            badgeBg = Color(0xFFE0EDFF),
            iconTint = Color(0xFF1D4ED8),
            title = "Nhân bản (10s)",
            subtitle = "Từ HĐ gần nhất",
            tag = null
        )
    }
}

@Composable
fun ActionCardItem(
    icon: ImageVector,
    badgeBg: Color,
    iconTint: Color,
    title: String,
    titleColor: Color = Color(0xFF0F172A),
    subtitle: String,
    tag: String? = null,
    tagBg: Color = Color.Transparent,
    tagTextColor: Color = Color.Transparent,
    cardBorderColor: Color = Color(0xFFF1F5F9)
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, cardBorderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(badgeBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = titleColor
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }

            if (tag != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(tagBg)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = tag,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = tagTextColor
                    )
                }
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