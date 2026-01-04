package moe.shizuku.manager.utils

import com.android.apksig.ApkSigner
import java.io.File
import java.math.BigInteger
import java.security.*
import java.security.KeyStore.PrivateKeyEntry
import java.security.cert.X509Certificate
import java.util.*
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder

object ApkSigner {

    private const val KEY_ALIAS = "key"
    private const val KEY_PASS = "keypass"
    private const val KEYSTORE_PASS = "storepass"

    init {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
            Security.addProvider(BouncyCastleProvider())
    }

    fun sign(input: File, output: File, key: PrivateKeyEntry) {
        val cert = key.certificate as X509Certificate

        ApkSigner.Builder(
            listOf(
                ApkSigner.SignerConfig.Builder(
                    "ShizukuSigner",
                    key.privateKey,
                    listOf(cert)
                ).build()
            )
        )
            .setInputApk(input)
            .setOutputApk(output)
            .setV1SigningEnabled(true)
            .setV2SigningEnabled(true)
            .setV3SigningEnabled(true)
            .build()
            .sign()
    }

    fun getOrCreateSigningKey(dir: File): PrivateKeyEntry {
        val file = keystoreFile(dir)
        return if (file.exists()) {
            loadKey(file)
        } else {
            val pair = generateKeyPairAndCert()
            writeKeyStore(file, pair)
            loadKey(file)
        }
    }

    private fun keystoreFile(dir: File): File =
        File(dir, "signing-key.bks")

    private fun generateKeyPairAndCert(): PrivateKeyCertificatePair {
        val keyPair = KeyPairGenerator.getInstance("RSA").apply {
            initialize(2048)
        }.generateKeyPair()

        val now = Date()
        val validUntil = Date(now.time + 3650L * 24 * 60 * 60 * 1000)

        val name = X500Name("CN=Shizuku")
        val certHolder = X509v3CertificateBuilder(
            name,
            BigInteger.valueOf(SecureRandom().nextLong()),
            now,
            validUntil,
            Locale.ENGLISH,
            name,
            SubjectPublicKeyInfo.getInstance(keyPair.public.encoded)
        ).build(
            JcaContentSignerBuilder("SHA256withRSA")
                .build(keyPair.private)
        )

        val cert = JcaX509CertificateConverter().getCertificate(certHolder)

        return PrivateKeyCertificatePair(keyPair.private, cert)
    }

    private fun writeKeyStore(file: File, pair: PrivateKeyCertificatePair) {
        val ks = KeyStore.getInstance("BKS", BouncyCastleProvider.PROVIDER_NAME).apply {
            load(null)
            setKeyEntry(
                KEY_ALIAS,
                pair.privateKey,
                KEY_PASS.toCharArray(),
                arrayOf(pair.certificate)
            )
        }

        file.outputStream().use {
            ks.store(it, KEYSTORE_PASS.toCharArray())
        }
    }

    private fun loadKey(file: File): PrivateKeyEntry {
        val ks = KeyStore.getInstance("BKS", BouncyCastleProvider.PROVIDER_NAME).apply {
            load(file.inputStream(), KEYSTORE_PASS.toCharArray())
        }

        return ks.getEntry(KEY_ALIAS, KeyStore.PasswordProtection(KEY_PASS.toCharArray()))
                as PrivateKeyEntry
    }

    private data class PrivateKeyCertificatePair(
        val privateKey: PrivateKey,
        val certificate: X509Certificate
    )
}