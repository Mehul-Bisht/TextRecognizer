package com.mehul.textrecognizer

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.Toast
import androidx.core.graphics.drawable.toBitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity() {

    val REQUEST_CODE = 900
    val interpolator = OvershootInterpolator()

    lateinit var tts: TextToSpeech

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportActionBar?.hide()

        tts = TextToSpeech(this, TextToSpeech.OnInitListener {
            //check status
            if (it == TextToSpeech.SUCCESS) {
                val result = tts.setLanguage(Locale.ENGLISH)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED)
                    Log.e("MainActivity", "Language not supported")
                else {
                   // fab.isEnabled = true
                }
            } else {
                Log.e("MainActivity", "Initialization Failed")
            }

        })

        bitmapgenerator.setOnClickListener {
            clickEvent(it)
        }

        fab.setOnClickListener{
           speak()
            fab.animate().scaleX(1.2f).scaleY(1.2f).setInterpolator(interpolator).setDuration(100).start()
            Handler().postDelayed({

                fab.animate().scaleX(1f).scaleY(1f).setInterpolator(interpolator).setDuration(100).start()

            },100)


        }

    }

    fun clickEvent(view : View){
        val imagePickerIntent = Intent(Intent.ACTION_PICK)
        imagePickerIntent.type = "image/*"
        startActivityForResult(imagePickerIntent, REQUEST_CODE)
    }

    private fun speak() {
        val text = recognizedText.text.toString()
        val pitch: Float = 1f
        val speed: Float = 1f

        tts.setPitch(pitch)
        tts.setSpeechRate(speed)

        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null)


    }

    var selectedPhotoUri : Uri? = null
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)


        if(resultCode == Activity.RESULT_CANCELED)
            return

        else if(resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE && data != null){
         selectedPhotoUri = data.data

            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, selectedPhotoUri)
            bitmapgenerator.setImageBitmap(bitmap)
            selector.visibility = View.GONE
            selector.isEnabled = false

            fab.translationX = 100f
            fab.alpha = 0f

            val bmp = bitmapgenerator.drawable.toBitmap()
            val image = InputImage.fromBitmap(bmp, 0)
            val recognizer = TextRecognition.getClient()

            recognizer.process(image).addOnSuccessListener {
                recognizedText.text = it.text

                Handler().postDelayed({

                    fab.animate().translationX(-100f).alpha(1f).setInterpolator(interpolator).setDuration(400).start()

                },1500)

            }
                .addOnFailureListener {
                    Toast.makeText(this, it.message, Toast.LENGTH_LONG).show()
                }

        }
    }

    override fun onStop() {
        tts.stop()
        super.onStop()
    }

    override fun onBackPressed() {
        if(tts.isSpeaking)
        tts.stop()

        else {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Confirm action")
                .setMessage("Do you want to exit ?")
                .setNegativeButton("No") { _: DialogInterface?, _: Int ->

            }
                .setPositiveButton("Exit") { _: DialogInterface?, _: Int ->
                    super.onBackPressed()
                }
            builder.show()
        }
    }

    override fun onDestroy() {
        if (tts != null) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }

}