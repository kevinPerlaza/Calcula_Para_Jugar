package com.example.calculaparajugar.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.calculaparajugar.databinding.ActivityPeriodicTableBinding

/** Explorador interactivo local de los 118 elementos químicos. */
class PeriodicTableActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPeriodicTableBinding

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPeriodicTableBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsets()

        binding.btnBackPeriodicTable.setOnClickListener { finish() }
        binding.btnBackPeriodicTable.applyPressFeedback()

        binding.periodicTableWebView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            builtInZoomControls = true
            displayZoomControls = false
        }
        binding.periodicTableWebView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = true
        }
        binding.periodicTableWebView.loadUrl("file:///android_asset/periodic_table.html")

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.periodicTableWebView.canGoBack()) binding.periodicTableWebView.goBack() else finish()
            }
        })
    }

    override fun onDestroy() {
        binding.periodicTableWebView.destroy()
        super.onDestroy()
    }
}
