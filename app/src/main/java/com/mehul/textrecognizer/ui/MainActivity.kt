package com.mehul.textrecognizer.ui

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.mehul.textrecognizer.R
import com.mehul.textrecognizer.scans.State
import com.mehul.textrecognizer.ui.fragments.MainViewModel
import com.mehul.textrecognizer.ui.fragments.MainViewModel.ActionModeStatus.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var listener: NavController.OnDestinationChangedListener
    private val mainViewModel by viewModels<MainViewModel>()
    private var actionMode: ActionMode? = null
    private var alertDialogBuilder: AlertDialog.Builder? = null
    private var mMenu: Menu? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        window.statusBarColor = resources.getColor(R.color.colorPrimaryDark)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragment_container_view)
                as NavHostFragment
        navController = navHostFragment.findNavController()
        drawerLayout = findViewById(R.id.drawerLayout)

        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.homeFragment, R.id.textToSpeechFragment, R.id.oldScansFragment),
            drawerLayout
        )

        listener =
            NavController.OnDestinationChangedListener { _, destination, _ ->

            }

        nav_view.setupWithNavController(navController)
        setupActionBarWithNavController(navController, appBarConfiguration)

        alertDialogBuilder = AlertDialog.Builder(this)

        lifecycleScope.launchWhenStarted {

            async {

                mainViewModel.actionModeStateFlow.collect {

                    when (it) {

                        is Uninitialised -> {

                            Log.d("launch ", "Uninitialised")
                            actionMode = null
                            window.statusBarColor = resources.getColor(R.color.colorPrimaryDark)
                        }

                        is Initialised -> {

                            Log.d("launch ", "Initialised")
                            if (actionMode != null) {
                                return@collect
                            }

                            actionMode = startSupportActionMode(getActionModeCallBack())

                        }

                        is Created -> {

                            Log.d("launch ", "Created")

                            window.statusBarColor =
                                resources.getColor(R.color.statusBarColor)

                            if (actionMode != null) {
                                return@collect
                            }
                            actionMode = startSupportActionMode(getActionModeCallBack())
                        }

                        is Destroyed -> {

                            Log.d("launch ", "Destroyed")

                            window.statusBarColor = resources.getColor(R.color.colorPrimaryDark)
                        }
                    }
                }
            }

            async {

                mainViewModel.actionModeForceDestroyFlow.collect {

                    if (it) {

                        window.statusBarColor = resources.getColor(R.color.colorPrimaryDark)

                        actionMode?.finish()
                    }
                }
            }

            async {

                lifecycleScope.launchWhenStarted {

                    mainViewModel.selectedItems.collect { count ->

                        mMenu?.findItem(R.id.counter)?.title = "$count selected"
                    }
                }
            }
        }

        drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {

            override fun onDrawerClosed(drawerView: View) {

                Log.d("Activity ","onDrawerClosed")
                mainViewModel.drawerUpdater(false)
            }

            override fun onDrawerStateChanged(newState: Int) {

            }

            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {

            }

            override fun onDrawerOpened(drawerView: View) {

                mainViewModel.drawerUpdater(true)
            }
        })
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    override fun onResume() {
        super.onResume()

        navController.addOnDestinationChangedListener(listener)
    }

    override fun onPause() {
        super.onPause()

        navController.removeOnDestinationChangedListener(listener)
    }

    private fun getActionModeCallBack() = object : ActionMode.Callback {

        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {

            mode?.menuInflater?.inflate(R.menu.context_menu, menu)
            mode?.title = "Select scans"
            mainViewModel.setActionModeState(Created)
            mMenu = menu
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            return false
        }

        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            return when (item?.itemId) {

                R.id.delete -> {

                    val job = lifecycleScope.launchWhenStarted {

                        mainViewModel.selectedItems.collect { count ->

                            if (count > 0) {

                                alertDialogBuilder
                                    ?.setTitle("Delete Items")
                                    ?.setMessage("Are you sure you want to delete these items? Deleted items cannot be restored as of this version of the app.")
                                    ?.setCancelable(false)
                                    ?.setPositiveButton("Yes, Delete") { _, _ ->

                                        Toast.makeText(
                                            this@MainActivity,
                                            "Selected items have been deleted",
                                            Toast.LENGTH_SHORT
                                        ).show()

                                        mainViewModel.deleteItems(this@MainActivity)
                                    }
                                    ?.setNegativeButton("No, Keep items") { _, _ ->

                                    }
                                    ?.create()
                                    ?.show()
                            } else {

                                Toast.makeText(
                                    this@MainActivity,
                                    "Select atleast 1 item to delete",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }

                    job.cancel()

                    true
                }
                else -> {
                    false
                }
            }
        }

        override fun onDestroyActionMode(mode: ActionMode?) {

            actionMode = null
            mainViewModel.setActionModeDestroyFlow(false)

            mainViewModel.apply {

                setItemsState(State.UNINITIALISED)
                resetSelected()
            }
        }
    }

    override fun onBackPressed() {

        if (drawerLayout.isDrawerOpen(GravityCompat.START))
            mainViewModel.toggleDrawerMode(true)
        else
            mainViewModel.toggleDrawerMode(false)

        mainViewModel.getBackStatusFlow()
        lifecycleScope.launchWhenStarted {

            mainViewModel.backStateFlow.collect {

                when (it) {

                    is MainViewModel.BackState.DrawerOpen -> {
                        drawerLayout.closeDrawer(GravityCompat.START)
                        Log.d("Hello ", "DrawerOpen")
                    }

                    is MainViewModel.BackState.TtsStatus -> {
                        mainViewModel.stopTTS()
                        Log.d("Hello ", "TtsStatus")
                    }
                    is MainViewModel.BackState.ItemSelect -> {
                        mainViewModel.apply {
                            resetSelected()
                        }
                        Log.d("Hello ", "ItemSelect")
                    }
                    is MainViewModel.BackState.None -> {
                        Log.d("Hello ", "None (super.onBackPressed())")
                        mainViewModel.setBackPressFlow()
                        super.onBackPressed()
                    }
                    else -> {
                        Log.d("Hello ", "final super.onBackPressed()")
                        super.onBackPressed()
                    }
                }
            }

        }
    }
}