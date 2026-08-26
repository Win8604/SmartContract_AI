package com.smartcontractai

import com.smartcontractai.utils.AsyncDigitalSignatureHelper
import com.smartcontractai.utils.DigitalSignatureHelper
import com.smartcontractai.utils.ECDSASignatureHelper
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DigitalSignatureTest {

    @Test
    fun testRsaDigitalSignature() {
        val originalMessage = "Hợp đồng chuyển nhượng quyền sở hữu #98765"
        val keyPair = DigitalSignatureHelper.generateKeyPair(2048)

        // Ký số
        val signature = DigitalSignatureHelper.sign(originalMessage, keyPair.private)
        assertTrue(signature.isNotEmpty())

        // Xác minh chữ ký hợp lệ
        val isValid = DigitalSignatureHelper.verify(originalMessage, signature, keyPair.public)
        assertTrue("Chữ ký RSA phải hợp lệ với văn bản gốc", isValid)

        // Xác minh thất bại khi văn bản bị sửa đổi
        val tamperedMessage = "Hợp đồng chuyển nhượng quyền sở hữu #98765 (đã sửa đổi)"
        val isTamperedValid = DigitalSignatureHelper.verify(tamperedMessage, signature, keyPair.public)
        assertFalse("Chữ ký RSA phải không hợp lệ khi văn bản bị sửa đổi", isTamperedValid)
    }

    @Test
    fun testEcdsaDigitalSignature() {
        val originalMessage = "Giao dịch Smart Contract 500 ETH"
        val keyPair = ECDSASignatureHelper.generateECKeyPair()

        // Ký số ECDSA
        val signature = ECDSASignatureHelper.sign(originalMessage, keyPair.private)
        assertTrue(signature.isNotEmpty())

        // Xác minh chữ ký hợp lệ
        val isValid = ECDSASignatureHelper.verify(originalMessage, signature, keyPair.public)
        assertTrue("Chữ ký ECDSA phải hợp lệ với văn bản gốc", isValid)

        // Xác minh thất bại khi văn bản bị sửa đổi
        val tamperedMessage = "Giao dịch Smart Contract 5000 ETH"
        val isTamperedValid = ECDSASignatureHelper.verify(tamperedMessage, signature, keyPair.public)
        assertFalse("Chữ ký ECDSA phải không hợp lệ khi văn bản bị sửa đổi", isTamperedValid)
    }

    @Test
    fun testAsyncDigitalSignature() = runBlocking {
        val originalMessage = "Xác nhận giao dịch thanh toán mượt mà trên Dispatchers.Default"
        val keyPair = AsyncDigitalSignatureHelper.generateECKeyPairAsync()

        // Ký số bất đồng bộ
        val signature = AsyncDigitalSignatureHelper.signAsync(originalMessage, keyPair.private, useECDSA = true)
        assertTrue(signature.isNotEmpty())

        // Xác minh bất đồng bộ
        val isValid = AsyncDigitalSignatureHelper.verifyAsync(originalMessage, signature, keyPair.public, useECDSA = true)
        assertTrue("Chữ ký bất đồng bộ phải hợp lệ với văn bản gốc", isValid)
    }
}
