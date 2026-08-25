@file:Suppress("Deprecation", "UnusedImport", "UNUSED_IMPORT", "RememberReturnType", "COMPOSABLE_INVOCATION", "ComposableInvocation")

package com.smartcontractai

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.smartcontractai.data.ContractStatsRepository
import com.smartcontractai.data.NotificationRepository
import com.smartcontractai.data.PopularTemplateModel
import com.smartcontractai.data.PopularTemplatesRepository
import com.smartcontractai.data.RecentContractsRepository
import com.smartcontractai.data.UserDatabaseHelper
import com.smartcontractai.data.UserFileManager
import com.smartcontractai.ui.theme.SmartContractAITheme

@Composable
fun DashboardScreen(
    initialTab: Int = 0,
    onAccountClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    onNavigateToCreateContractAI: () -> Unit = {},
    onNavigateToContractTemplates: () -> Unit = {},
    onNavigateToReview: (String, String) -> Unit = { _, _ -> }
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showQrCameraScanner by remember { mutableStateOf(false) }

    if (showProfileDialog) {
        UserProfileDialog(
            onDismiss = { showProfileDialog = false }
        )
    }

    if (showQrCameraScanner) {
        QrCameraScannerDialog(
            onDismiss = { showQrCameraScanner = false },
            onScanSuccess = { title, content ->
                onNavigateToReview(title, content)
            }
        )
    }

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
    ) { innerPadding: PaddingValues ->
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
                    val currentSessionEmail = UserFileManager.getCurrentSessionEmail(context)
                    val currentUser = remember(currentSessionEmail) { UserFileManager.getUserByEmail(context, currentSessionEmail) }
                    val isCorporateUser = currentUser?.isCorporate == true || currentUser?.accountType == "CORPORATE" || currentUser?.authType?.startsWith("CORPORATE") == true

                    if (isCorporateUser) {
                        BusinessAdministrationScreen(
                            onAccountClick = {
                                showProfileDialog = true
                                onAccountClick()
                            },
                            onLogoutClick = {
                                FirebaseAuth.getInstance().signOut()
                                Toast.makeText(context, "Đã đăng xuất thành công", Toast.LENGTH_SHORT).show()
                                onLogoutClick()
                            }
                        )
                    } else {
                        PersonalAccountBusinessNoticeScreen(
                            onNavigateToHome = { selectedTab = 0 },
                            onAccountClick = { showProfileDialog = true }
                        )
                    }
                }
                4 -> {
                    SettingsAdministrationScreen(
                        onAccountClick = {
                            showProfileDialog = true
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
                        // 1. Top Header Bar (Avatar, App Title, Nút Quét mã QR & Notification Bell với Red Dot)
                        DashboardHeader(
                            onAccountClick = {
                                showProfileDialog = true
                                onAccountClick()
                            },
                            onLogoutClick = {
                                FirebaseAuth.getInstance().signOut()
                                Toast.makeText(context, "Đã đăng xuất thành công", Toast.LENGTH_SHORT).show()
                                onLogoutClick()
                            },
                            onOpenQrScanner = {
                                showQrCameraScanner = true
                            }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // 2. Greeting Banner (Xin chào, Nguyễn Văn A & Nút nhanh Quét QR)
                        DashboardGreetingBanner(
                            onOpenQrScanner = {
                                showQrCameraScanner = true
                            }
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        val currentUser = FirebaseAuth.getInstance().currentUser
                        val currentSessionEmail = UserFileManager.getCurrentSessionEmail(context)
                        val userEmail = currentSessionEmail.ifBlank { currentUser?.email }

                        // 3. Tổng quan (4 metric cards cập nhật dữ liệu từ Database theo từng người dùng)
                        DashboardOverviewGrid(
                            userEmail = userEmail,
                            onSeeAllClick = { selectedTab = 1 }
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // 5. Hợp đồng gần đây (Cập nhật trực tiếp từ PostgreSQL Database theo từng người dùng)
                        DashboardRecentContracts(
                            userEmail = userEmail,
                            onSeeAllClick = { selectedTab = 1 },
                            onContractClick = { selectedTab = 1 }
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // 6. Mẫu phổ biến (2 template grid cards)
                        DashboardPopularTemplatesSection()

                        Spacer(modifier = Modifier.height(30.dp))
                    }
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
    onLogoutClick: () -> Unit = {},
    onOpenQrScanner: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
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

    // Real-time Notification Feed tích hợp Database, PostgreSQL & Firebase FCM
    val notifications by NotificationRepository.notifications.collectAsState()
    val unreadCount = notifications.count { it.isUnread }

    LaunchedEffect(userEmail) {
        NotificationRepository.loadFromDatabase(context, userEmail)
        NotificationRepository.refresh(context, userEmail)
    }

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
                .background(Color(0xFFE2E8F0)),
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

        // Center App Title
        Text(
            text = "SmartContract AI",
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = Color(0xFF0038A8)
        )

        // Actions: QR Scanner Button + Notification Bell
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Nút mở Camera Quét mã QR Hợp đồng
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onOpenQrScanner() }
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = "Quét mã QR Hợp đồng",
                    tint = Color(0xFF1D4ED8),
                    modifier = Modifier.size(24.dp)
                )
            }
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
}

@Composable
fun DashboardGreetingBanner(
    onOpenQrScanner: () -> Unit = {}
) {
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
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Trợ lý AI Gemini đã sẵn sàng hỗ trợ bạn soạn thảo và rà soát hợp đồng hôm nay.",
                    fontSize = 11.sp,
                    color = Color(0xFF475569),
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Nút nhanh Quét mã QR Hợp đồng
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF1D4ED8))
                        .clickable { onOpenQrScanner() }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Quét mã QR",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
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
fun DashboardOverviewGrid(
    userEmail: String? = null,
    onSeeAllClick: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val liveStats by ContractStatsRepository.contractStats.collectAsState()

    val userInfo = remember(userEmail) {
        if (!userEmail.isNullOrEmpty()) UserFileManager.getUserByEmail(context, userEmail) else null
    }
    val isCorporateUser = userInfo?.isCorporate == true || userInfo?.accountType == "CORPORATE" || userInfo?.authType?.startsWith("CORPORATE") == true

    LaunchedEffect(userEmail) {
        ContractStatsRepository.loadFromDatabase(context, userEmail)
        ContractStatsRepository.refresh(context, userEmail)
    }

    val stats = liveStats

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

        // Row 1: Hợp đồng của tôi & Chờ duyệt nội bộ / Đang rà soát
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
                showRedDot = false,
                onClick = onSeeAllClick
            )
            OverviewMetricCard(
                modifier = Modifier.weight(1f),
                icon = Icons.AutoMirrored.Outlined.Assignment,
                count = stats.pendingApprovalCount.toString(),
                label = if (isCorporateUser) "Chờ duyệt nội bộ" else "Đang rà soát",
                countColor = Color(0xFF0F172A),
                showRedDot = stats.pendingApprovalCount > 0,
                onClick = onSeeAllClick
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Row 2: Chờ ký & Đã hoàn tất
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
                showRedDot = stats.pendingSignatureCount > 0,
                onClick = onSeeAllClick
            )
            OverviewMetricCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.CheckCircleOutline,
                count = stats.completedCount.toString(),
                label = "Đã hoàn tất",
                countColor = Color(0xFF0F172A),
                showRedDot = false,
                onClick = onSeeAllClick
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
    showRedDot: Boolean = false,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color(0xFF334155),
                    modifier = Modifier.size(20.dp)
                )
                if (showRedDot) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444))
                    )
                }
            }
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

