package com.smartcontractai.utils

/**
 * Utility hỗ trợ trích xuất và thay thế các biến số {{Ten_Bien}} trong văn bản hợp đồng mẫu.
 */
object TemplateVariableParser {

    private val VARIABLE_REGEX = Regex("""\{\{([^}]+)\}\}""")

    data class ExtractedVariable(
        val key: String,
        val rawTag: String,
        val displayName: String
    )

    /**
     * Trích xuất danh sách tất cả các biến số {{...}} có trong văn bản
     */
    fun extractVariables(content: String): List<ExtractedVariable> {
        val matches = VARIABLE_REGEX.findAll(content)
        val list = mutableListOf<ExtractedVariable>()
        val seenKeys = mutableSetOf<String>()

        for (match in matches) {
            val key = match.groupValues[1].trim()
            if (key.isNotEmpty() && !seenKeys.contains(key)) {
                seenKeys.add(key)
                val displayName = formatDisplayName(key)
                list.add(ExtractedVariable(key = key, rawTag = match.value, displayName = displayName))
            }
        }
        return list
    }

    /**
     * Thay thế tất cả các thẻ {{Key}} trong văn bản bằng giá trị người dùng nhập vào Map
     */
    fun replaceVariables(content: String, valuesMap: Map<String, String>): String {
        var result = content
        val variables = extractVariables(content)

        for (variable in variables) {
            val value = valuesMap[variable.key]
            if (!value.isNullOrBlank()) {
                result = result.replace(variable.rawTag, value)
            }
        }
        return result
    }

    /**
     * Định dạng tên hiển thị thân thiện từ tên biến e.g. "Ten_Khach_Hang" -> "Tên Khách Hàng"
     */
    private fun formatDisplayName(key: String): String {
        return key.replace("_", " ")
            .replace(Regex("(?<=[a-z])(?=[A-Z])"), " ")
            .trim()
            .split(" ")
            .joinToString(" ") { word ->
                word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
    }
}
