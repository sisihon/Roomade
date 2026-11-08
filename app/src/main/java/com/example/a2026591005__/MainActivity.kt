package com.example.a2026591005__

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView

import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat

import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

import org.json.JSONObject

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@ExperimentalGetImage
class MainActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var txtScanResult: TextView
    private lateinit var txtSelectedCategory: TextView

    private lateinit var edtProductName: EditText
    private lateinit var edtQuantity: EditText
    private lateinit var edtExpiryDate: EditText
    private lateinit var expiryLayout: LinearLayout

    private lateinit var rbPersonal: RadioButton
    private lateinit var rbShared: RadioButton

    private lateinit var cameraExecutor: ExecutorService
    private val lookupExecutor = Executors.newSingleThreadExecutor()

    private var selectedCategory: String? = null

    private val qrDetected = AtomicBoolean(false)

    private val serverAddress = "http://172.30.1.19:3000"

    private val localNetworkPermission =
        "android.permission.ACCESS_LOCAL_NETWORK"

    private val scanner by lazy {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()

        BarcodeScanning.getClient(options)
    }

    private val requestCameraPermission =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                startCamera()
            } else {
                txtScanResult.text = "카메라 권한이 필요합니다."
            }
        }

    private val requestLocalNetworkPermission =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (!granted) {
                txtScanResult.text =
                    "로컬 네트워크 권한이 필요합니다."
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.previewView)
        txtScanResult = findViewById(R.id.txtScanResult)
        txtSelectedCategory =
            findViewById(R.id.txtSelectedCategory)

        edtProductName = findViewById(R.id.edtProductName)
        edtQuantity = findViewById(R.id.edtQuantity)
        edtExpiryDate = findViewById(R.id.edtExpiryDate)
        expiryLayout = findViewById(R.id.expiryLayout)

        rbPersonal = findViewById(R.id.rbPersonal)
        rbShared = findViewById(R.id.rbShared)

        cameraExecutor = Executors.newSingleThreadExecutor()

        findViewById<Button>(R.id.btnFood).setOnClickListener {
            selectCategory("FOOD")
        }

        findViewById<Button>(R.id.btnSupply).setOnClickListener {
            selectCategory("SUPPLY")
        }

        findViewById<Button>(R.id.btnSaveItem).setOnClickListener {
            saveItem()
        }

        checkLocalNetworkPermission()
    }

    // 로컬 네트워크 권한 확인
    private fun checkLocalNetworkPermission() {
        if (Build.VERSION.SDK_INT >= 37) {
            if (
                checkSelfPermission(localNetworkPermission) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestLocalNetworkPermission.launch(
                    localNetworkPermission
                )
            }
        }
    }

    // 물품 종류 선택
    private fun selectCategory(category: String) {
        selectedCategory = category
        qrDetected.set(false)

        txtScanResult.text = "QR 코드를 카메라에 비춰주세요."

        edtProductName.text.clear()
        edtQuantity.text.clear()
        edtExpiryDate.text.clear()

        if (category == "FOOD") {
            txtSelectedCategory.text = "선택한 종류: 식료품"
            expiryLayout.visibility = View.VISIBLE
        } else {
            txtSelectedCategory.text = "선택한 종류: 생활용품"
            expiryLayout.visibility = View.GONE
        }

        checkCameraPermission()
    }

    // 카메라 권한 확인
    private fun checkCameraPermission() {
        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            requestCameraPermission.launch(
                Manifest.permission.CAMERA
            )
        }
    }

    // 카메라 실행
    private fun startCamera() {
        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({

            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(
                    ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
                )
                .build()

            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->

                if (qrDetected.get()) {
                    imageProxy.close()
                    return@setAnalyzer
                }

                val mediaImage = imageProxy.image

                if (mediaImage == null) {
                    imageProxy.close()
                    return@setAnalyzer
                }

                val image = InputImage.fromMediaImage(
                    mediaImage,
                    imageProxy.imageInfo.rotationDegrees
                )

                scanner.process(image)
                    .addOnSuccessListener { barcodes ->

                        val qrValue = barcodes
                            .firstOrNull {
                                it.format == Barcode.FORMAT_QR_CODE
                            }
                            ?.rawValue

                        if (
                            qrValue != null &&
                            qrDetected.compareAndSet(false, true)
                        ) {
                            runOnUiThread {
                                txtScanResult.text =
                                    "QR 인식 성공!\n상품명 조회 중..."
                            }

                            lookupProductName(qrValue)
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("QR_SCAN", "QR 인식 실패", e)
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            }

            try {
                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis
                )

            } catch (e: Exception) {
                Log.e("CAMERA", "카메라 실행 실패", e)
                txtScanResult.text =
                    "카메라 실행 실패: ${e.message}"
            }

        }, ContextCompat.getMainExecutor(this))
    }

    // 서버에서 상품명 가져오기
    private fun lookupProductName(qrValue: String) {

        lookupExecutor.execute {

            var connection: HttpURLConnection? = null

            try {
                val encodedUrl =
                    URLEncoder.encode(qrValue, "UTF-8")

                val apiUrl =
                    "$serverAddress/items/lookup?url=$encodedUrl"

                connection =
                    URL(apiUrl).openConnection() as HttpURLConnection

                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val responseCode = connection.responseCode

                val responseStream =
                    if (responseCode in 200..299) {
                        connection.inputStream
                    } else {
                        connection.errorStream
                    }

                val responseText = responseStream
                    ?.bufferedReader()
                    ?.use { it.readText() }
                    ?: ""

                val json = JSONObject(responseText)

                val productName = json
                    .optString("product_name", "")
                    .takeIf {
                        it.isNotBlank() && it != "null"
                    }

                runOnUiThread {
                    if (isFinishing || isDestroyed) {
                        return@runOnUiThread
                    }

                    if (
                        responseCode in 200..299 &&
                        productName != null
                    ) {
                        edtProductName.setText(productName)

                        txtScanResult.text =
                            "상품명 조회 성공!\n$productName"
                    } else {
                        txtScanResult.text =
                            "상품명을 가져오지 못했습니다.\n$responseText"
                    }
                }

            } catch (e: Exception) {
                Log.e("PRODUCT_LOOKUP", "상품명 조회 실패", e)

                runOnUiThread {
                    if (isFinishing || isDestroyed) {
                        return@runOnUiThread
                    }

                    txtScanResult.text =
                        "상품명 조회 실패\n${e.message}"
                }

            } finally {
                connection?.disconnect()
            }
        }
    }

    // 물품 등록
    private fun saveItem() {

        val productName =
            edtProductName.text.toString().trim()

        val quantityText =
            edtQuantity.text.toString().trim()

        if (selectedCategory == null) {
            txtScanResult.text =
                "먼저 식료품 또는 생활용품을 선택해 주세요."
            return
        }

        if (productName.isBlank()) {
            txtScanResult.text = "상품명을 입력해 주세요."
            return
        }

        val quantity = quantityText.toIntOrNull()

        if (quantity == null || quantity <= 0) {
            txtScanResult.text =
                "수량은 1 이상의 숫자로 입력해 주세요."
            return
        }

        // 유통기한은 선택사항
        val expiryText =
            edtExpiryDate.text.toString().trim()

        val expiryDate: Any = if (expiryText.isBlank()) {
            JSONObject.NULL
        } else {
            // YYYY-MM-DD 형식 및 실제 날짜 확인
            try {
                if (!expiryText.matches(
                        Regex("""\d{4}-\d{2}-\d{2}""")
                    )
                ) {
                    txtScanResult.text =
                        "유통기한은 YYYY-MM-DD 형식으로 입력해 주세요."
                    return
                }

                LocalDate.parse(expiryText)
                expiryText

            } catch (e: DateTimeParseException) {
                txtScanResult.text =
                    "올바른 날짜를 입력해 주세요. 예: 2026-12-31"
                return
            }
        }

        // 개인 / 공용 선택
        val ownershipType =
            if (rbPersonal.isChecked) {
                "PERSONAL"
            } else {
                "SHARED"
            }

        // 서버로 전송할 JSON
        val json = JSONObject().apply {
            put("product_name", productName)
            put("category", selectedCategory)
            put("quantity", quantity)

            // 빈칸이면 NULL, 입력하면 날짜 저장
            put(
                "expiry_date",
                if (selectedCategory == "FOOD") {
                    expiryDate
                } else {
                    JSONObject.NULL
                }
            )

            put("ownership_type", ownershipType)
        }

        txtScanResult.text = "물품 등록 중..."

        lookupExecutor.execute {

            var connection: HttpURLConnection? = null

            try {
                connection =
                    URL("$serverAddress/items")
                        .openConnection() as HttpURLConnection

                connection.requestMethod = "POST"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.doOutput = true

                connection.setRequestProperty(
                    "Content-Type",
                    "application/json; charset=UTF-8"
                )

                connection.outputStream.use {
                    it.write(
                        json.toString().toByteArray(Charsets.UTF_8)
                    )
                }

                val responseCode = connection.responseCode

                val responseStream =
                    if (responseCode in 200..299) {
                        connection.inputStream
                    } else {
                        connection.errorStream
                    }

                val responseText = responseStream
                    ?.bufferedReader()
                    ?.use { it.readText() }
                    ?: ""

                runOnUiThread {
                    if (isFinishing || isDestroyed) {
                        return@runOnUiThread
                    }

                    if (responseCode in 200..299) {

                        txtScanResult.text =
                            "물품 등록 성공!\n$responseText"

                        edtProductName.text.clear()
                        edtQuantity.text.clear()
                        edtExpiryDate.text.clear()

                        rbPersonal.isChecked = true

                        qrDetected.set(false)

                    } else {
                        txtScanResult.text =
                            "물품 등록 실패\n$responseText"
                    }
                }

            } catch (e: Exception) {
                Log.e("ITEM_SAVE", "물품 등록 실패", e)

                runOnUiThread {
                    if (isFinishing || isDestroyed) {
                        return@runOnUiThread
                    }

                    txtScanResult.text =
                        "서버 통신 실패\n${e.message}"
                }

            } finally {
                connection?.disconnect()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        if (::cameraExecutor.isInitialized) {
            cameraExecutor.shutdown()
        }

        lookupExecutor.shutdown()
        scanner.close()
    }
}