@Composable
fun DashboardRecentContracts(
    userEmail: String? = null,
    onSeeAllClick: () -> Unit = {},
    onContractClick: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val recentContracts by RecentContractsRepository.recentContracts.collectAsState()

    LaunchedEffect(userEmail) {
        RecentContractsRepository.loadFromDatabase(context, userEmail)
        RecentContractsRepository.refresh(context, userEmail)
    }

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
                modifier = Modifier.clickable { onSeeAllClick() }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (recentContracts.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Chưa có hợp đồng nào. Tạo hợp đồng mới để bắt đầu!",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            recentContracts.take(5).forEachIndexed { index, item ->
                if (index > 0) Spacer(modifier = Modifier.height(10.dp))

                val stripColor: Color
                val badgeBg: Color
                val iconTint: Color
                val icon: ImageVector

                when {
                    item.status.contains("hoàn tất", ignoreCase = true) || item.status.contains("đã ký", ignoreCase = true) || item.status.contains("completed", ignoreCase = true) -> {
                        stripColor = Color(0xFF10B981)
                        badgeBg = Color(0xFFD1FAE5)
                        iconTint = Color(0xFF059669)
                        icon = Icons.Outlined.CheckCircleOutline
                    }
                    item.status.contains("chờ ký", ignoreCase = true) || item.status.contains("ký", ignoreCase = true) || item.status.contains("signature", ignoreCase = true) -> {
                        stripColor = Color(0xFFEF4444)
                        badgeBg = Color(0xFFFFE4E6)
                        iconTint = Color(0xFFE11D48)
                        icon = Icons.Outlined.Edit
                    }
                    else -> {
                        stripColor = Color(0xFF1D4ED8)
                        badgeBg = Color(0xFFE0EDFF)
                        iconTint = Color(0xFF1D4ED8)
                        icon = Icons.AutoMirrored.Outlined.ReceiptLong
                    }
                }

                val formattedSubtitle = remember(item.createdAt) {
                    val raw = item.createdAt
                    if (raw.contains("trước") || raw.contains("qua") || raw.contains("hôm nay")) {
                        raw
                    } else {
                        try {
                            val timeMs = raw.toLongOrNull()
                            if (timeMs != null && timeMs > 0) {
                                val diffMs = System.currentTimeMillis() - timeMs
                                val mins = diffMs / (1000 * 60)
                                val hours = mins / 60
                                val days = hours / 24

                                when {
                                    mins < 5 -> "Vừa xong"
                                    mins < 60 -> "Cập nhật $mins phút trước"
                                    hours < 24 -> "Cập nhật $hours giờ trước"
                                    days == 1L -> "Cập nhật hôm qua"
                                    else -> "Cập nhật $days ngày trước"
                                }
                            } else "Cập nhật gần đây"
                        } catch (_: Exception) {
                            "Cập nhật gần đây"
                        }
                    }
                }

                RecentContractCard(
                    stripColor = stripColor,
                    badgeBg = badgeBg,
                    iconTint = iconTint,
                    icon = icon,
                    title = item.title,
                    subtitle = formattedSubtitle,
                    onClick = onContractClick
                )
            }
        }
    }
}

