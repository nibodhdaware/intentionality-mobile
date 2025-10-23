package com.nibodhdaware.intentionality.ui.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.*
import com.nibodhdaware.intentionality.R

class OverlayWindow(private val context: Context) {
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var onSubmitCallback: ((String, Int) -> Unit)? = null

    @SuppressLint("InflateParams")
    fun show(appName: String, packageName: String, onSubmit: (reason: String, rating: Int) -> Unit) {
        this.onSubmitCallback = onSubmit
        
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.CENTER
        
        // Make it focusable so user can interact
        params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()

        overlayView = LayoutInflater.from(context).inflate(R.layout.overlay_prompt, null)
        
        setupViews(appName, packageName)
        
        try {
            windowManager?.addView(overlayView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupViews(appName: String, packageName: String) {
        overlayView?.apply {
            val titleText = findViewById<TextView>(R.id.titleText)
            val reasonInput = findViewById<EditText>(R.id.reasonInput)
            val ratingSpinner = findViewById<Spinner>(R.id.ratingSpinner)
            val submitButton = findViewById<Button>(R.id.submitButton)
            val cancelButton = findViewById<Button>(R.id.cancelButton)

            titleText.text = "Why are you opening $appName?"

            // Setup spinner with ratings
            val ratings = arrayOf(
                "1 - Very intentional",
                "2 - Somewhat intentional",
                "3 - Not intentional",
                "4 - Mindless",
                "5 - Regretful"
            )
            
            val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, ratings)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            ratingSpinner.adapter = adapter
            ratingSpinner.setSelection(0)

            submitButton.setOnClickListener {
                val reason = reasonInput.text.toString()
                val rating = ratingSpinner.selectedItemPosition + 1
                onSubmitCallback?.invoke(reason, rating)
                dismiss()
            }

            cancelButton.setOnClickListener {
                dismiss()
            }
        }
    }

    fun dismiss() {
        try {
            if (overlayView != null && overlayView?.windowToken != null) {
                windowManager?.removeView(overlayView)
            }
            overlayView = null
            windowManager = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isShowing(): Boolean {
        return overlayView != null
    }
}

