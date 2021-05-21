package com.mehul.textrecognizer.ui.fragments

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavArgs
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.bumptech.glide.TransitionOptions
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.mehul.textrecognizer.R
import com.mehul.textrecognizer.ui.fragments.MainViewModel.CurrentFragment.Details
import kotlinx.android.synthetic.main.fragment_details.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import java.io.File

class DetailsFragment : Fragment() {

    private val mainViewModel by activityViewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_details, container, false)
    }

    private val args: DetailsFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mainViewModel.wasAlreadyActive(Details)

        recognizedText_details.text = args.scan.recognisedText

        val file = File(requireContext().getExternalFilesDir(null), args.scan.filename)

        if(file.exists()) {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            Glide.with(requireContext())
                .load(bitmap)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(image)
        }
    }

    override fun onPause() {
        super.onPause()

        mainViewModel.resetSelected()
    }
}