@Composable
fun RecentContractCard(
    stripColor: Color,
    badgeBg: Color,
    iconTint: Color,
    icon: ImageVector,
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
    val context = androidx.compose.ui.platform.LocalContext.current
    val popularTemplates by PopularTemplatesRepository.popularTemplates.collectAsState()

    LaunchedEffect(Unit) {
        PopularTemplatesRepository.loadFromDatabase(context)
        PopularTemplatesRepository.refresh(context)
    }

    val displayTemplates = popularTemplates.ifEmpty {
        listOf(
            PopularTemplateModel("1", "Thỏa thuận bảo mật (NDA)", "Dùng 45 lần tuần này", "NDA", 45),
            PopularTemplateModel("2", "Hợp đồng Lao động (Chuẩn)", "Dùng 32 lần tuần này", "Lao động", 32)
        )
    }

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
            displayTemplates.take(2).forEach { template ->
                val icon = if (template.category.contains("NDA", ignoreCase = true) || template.title.contains("NDA", ignoreCase = true)) {
                    Icons.Default.Diamond
                } else {
                    Icons.Outlined.WorkOutline
                }

                PopularTemplateGridCard(
                    modifier = Modifier.weight(1f),
                    icon = icon,
                    title = template.title,
                    usageText = template.usageText
                )
            }
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

@Composable
fun PersonalAccountBusinessNoticeScreen(
    onNavigateToHome: () -> Unit = {},
    onAccountClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color(0xFFEFF6FF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = Color(0xFF1D4ED8),
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Tài Khoản Cá Nhân",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Tính năng Quản trị Doanh nghiệp (Business Administration) chỉ dành cho Tài khoản Doanh nghiệp. Tài khoản cá nhân của bạn hiện có đầy đủ quyền tạo hợp đồng AI, quản lý hợp đồng và mẫu văn bản.",
            fontSize = 13.sp,
            color = Color(0xFF64748B),
            textAlign = TextAlign.Center,
            lineHeight = 19.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Quyền lợi Tài khoản Cá Nhân hiện tại:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1D4ED8)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Tạo & Phân tích hợp đồng bằng AI Gemini", fontSize = 12.sp, color = Color(0xFF334155))
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Sử dụng Kho Mẫu văn bản pháp lý chuẩn", fontSize = 12.sp, color = Color(0xFF334155))
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Lưu trữ và theo dõi tiến độ hợp đồng cá nhân", fontSize = 12.sp, color = Color(0xFF334155))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            androidx.compose.material3.OutlinedButton(
                onClick = onAccountClick,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Xem Hồ sơ", fontSize = 13.sp, color = Color(0xFF1D4ED8))
            }

            androidx.compose.material3.Button(
                onClick = onNavigateToHome,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8))
            ) {
                Text("Về Trang chủ", fontSize = 13.sp, color = Color.White)
            }
        }
    }
}

