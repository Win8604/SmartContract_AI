@file:Suppress("Deprecation", "UnusedImport", "UNUSED_IMPORT", "RememberReturnType", "COMPOSABLE_INVOCATION", "ComposableInvocation")

package com.smartcontractai

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Laptop
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartcontractai.ui.theme.SmartContractAITheme

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
    onNavigateToDashboard: (Int) -> Unit = {},
    onNavigateToCreateWithAI: () -> Unit = {},
    onNavigateToDocumentEditor: (String) -> Unit = {},
    onNavigateToReview: (String, String) -> Unit = { _, _ -> }
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

    var selectedTemplateId by remember { mutableStateOf<String?>(null) } // null: chưa click chọn mẫu nào

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
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Mẫu Hợp Đồng",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1D4ED8)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            BottomNavigationBar(
                selectedTab = bottomNavTab,
                onTabSelected = { index ->
                    bottomNavTab = index
                    if (index != 2) {
                        onNavigateToDashboard(index)
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
                val isSelected = selectedTemplateId == template.id
                TemplateCardItem(
                    template = template,
                    isSelected = isSelected,
                    onSelect = { selectedTemplateId = template.id },
                    onUseClick = {
                        selectedTemplateId = template.id
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
    isSelected: Boolean = false,
    onSelect: () -> Unit = {},
    onUseClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFFAFCFF) else Color.White
        ),
        border = BorderStroke(1.dp, if (isSelected) Color(0xFF93C5FD) else Color(0xFFF1F5F9)),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 1.dp)
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
            // Nút bấm thay đổi giao diện theo trạng thái click chọn (isSelected)
            if (isSelected) {
                // Hình 1: Nút xanh dương nổi bật kèm mũi tên -> (Khi được click chọn)
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
                // Hình 2: Nút viền trong suốt/trắng với chữ xanh (Khi chưa được click chọn)
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
