package com.mehul.textrecognizer.ui.fragments

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.*
import androidx.lifecycle.Observer
import androidx.paging.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.mehul.textrecognizer.scans.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import java.io.*
import java.util.*

class MainViewModel : ViewModel() {

    private val _tts: MutableLiveData<TextToSpeech> = MutableLiveData()
    val tts: LiveData<TextToSpeech> get() = _tts

    private val _selectedPhotoUri: MutableLiveData<Uri?> = MutableLiveData()
    val selectedPhotoUri: LiveData<Uri?> get() = _selectedPhotoUri

    private val _selectedPhotoBitmap: MutableLiveData<Bitmap?> = MutableLiveData()
    val selectedPhotoBitmap: LiveData<Bitmap?> get() = _selectedPhotoBitmap

    private val _recognisedText: MutableLiveData<String?> = MutableLiveData()
    val recognisedText: LiveData<String?> get() = _recognisedText

    private val _isTTSActive: MutableLiveData<Boolean> = MutableLiveData(false)
    val isTTSActive: LiveData<Boolean> get() = _isTTSActive

    private val _drawerFlow: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val drawerFlow: StateFlow<Boolean> = _drawerFlow

    private val _configuredStateFlow: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val configuredStateFlow: StateFlow<Boolean> = _configuredStateFlow

    private val _itemsMetaData: MutableStateFlow<Map<Int, State>> = MutableStateFlow(mapOf())
    private val itemsMetaData: StateFlow<Map<Int, State>> get() = _itemsMetaData

    private val _godFlow: MutableStateFlow<PagingData<ScanMapper>?> = MutableStateFlow(null)
    val godFlow: StateFlow<PagingData<ScanMapper>?> get() = _godFlow

    private var _selectMode: SelectMode = SelectMode.ItemClickMode
    private var _isDrawerOpened: Boolean = false
    private var currentFragment: CurrentFragment = CurrentFragment.Home
    private var isDrawerAnimationOver = false

    private val backStateChannel = Channel<BackState>()
    val backStateFlow = backStateChannel.receiveAsFlow()

    private val longClickChannel = Channel<Boolean>()
    val longClickFlow = longClickChannel.receiveAsFlow()

    private val _actionModeStateFlow: MutableStateFlow<ActionModeStatus> =
        MutableStateFlow(ActionModeStatus.Uninitialised)
    val actionModeStateFlow: StateFlow<ActionModeStatus> get() = _actionModeStateFlow

    private val _actionModeForceDestroyFlow: MutableStateFlow<Boolean> =
        MutableStateFlow(false)
    val actionModeForceDestroyFlow: StateFlow<Boolean> get() = _actionModeForceDestroyFlow

    private val _hasAnyItems: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val hasAnyItems: StateFlow<Boolean> get() = _hasAnyItems

    private val _backButtonFlow: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val backButtonFlow: StateFlow<Boolean> get() = _backButtonFlow

    private val _selectionLiveData: MutableLiveData<SelectMode> =
        MutableLiveData(SelectMode.ItemClickMode)
    val selectionLiveData: LiveData<SelectMode> get() = _selectionLiveData

    sealed class ActionModeStatus {
        object Uninitialised: ActionModeStatus()
        object Initialised: ActionModeStatus()
        object Created : ActionModeStatus()
        object Destroyed : ActionModeStatus()
    }

    sealed class SelectMode {
        object ItemSelectMode : SelectMode()
        object ItemClickMode : SelectMode()
    }

    sealed class BackState {
        object TtsStatus : BackState()
        object ItemSelect : BackState()
        object DrawerOpen : BackState()
        object None : BackState()
    }

    sealed class CurrentFragment {
        object Home : CurrentFragment()
        object OldScans : CurrentFragment()
        object Details : CurrentFragment()
        object TextToSpeech : CurrentFragment()
    }

    private fun hasNoDrawerAnimation(currentFrag: CurrentFragment): Boolean {

        if (currentFragment == CurrentFragment.Details &&
            currentFrag == CurrentFragment.OldScans
        ) {
            return true
        }

        return false
    }

    fun wasAlreadyActive(currentFrag: CurrentFragment): Boolean {

        if (currentFragment == currentFrag) {
            return true
        }

        currentFragment = currentFrag
        return false
    }

    val selectedItems = itemsMetaData
        .flatMapLatest { map ->

        flow {

            var count = 0
            val keys = map.keys

            for (key in keys) {

                if (map[key] == State.SELECTED)
                    count++
            }

            emit(count)
        }
    }

    fun deleteItems(context: Context) =
        viewModelScope.launch{

        Log.d("deleteItems ","hi")

        val selected = mutableListOf<Int>()

        val map = _itemsMetaData.value.toMutableMap()
        val keySet = map.keys

        for (key in keySet) {

            if (map[key] == State.SELECTED) {

                selected.add(key)
            }
        }

        withContext(Dispatchers.IO) {

            val dao = ScanDatabase.invoke(context).getScanDao()
            val selectedIndices: List<Int> = selected
            dao.deleteMultipleScans(selectedIndices)
        }

        _actionModeForceDestroyFlow.value = true
        initializeMap(context)
        resetSelected()
    }