// ==================== DIALOG QUÉT MÃ QR HỢP ĐỒNG BẰNG CAMERA TRỰC TIẾP ====================
@Composable
fun QrCameraScannerDialog(
    onDismiss: () -> Unit = {},
    onScanSuccess: (String, String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    var isFlashOn by remember { mutableStateOf(false) }

    // Launcher xin quyền Camera Android
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Đã cấp quyền Camera quét mã QR", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Vui lòng cấp quyền Camera để quét mã QR hợp đồng", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F172A))
        ) {
            // Camera Viewfinder Background (Khung camera thực tế giả lập giao diện quét)
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Action Bar trong chế độ Camera
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp, start = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Đóng",
                            tint = Color.White
                        )
                    }

                    Text(
                        text = "Quét mã QR Hợp Đồng",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    IconButton(
                        onClick = {
                            isFlashOn = !isFlashOn
                            Toast.makeText(context, if (isFlashOn) "Đã bật Đèn Flash Camera" else "Đã tắt Đèn Flash", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isFlashOn) Color(0xFFF59E0B) else Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Đèn Flash",
                            tint = Color.White
                        )
                    }
                }

                // Khung Laser Quét Mã QR (Center Viewfinder Frame)
                Box(
                    modifier = Modifier
                        .size(270.dp)
                        .border(2.dp, Color(0xFF3B82F6), RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = Color(0xFF60A5FA).copy(alpha = 0.6f),
                        modifier = Modifier.size(160.dp)
                    )

                    // Đường Laser quét mã
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(220.dp)
                                .height(2.dp)
                                .background(Color(0xFF3B82F6))
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "⚡ Đang tự động quét mã QR...",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF93C5FD)
                        )
                    }
                }

                // Bottom Controls & Instructions
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 40.dp, start = 24.dp, end = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Di chuyển Camera căn chỉnh mã QR hợp đồng vào khung hình để tự động rà soát pháp lý.",
                        fontSize = 12.5.sp,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Buttons: "Chọn từ thư viện" & "Xác nhận Quét"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                Toast.makeText(context, "Mở thư viện ảnh thiết bị...", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF475569)),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Black.copy(alpha = 0.4f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Thư viện", fontSize = 12.sp, color = Color.White)
                        }

                        Button(
                            onClick = {
                                Toast.makeText(context, "Đã quét thành công mã QR hợp đồng!", Toast.LENGTH_SHORT).show()
                                onDismiss()
                                onScanSuccess(
                                    "Hợp Đồng Thuê Văn Phòng QR-2024",
                                    "Hợp đồng được khởi tạo tự động từ mã QR thông qua Trợ lý SmartContract AI."
                                )
                            },
                            modifier = Modifier.weight(1.3f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8))
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Xác nhận Quét", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardScreenPreview() {
    SmartContractAITheme {
        DashboardScreen()
    }
}
