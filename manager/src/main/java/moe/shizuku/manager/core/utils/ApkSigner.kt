package moe.shizuku.manager.utils

import android.content.pm.PackageManager
import com.android.apksig.ApkSigner
import java.io.File
import java.io.IOException
import java.math.BigInteger
import java.security.*
import java.security.KeyStore.PrivateKeyEntry
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.*
import moe.shizuku.manager.ShizukuApplication
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder

private val appContext = ShizukuApplication.appContext

object ApkSigner {

    private const val KEY_ALIAS = "key"
    private const val KEY_PASS = "keypass"
    private const val KEYSTORE_NAME = "signing-key.bks"
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
            .setV1SigningEnabled(false)
            .setV2SigningEnabled(true)
            .setV3SigningEnabled(false)
            .build()
            .sign()
    }

    fun getSigningKey(createIfNeeded: Boolean = false): PrivateKeyEntry =
        if (keystoreFile.exists()) {
            loadKey()
        } else try {
            getSelfSigningKey()
        } catch (e: IOException) {
            if (createIfNeeded) createSigningKey()
            else throw IllegalStateException("Signing key does not exist")
        }

    private fun getSelfSigningKey(): PrivateKeyEntry {
        appContext.assets.open(KEYSTORE_NAME).use { input ->
            keystoreFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        val key = loadKey()
        if (!key.matchesAppCert()) {
            throw IllegalStateException("Signing key does not match app certificate")
        }
        return key
    }

    private fun createSigningKey(): PrivateKeyEntry {
        val keystoreParameters = generatePrivateKeyAndCert()
        writeKeyStore(keystoreParameters)
        return loadKey()
    }

    val keystoreFile by lazy {
        File(appContext.filesDir, KEYSTORE_NAME)
    }

    private fun generatePrivateKeyAndCert(): PrivateKeyCertificatePair {
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

    private fun writeKeyStore(pair: PrivateKeyCertificatePair) {
        val ks = KeyStore.getInstance("BKS", BouncyCastleProvider.PROVIDER_NAME).apply {
            load(null)
            setKeyEntry(
                KEY_ALIAS,
                pair.privateKey,
                KEY_PASS.toCharArray(),
                arrayOf(pair.certificate)
            )
        }

        keystoreFile.outputStream().use {
            ks.store(it, KEYSTORE_PASS.toCharArray())
        }
    }

    private fun loadKey(): PrivateKeyEntry {
        val ks = KeyStore.getInstance("BKS", BouncyCastleProvider.PROVIDER_NAME).apply {
            load(keystoreFile.inputStream(), KEYSTORE_PASS.toCharArray())
        }

        return ks.getEntry(KEY_ALIAS, KeyStore.PasswordProtection(KEY_PASS.toCharArray()))
            as PrivateKeyEntry
    }

    private data class PrivateKeyCertificatePair(
        val privateKey: PrivateKey,
        val certificate: X509Certificate
    )

    private fun getAppCertificate(): X509Certificate {
        val info = appContext.packageManager.getPackageInfo(
            appContext.packageName,
            PackageManager.GET_SIGNING_CERTIFICATES
        )

        val signers = info.signingInfo?.apkContentsSigners ?: emptyArray()
        require(signers.isNotEmpty()) { "No signing certificates found" }

        val certBytes = signers[0].toByteArray()

        val factory = CertificateFactory.getInstance("X.509")
        return factory.generateCertificate(certBytes.inputStream()) as X509Certificate
    }

    private fun PrivateKeyEntry.matchesAppCert(): Boolean {
        val appCert = getAppCertificate()
        val keyCert = this.certificate as X509Certificate
        return appCert.publicKey.encoded.contentEquals(keyCert.publicKey.encoded)
    }

}