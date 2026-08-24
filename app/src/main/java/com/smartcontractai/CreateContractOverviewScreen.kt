@file:Suppress("Deprecation", "UnusedImport", "UNUSED_IMPORT", "RememberReturnType", "COMPOSABLE_INVOCATION", "ComposableInvocation")

package com.smartcontractai

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartcontractai.ui.theme.SmartContractAITheme

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

// ==================== MÀN HÌNH KHỞI TẠO NHANH (MÀN HÌNH 1) ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateContractOverviewScreen(
    onBack: () -> Unit = {},
    onNavigateToDashboard: (Int) -> Unit = {},
    onNavigateToCreateWithAI: () -> Unit = {},
    onNavigateToContractTemplates: () -> Unit = {},
    onNavigateToReview: (title: String, content: String) -> Unit = { _, _ -> }
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var bottomNavTab by remember { mutableIntStateOf(1) } // 1: Contracts selected
    var showCloneBottomSheet by remember { mutableStateOf(false) }

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
                    if (index == 2) {
                        onNavigateToContractTemplates()
                    } else {
                        onNavigateToDashboard(index)
                    }
                }
            )
        },
        containerColor = Color.White
    ) { innerPadding: PaddingValues ->
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

            // Card 2: Nhân bản
            QuickStartItemCard(
                icon = Icons.Outlined.ContentCopy,
                iconBg = Color(0xFFE2E8F0),
                iconTint = Color(0xFF1D4ED8),
                title = "Nhân bản",
                subtitle = "Từ hợp đồng đã có",
                onClick = {
                    showCloneBottomSheet = true
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

        if (showCloneBottomSheet) {
            CloneContractBottomSheet(
                onDismissRequest = { showCloneBottomSheet = false },
                onContractCloned = { newTitle, newContent ->
                    showCloneBottomSheet = false
                    onNavigateToReview(newTitle, newContent)
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CreateContractOverviewScreenPreview() {
    SmartContractAITheme {
        CreateContractOverviewScreen()
    }
}
