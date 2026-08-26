package com.smartcontractai.utils

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.util.Base64

/**
 * Utility hỗ trợ tạo và xác thực Chữ ký số (Digital Signature)
 * sử dụng thuật toán băm SHA-256 kết hợp RSA.
 */
object DigitalSignatureHelper {

    private const val ALGORITHM_RSA = "RSA"
    private const val SIGNATURE_ALGORITHM_RSA = "SHA256withRSA"

    /**
     * Tạo cặp khóa RSA (Key Size mặc định: 2048 bit)
     */
    fun generateKeyPair(keySize: Int = 2048): KeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance(ALGORITHM_RSA)
        keyPairGenerator.initialize(keySize)
        return keyPairGenerator.generateKeyPair()
    }

    /**
     * Ký số văn bản / dữ liệu bằng Private Key (SHA256withRSA)
     *
     * @param data Chuỗi dữ liệu cần ký
     * @param privateKey Khóa bí mật dùng để ký
     * @return Chuỗi chữ ký số mã hóa Base64
     */
    fun sign(data: String, privateKey: PrivateKey): String {
        val signature = Signature.getInstance(SIGNATURE_ALGORITHM_RSA)
        signature.initSign(privateKey)
        signature.update(data.toByteArray(Charsets.UTF_8))

        val signatureBytes = signature.sign()
        return Base64.getEncoder().encodeToString(signatureBytes)
    }

    /**
     * Xác thực chữ ký số bằng Public Key (SHA256withRSA)
     *
     * @param data Dữ liệu gốc cần kiểm tra
     * @param signatureBase64 Chữ ký số dạng Base64
     * @param publicKey Khóa công khai dùng để xác thực
     * @return true nếu chữ ký hợp lệ và dữ liệu chưa bị chỉnh sửa
     */
    fun verify(data: String, signatureBase64: String, publicKey: PublicKey): Boolean {
        return try {
            val signature = Signature.getInstance(SIGNATURE_ALGORITHM_RSA)
            signature.initVerify(publicKey)
            signature.update(data.toByteArray(Charsets.UTF_8))

            val signatureBytes = Base64.getDecoder().decode(signatureBase64)
            signature.verify(signatureBytes)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

/**
 * Utility hỗ trợ chữ ký số dùng ECDSA (Elliptic Curve Digital Signature Algorithm) với SHA-256.
 * Phù hợp cho thiết bị di động / Blockchain do kích thước chữ ký nhỏ và tốc độ tính toán nhanh.
 */
object ECDSASignatureHelper {

    private const val ALGORITHM_EC = "EC"
    private const val SIGNATURE_ALGORITHM_ECDSA = "SHA256withECDSA"

    /**
     * Tạo cặp khóa EC (secp256r1 - 256 bit)
     */
    fun generateECKeyPair(): KeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance(ALGORITHM_EC)
        keyPairGenerator.initialize(256)
        return keyPairGenerator.generateKeyPair()
    }

    /**
     * Ký số văn bản / dữ liệu bằng Private Key EC (SHA256withECDSA)
     */
    fun sign(data: String, privateKey: PrivateKey): String {
        val signature = Signature.getInstance(SIGNATURE_ALGORITHM_ECDSA)
        signature.initSign(privateKey)
        signature.update(data.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(signature.sign())
    }

    /**
     * Xác thực chữ ký số bằng Public Key EC (SHA256withECDSA)
     */
    fun verify(data: String, signatureBase64: String, publicKey: PublicKey): Boolean {
        return try {
            val signature = Signature.getInstance(SIGNATURE_ALGORITHM_ECDSA)
            signature.initVerify(publicKey)
            signature.update(data.toByteArray(Charsets.UTF_8))
            val signatureBytes = Base64.getDecoder().decode(signatureBase64)
            signature.verify(signatureBytes)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

/**
 * Helper thực hiện tác vụ ký số bất đồng bộ với Coroutines (Dispatchers.Default),
 * giúp ứng dụng chạy mượt mà không gây giật/đơ giao diện trên thiết bị thực tế.
 */
object AsyncDigitalSignatureHelper {

    private const val SIGNATURE_ALGORITHM_ECDSA = "SHA256withECDSA"
    private const val SIGNATURE_ALGORITHM_RSA = "SHA256withRSA"

    /**
     * Sinh khóa trên background thread (Chạy mượt, không giật UI)
     */
    suspend fun generateECKeyPairAsync(): KeyPair = withContext(Dispatchers.Default) {
        val keyPairGenerator = KeyPairGenerator.getInstance("EC")
        keyPairGenerator.initialize(256) // secp256r1
        keyPairGenerator.generateKeyPair()
    }

    /**
     * Ký số bất đồng bộ
     */
    suspend fun signAsync(data: String, privateKey: PrivateKey, useECDSA: Boolean = true): String = withContext(Dispatchers.Default) {
        val algorithm = if (useECDSA) SIGNATURE_ALGORITHM_ECDSA else SIGNATURE_ALGORITHM_RSA
        val signature = Signature.getInstance(algorithm)
        signature.initSign(privateKey)
        signature.update(data.toByteArray(Charsets.UTF_8))
        Base64.getEncoder().encodeToString(signature.sign())
    }

    /**
     * Xác minh chữ ký bất đồng bộ
     */
    suspend fun verifyAsync(data: String, signatureBase64: String, publicKey: PublicKey, useECDSA: Boolean = true): Boolean = withContext(Dispatchers.Default) {
        try {
            val algorithm = if (useECDSA) SIGNATURE_ALGORITHM_ECDSA else SIGNATURE_ALGORITHM_RSA
            val signature = Signature.getInstance(algorithm)
            signature.initVerify(publicKey)
            signature.update(data.toByteArray(Charsets.UTF_8))
            val signatureBytes = Base64.getDecoder().decode(signatureBase64)
            signature.verify(signatureBytes)
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * Helper lưu trữ và khởi tạo cặp khóa an toàn trong phần cứng thiết bị (Android KeyStore TEE/StrongBox).
 */
object AndroidKeyStoreHelper {

    private const val KEY_ALIAS = "SmartContractUserKey"
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"

    /**
     * Khởi tạo và lưu Key trong Chip bảo mật phần cứng của điện thoại (chỉ gọi 1 lần)
     */
    fun getOrCreateHardwareKeyPair(): Pair<PublicKey, PrivateKey> {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }

        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyPairGenerator = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_EC, KEYSTORE_PROVIDER
            )
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
            )
                .setDigests(KeyProperties.DIGEST_SHA256)
                .build()

            keyPairGenerator.initialize(spec)
            keyPairGenerator.generateKeyPair()
        }

        val privateKey = keyStore.getKey(KEY_ALIAS, null) as PrivateKey
        val publicKey = keyStore.getCertificate(KEY_ALIAS).publicKey
        return Pair(publicKey, privateKey)
    }
}
