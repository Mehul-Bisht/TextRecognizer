package com.mehul.textrecognizer.ui.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.asFlow
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.observe
import com.mehul.textrecognizer.R
import com.mehul.textrecognizer.ui.fragments.MainViewModel.CurrentFragment.TextToSpeech
import kotlinx.android.synthetic.main.fragment_text_to_speech.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class TextToSpeechFragment : Fragment() {

    val REQUEST_CODE = 900
    val interpolator = OvershootInterpolator()

    private val mainViewModel by activityViewModels<MainViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_text_to_speech, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mainViewModel.setActiveReceiveFlow(TextToSpeech, requireContext())

        lifecycleScope.launchWhenStarted {

            mainViewModel.shouldProceedFlow.collect { success ->

                if(success) {

                    mainViewModel.selectedPhotoBitmap.observe(viewLifecycleOwner, Observer {  bitmap ->
                        bitmap?.let {
                            bitmapgenerator.setImageBitmap(it)
                        }
                    })

                    mainViewModel.recognisedText.observe(viewLifecycleOwner, Observer {  text ->
                        text?.let {
                            recognizedText.text = it
                        }
                    })

                    mainViewModel.initTTS(requireContext())

                    speak.setOnClickListener {

                        mainViewModel.speak(viewLifecycleOwner)
                    }

                    share.setOnClickListener {

                        val flow = mainViewModel.recognisedText.asFlow()

                        val job = lifecycleScope.launchWhenStarted {

                            flow.collect { text ->

                                text?.let { recognizedText ->

                                    val intent = Intent(Intent.ACTION_SEND)
                                    intent.type = "text/plain"
                                    intent.putExtra(Intent.EXTRA_TEXT, recognizedText)
                                    val chooser = Intent.createChooser(intent,"Share via..")
                                    startActivity(chooser)
                                }
                            }
                        }

                        job.cancel()
                    }

                    bitmapgenerator.setOnClickListener {
                        val imagePickerIntent = Intent(Intent.ACTION_PICK)
                        imagePickerIntent.type = "image/*"
                        startActivityForResult(imagePickerIntent, REQUEST_CODE)
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(resultCode == Activity.RESULT_CANCELED)
            return

        else if(resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE && data != null) {
            mainViewModel.setUri(data.data)

            mainViewModel.getRecognisedText(
                requireActivity().contentResolver,
                requireContext()
            )
        }
    }

    override fun onStop() {
        mainViewModel.stopTTS()
        super.onStop()
    }

    override fun onDestroyView() {
        mainViewModel.stopTTS()
        mainViewModel.shutdownTTS()
        mainViewModel.reset()
        super.onDestroyView()
    }

    private fun speak() {
        mainViewModel.speakTTS(recognizedText.text.toString())
    }
}