    val shouldProceedFlow: Flow<Boolean> =
        combine(
            configuredStateFlow,
            drawerFlow
        ) { isConfigured, isDrawerAnimCompleted ->
            isConfigured || isDrawerAnimCompleted
        }.flatMapLatest {
            flow {
                emit(it)
            }
        }

    val shouldProceedToHomeFlow: Flow<Boolean> =
        combine(
            configuredStateFlow,
            drawerFlow,
            backButtonFlow
        ) { isConfigured, isDrawerAnimCompleted, wasBackButtonClicked ->
            isConfigured || isDrawerAnimCompleted || wasBackButtonClicked
        }.flatMapLatest {
            flow {
                emit(it)
            }
        }

    fun setBackPressFlow() =
        viewModelScope.launch{

            delay(200)
            _backButtonFlow.value = true
            delay(1000)
            _backButtonFlow.value = false
    }

    fun getScansSyncFlow(context: Context) {

        viewModelScope.launch {

            combine(
                getScansFlow(context),
                itemsMetaData
            ) { scan, stateMap ->
                scan.map {
                    it.toScanMapper(stateMap[it.id] ?: State.UNKNOWN)
                }
            }.collect {
                _godFlow.emit(it)
            }
        }
    }

    fun setActiveReceiveFlow(currentFrag: CurrentFragment, context: Context) =
        viewModelScope.launch {

            launch {

                if (currentFrag == CurrentFragment.OldScans) {

                    val wasActive =
                        hasNoDrawerAnimation(currentFrag) || wasAlreadyActive(currentFrag)
                    _configuredStateFlow.emit(wasActive)
                    _drawerFlow.emit(false)

                    if (!wasActive) {
                        initializeMap(context)
                    }

                } else {

                    _configuredStateFlow.emit(wasAlreadyActive(currentFrag))
                    _drawerFlow.emit(false)
                }
            }
        }

    fun getAllIds(context: Context) =
        viewModelScope.launch{

            val dao = ScanDatabase.invoke(context).getScanDao()

            withContext(Dispatchers.IO) {

                dao.getAllIds().flatMapLatest {

                    flow {
                        emit(it)
                    }
                }.collect {  list ->
                    _hasAnyItems.value = list.isNotEmpty()
                }
            }
    }

    private fun initializeMap(context: Context) =
        viewModelScope.launch {

            var idList: Flow<List<Int>> = flowOf(listOf())

            withContext(Dispatchers.IO) {

                val task = async {
                    return@async ScanDatabase.invoke(context).getScanDao().getAllIds()
                }

                idList = task.await().flatMapLatest {
                    flow {
                        emit(it)
                    }
                }
            }

            val map = _itemsMetaData.value.toMutableMap()
            map.clear()

            idList.collect { ids ->

                for (id in ids) {
                    map[id] = State.UNINITIALISED
                }

                _itemsMetaData.value = map
            }
        }

    fun drawerUpdater(isOpen: Boolean) =
        viewModelScope.launch {

            isDrawerAnimationOver = !isOpen
            _drawerFlow.emit(isDrawerAnimationOver)
        }

    fun setItemsState(state: State) =
        viewModelScope.launch {

            val map = _itemsMetaData.value.toMutableMap()
            val keys = map.keys

            for (key in keys) {

                map[key] = state
            }

            _itemsMetaData.value = map
        }

    fun updateScanIdList(scanId: Int) {

        val map = _itemsMetaData.value.toMutableMap()

        when (map[scanId]) {

            State.UNINITIALISED -> Unit

            State.INITIALISED -> {
                map[scanId] = State.UNSELECTED
            }

            State.UNSELECTED -> {
                map[scanId] = State.SELECTED
            }

            State.SELECTED -> {
                map[scanId] = State.UNSELECTED
            }

            State.UNKNOWN -> Unit
        }

        _itemsMetaData.value = map
    }

    fun setActionModeState(actionModeStatus: ActionModeStatus) {

        _actionModeStateFlow.value = actionModeStatus
    }

    fun getBackStatusFlow() = viewModelScope.launch {
        when {
            _isDrawerOpened -> {
                backStateChannel.send(BackState.DrawerOpen)
            }
            isTTSActive.value!! -> {
                backStateChannel.send(BackState.TtsStatus)
            }
            else -> {
                backStateChannel.send(BackState.None)
            }
        }
    }

