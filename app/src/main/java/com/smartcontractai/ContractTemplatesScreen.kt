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
    val icon: ImageVector = Icons.Outlined.EditNote,
    val isPrimaryButton: Boolean = false,
    val code: String = "",
    val templateContent: String = ""
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
    var selectedCategoryKey by remember { mutableStateOf("all") }
    var bottomNavTab by remember { mutableIntStateOf(2) } // 2: Templates selected

    var categoriesList by remember {
        mutableStateOf(
            listOf(
                com.smartcontractai.network.TemplateCategoryItem("all", "Tất cả"),
                com.smartcontractai.network.TemplateCategoryItem("rental", "Thuê nhà"),
                com.smartcontractai.network.TemplateCategoryItem("deposit", "Đặt cọc"),
                com.smartcontractai.network.TemplateCategoryItem("commercial", "Mặt bằng kinh doanh"),
                com.smartcontractai.network.TemplateCategoryItem("annex", "Phụ lục")
            )
        )
    }

    var templatesList by remember { mutableStateOf<List<ContractTemplateModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTemplateId by remember { mutableStateOf<String?>(null) } // null: chưa click chọn mẫu nào

    // Tự động tải danh sách Danh mục từ PostgreSQL Backend Server
    androidx.compose.runtime.LaunchedEffect(Unit) {
        com.smartcontractai.network.ApiClient.fetchTemplateCategoriesFromPostgres { success, fetchedCategories ->
            if (success && fetchedCategories.isNotEmpty()) {
                categoriesList = fetchedCategories
            }
        }
    }

    // Tự động tải danh sách Mẫu hợp đồng từ PostgreSQL Backend Server
    androidx.compose.runtime.LaunchedEffect(selectedCategoryKey) {
        isLoading = true
        val catParam = if (selectedCategoryKey == "all") null else selectedCategoryKey
        com.smartcontractai.network.ApiClient.fetchTemplatesFromPostgres(category = catParam) { _, items ->
            isLoading = false
            templatesList = items.ifEmpty {
                com.smartcontractai.network.ApiClient.getDefaultBackendTemplates(catParam)
            }
        }
    }

    val filteredTemplates = remember(templatesList, selectedScopeTab) {
        templatesList.filter { template ->
            when (selectedScopeTab) {
                0 -> true
                1 -> template.scope == "Doanh nghiệp" || template.category == "Mặt bằng kinh doanh"
                else -> template.scope == "Cá nhân" || template.scope == "Công khai" || template.isAiOptimized
            }
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
    ) { innerPadding: PaddingValues ->
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
                    items(categoriesList) { catItem ->
                        val isSelected = selectedCategoryKey == catItem.key
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) Color(0xFFDBEAFE) else Color(0xFFF1F5F9))
                                .clickable { selectedCategoryKey = catItem.key }
                                .padding(horizontal = 14.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = catItem.name,
                                fontSize = 12.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color(0xFF1D4ED8) else Color(0xFF475569)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. Template Cards List from Backend API
            if (isLoading) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFF1F5F9))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Đang tải mẫu hợp đồng từ Backend...",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            } else if (filteredTemplates.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFF1F5F9))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Không tìm thấy mẫu hợp đồng nào phù hợp.",
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            } else {
                filteredTemplates.forEach { template ->
                    val isSelected = selectedTemplateId == template.id
                    TemplateCardItem(
                        template = template,
                        isSelected = isSelected,
                        onSelect = { selectedTemplateId = template.id },
                        onUseClick = {
                            selectedTemplateId = template.id
                            Toast.makeText(context, "Mở hợp đồng mẫu: ${template.title}", Toast.LENGTH_SHORT).show()
                            onNavigateToDocumentEditor(template.title)
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
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
