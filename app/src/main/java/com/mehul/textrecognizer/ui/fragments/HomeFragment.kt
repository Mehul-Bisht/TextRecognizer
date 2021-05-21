package com.mehul.textrecognizer.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.mehul.textrecognizer.Adapter.HomeScansAdapter
import com.mehul.textrecognizer.R
import com.mehul.textrecognizer.ui.fragments.MainViewModel.CurrentFragment.Home
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest

class HomeFragment : Fragment() {

    private val mainViewModel by activityViewModels<MainViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mainViewModel.setActiveReceiveFlow(Home,requireContext())

        lifecycleScope.launchWhenStarted {

            mainViewModel.shouldProceedToHomeFlow.collect { success ->

                if(success) {

                    lifecycleScope.launchWhenStarted {

                        async {

                            val adapter = HomeScansAdapter(requireContext())
                            recyclerview.adapter = adapter

                            mainViewModel.getScansFlow(requireContext()).collectLatest {

                                adapter.submitData(it)
                            }
                        }

                        async {

                            mainViewModel.getAllIds(requireContext())

                            mainViewModel.hasAnyItems.collect { isEmpty ->

                                if(isEmpty) {

                                    no_recent_scans.visibility = View.GONE
                                    recyclerview.visibility = View.VISIBLE

                                } else {

                                    no_recent_scans.visibility = View.VISIBLE
                                    recyclerview.visibility = View.GONE
                                }
                            }
                        }

//                        async {
//
//                            view_all.setOnClickListener {
//
//                                val action =
//                                    HomeFragmentDirections.actionHomeFragmentToOldScansFragment()
//
//                                findNavController().navigate(action)
//                            }
//                        }
                    }
                }
            }
        }
    }
}