    fun initTTS(context: Context) {
        _tts.value = TextToSpeech(context, TextToSpeech.OnInitListener {
            //check status
            if (it == TextToSpeech.SUCCESS) {
                val result = _tts.value?.setLanguage(Locale.ENGLISH)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED)
                    Log.e("TTS Fragment", "Language not supported")
                else {
                    // fab.isEnabled = true
                }
            } else {
                Log.e("TTS Fragment", "Initialization Failed")
            }

        })
    }

    private fun saveScan(context: Context) {
        if (!_recognisedText.value.isNullOrEmpty() && _selectedPhotoUri.value != null) {
            val scanDao = ScanDatabase.invoke(context).getScanDao()

            val name = System.currentTimeMillis()
            saveImageToStorage(context, name)
            val firstLine =
                if (_recognisedText.value!!.length > 10)
                    _recognisedText.value!!.substring(0, 10)
                else
                    "text wasn't recognized for this scan"

            viewModelScope.launch(Dispatchers.IO) {
                scanDao.insertScan(
                    Scan(
                        name = firstLine,
                        recognisedText = _recognisedText.value!!,
                        timeOfStorage = System.currentTimeMillis(),
                        filename = "${name}.jpg"
                    )
                )
            }
        }
    }

    private fun saveImageToStorage(context: Context, filename: Long) {
        val file = File(context.getExternalFilesDir(null), "${filename}.jpg")
        try {
            val istream: InputStream = bitmapToInputStream(_selectedPhotoBitmap.value!!)
            val os: OutputStream = FileOutputStream(file)
            val data = ByteArray(istream.available())
            istream.read(data)
            os.write(data)
            istream.close()
            os.close()
        } catch (e: IOException) {
            Log.d("saveImageToStorage ", "Error writing $file", e)
        }
    }

    private fun bitmapToInputStream(bmp: Bitmap): InputStream {
        val baos = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        return ByteArrayInputStream(baos.toByteArray())
    }

    fun toggleSelectMode() =
        viewModelScope.launch {

            if(_selectMode == SelectMode.ItemClickMode) {
                Log.d("select mode before","ItemClickMode")
            } else if(_selectMode == SelectMode.ItemSelectMode) {
                Log.d("select mode before","ItemSelectMode")
            }

            _selectMode = when (_selectMode) {
                is SelectMode.ItemClickMode -> {
                    SelectMode.ItemSelectMode
                }
                else -> {
                    SelectMode.ItemClickMode
                }
            }

            if(_selectMode == SelectMode.ItemClickMode) {
                Log.d("select mode after","ItemClickMode")
            } else if(_selectMode == SelectMode.ItemSelectMode) {
                Log.d("select mode after","ItemSelectMode")
            }

            _selectionLiveData.value = _selectMode
        }

    fun resetSelected() {
        _selectMode = SelectMode.ItemClickMode
        _selectionLiveData.value = _selectMode
        _actionModeStateFlow.value = ActionModeStatus.Uninitialised
    }

    fun getScansFlow(context: Context): Flow<PagingData<Scan>> {
        val scanDao = ScanDatabase.invoke(context).getScanDao()
        return Pager(
            config = PagingConfig(
                pageSize = 10,
                maxSize = 30,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { scanDao.getAllScans() }
        ).flow.cachedIn(viewModelScope)
    }

    fun toggleDrawerMode(isDrawerOpen: Boolean) =
        viewModelScope.launch {

            _isDrawerOpened = isDrawerOpen
        }

    fun setActionModeDestroyFlow(value: Boolean) {

        _actionModeForceDestroyFlow.value = value
    }

    fun setUri(uri: Uri?) {
        _selectedPhotoUri.value = uri
    }

    fun getRecognisedText(
        contentResolver: ContentResolver,
        context: Context
    ) {
        val bmp: Bitmap = MediaStore.Images.Media.getBitmap(
            contentResolver,
            _selectedPhotoUri.value
        )
        _selectedPhotoBitmap.value = bmp
        val image = InputImage.fromBitmap(bmp, 0)
        val recognizer = TextRecognition.getClient()

        recognizer.process(image).addOnSuccessListener {
            if (it.text.isEmpty())
                _recognisedText.value = "Text couldn't be recognised"
            else
                _recognisedText.value = it.text

            saveScan(context)
        }
            .addOnFailureListener {
                Toast.makeText(context, it.message, Toast.LENGTH_LONG).show()
            }
    }

    fun speak(lifecycleOwner: LifecycleOwner) =
        viewModelScope.launch{

        recognisedText.observe(lifecycleOwner, Observer {  text ->

            text?.let { recognisedText ->

                speakTTS(recognisedText)
            }
        })
    }

    fun speakTTS(recognisedText: String) {
        val text = recognisedText.toString()
        val pitch: Float = 1f
        val speed: Float = 1f

        _tts.value?.setPitch(pitch)
        _tts.value?.setSpeechRate(speed)
        _tts.value?.speak(text, TextToSpeech.QUEUE_FLUSH, null)
    }

    fun reset() {
        _selectedPhotoUri.value = null
        _selectedPhotoBitmap.value = null
        _recognisedText.value = null
    }

    fun stopTTS() {
        if (_tts.value != null)
            _tts.value?.stop()
    }

    fun shutdownTTS() {
        if (_tts.value != null)
            _tts.value?.shutdown()
    }

    fun sendOnLongClickFlow(context: Context, pos: Int) =
        viewModelScope.launch {

            _actionModeStateFlow.value = ActionModeStatus.Initialised

            val map = _itemsMetaData.value.toMutableMap()
            val keySet = map.keys

            for (key in keySet) {

                map[key] = State.UNSELECTED
            }

            _itemsMetaData.value = map
        }
}