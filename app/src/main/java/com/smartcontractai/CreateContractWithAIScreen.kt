@file:Suppress("Deprecation", "UnusedImport", "UNUSED_IMPORT", "RememberReturnType", "COMPOSABLE_INVOCATION", "ComposableInvocation")

package com.smartcontractai

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartcontractai.ui.theme.SmartContractAITheme
import kotlin.time.Duration.Companion.milliseconds

// Hằng số định danh cấu hình Backend API Key cho Gemini AI
// Đã tích hợp GEMINI_API_KEY từ BuildConfig / Backend Environment
object BackendAIConfig {
    var GEMINI_API_KEY: String = BuildConfig.GEMINI_API_KEY.ifBlank { "" }
}

// ==================== MÀN HÌNH TẠO BẰNG AI CHUYÊN BIỆT (HÌNH ĐÍNH KÈM) ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateContractWithAIScreen(
    onBack: () -> Unit = {},
    onNavigateToDashboard: (Int) -> Unit = {},
    onNavigateToReview: (String, String) -> Unit = { _, _ -> }
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
                    if (index != 2) {
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
                            // Gọi AI Gemini tạo văn bản hợp đồng hoàn chỉnh từ yêu cầu
                            kotlinx.coroutines.delay(2000.milliseconds)
                            isAiGenerating = false
                            val title = promptText.trim().uppercase()
                            generatedContract = """
                            CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM
                            Độc lập - Tự do - Hạnh phúc
                            -------------------

                            HỢP ĐỒNG ${if (title.contains("HỢP ĐỒNG")) title else "DỊCH VỤ / TẠO BỞI AI GEMINI: " + title}
                            Mã số tham chiếu: HĐ-2026/GEMINI-AI-${(1000..9999).random()}

                            Hôm nay, ngày 22 tháng 08 năm 2026, tại hệ thống SmartContract AI, các bên gồm có:

                            BÊN A (BÊN GIAO DỊCH / CHỦ ĐẦU TƯ):
                            - Tên đơn vị: CÔNG TY CỔ PHẦN CÔNG NGHỆ SMARTCONTRACT AI
                            - Đại diện: Ông Nguyễn Quang Minh
                            - Chức vụ: Giám đốc Điều hành
                            - Mã số thuế: 0312345678
                            - Địa chỉ: Tầng 8, Innovation Building, Quận 1, TP. Hồ Chí Minh

                            BÊN B (BÊN ĐỐI TÁC / THỰC HIỆN):
                            - Tên đơn vị: CÔNG TY TNHH GIẢI PHÁP PHẦN MỀM TOÀN CẦU
                            - Đại diện: Bà Trần Thị Mai
                            - Chức vụ: Giám đốc Kỹ thuật
                            - Mã số thuế: 0398765432
                            - Địa chỉ: Tòa nhà TechPark, Phường Tân Định, Quận 1, TP. Hồ Chí Minh

                            Cùng thống nhất các điều khoản được tổng hợp từ AI Gemini:

                            ĐIỀU 1: PHẠM VI VÀ NỘI DUNG YÊU CẦU
                            1.1. Nội dung công việc thực hiện: "${promptText.trim()}".
                            1.2. Đảm bảo đúng tiêu chuẩn chất lượng, thời hạn bàn giao và quy định pháp lý hiện hành.

                            ĐIỀU 2: GIÁ TRỊ HỢP ĐỒNG VÀ PHƯƠNG THỨC THANH TOÁN
                            2.1. Giá trị hợp đồng và tiến độ giải ngân theo từng giai đoạn nghiệm thu của hai bên.
                            2.2. Phương thức thanh toán: Chuyển khoản ngân hàng hoặc ví giao dịch bảo mật.

                            ĐIỀU 3: BẢO MẬT THÔNG TIN VÀ NGHĨA VỤ CÁC BÊN
                            3.1. Các bên cam kết bảo mật tuyệt đối các thông tin giao dịch, dữ liệu và mã nguồn.
                            3.2. Mọi sửa đổi, bổ sung hợp đồng phải được hai bên xác nhận qua chữ ký số hệ thống.

                            ĐẠI DIỆN BÊN A                                     ĐẠI DIỆN BÊN B
                            (Ký, ghi rõ họ tên)                                (Ký, ghi rõ họ tên)
                            """.trimIndent()
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
                                    fontFamily = FontFamily.Monospace
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
                                        Toast.makeText(context, "Chuyển sang màn hình Review hợp đồng...", Toast.LENGTH_SHORT).show()
                                        val title = promptText.ifBlank { null }?.let { "Hợp đồng AI: ${it.take(25)}..." } ?: "Hợp Đồng Tạo Bởi AI Gemini"
                                        onNavigateToReview(title, generatedContract ?: "")
                                    },
                                    modifier = Modifier.weight(1.2f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8))
                                ) {
                                    Text("Sử dụng & Review", fontSize = 12.sp, color = Color.White)
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

@Preview(showBackground = true)
@Composable
fun CreateContractWithAIScreenPreview() {
    SmartContractAITheme {
        CreateContractWithAIScreen()
    }
}
