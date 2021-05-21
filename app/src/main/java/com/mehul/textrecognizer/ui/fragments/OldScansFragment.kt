package com.mehul.textrecognizer.ui.fragments

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.mehul.textrecognizer.Adapter.OldScansAdapter
import com.mehul.textrecognizer.R
import com.mehul.textrecognizer.scans.mapToScan
import com.mehul.textrecognizer.ui.fragments.MainViewModel.CurrentFragment.OldScans
import kotlinx.android.synthetic.main.fragment_old_scans.*
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest

class OldScansFragment : Fragment() {

    private val mainViewModel by activityViewModels<MainViewModel>()
    private lateinit var navController: NavController

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_old_scans, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mainViewModel.setActiveReceiveFlow(OldScans, requireContext())

        lifecycleScope.launchWhenStarted {

            mainViewModel.shouldProceedFlow.collect { success ->

                if (success) {

                    lifecycleScope.launchWhenStarted {

                        async {

                            navController = findNavController()

                            val recyclerview =
                                view.findViewById<RecyclerView>(R.id.recyclerview_old_scans)

                            val adapter = OldScansAdapter(
                                context = requireContext(),
                                onLongClick = {
                                    mainViewModel.apply {
                                        sendOnLongClickFlow(requireContext(), it)

                                        toggleSelectMode()
                                    }
                                }
                            )

                            mainViewModel.selectionLiveData.observe(viewLifecycleOwner, Observer {

                                when (it) {
                                    is MainViewModel.SelectMode.ItemClickMode -> {
                                        adapter.setOnClick { scanMapper, _ ->

                                            val action =
                                                OldScansFragmentDirections.actionOldScansFragmentToDetailsFragment(
                                                    scanMapper.mapToScan()
                                                )
                                            findNavController().navigate(action)
                                        }
                                    }

                                    is MainViewModel.SelectMode.ItemSelectMode -> {
                                        adapter.setOnClick { scanMapper, _ ->

                                            mainViewModel.apply {

                                                updateScanIdList(scanMapper.id)
                                            }
                                        }
                                    }
                                }
                            })

                            recyclerview.adapter = adapter
                            recyclerview.setHasFixedSize(true)

                            mainViewModel.getScansSyncFlow(requireContext())

                            mainViewModel.godFlow.collectLatest {

                                it?.let { data ->
                                    adapter.submitData(data)
                                }
                            }
                        }

                        async {

                            mainViewModel.getAllIds(requireContext())

                            mainViewModel.hasAnyItems.collect { isEmpty ->

                                if(isEmpty) {

                                    no_recent_scans_gallery.visibility = View.GONE
                                    recyclerview_old_scans.visibility = View.VISIBLE

                                } else {

                                    no_recent_scans_gallery.visibility = View.VISIBLE
                                    recyclerview_old_scans.visibility = View.GONE
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}