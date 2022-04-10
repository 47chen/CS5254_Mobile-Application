package edu.vt.cs.cs5254.dreamcatcher

import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.format.DateFormat
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.Button
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.viewModels
import edu.vt.cs.cs5254.dreamcatcher.databinding.FragmentDreamDetailBinding
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import edu.vt.cs.cs5254.dreamcatcher.databinding.ListItemDreamEntryBinding
import java.io.File
import java.util.*


private const val TAG = "DreamDetailFragment"
private const val ARG_DREAM_ID = "dream_id"
private const val CONCEIVED_BUTTON_COLOR = "#a0cace"
private const val DEFERRED_BUTTON_COLOR = "#f4dec7"
private const val FULFILLED_BUTTON_COLOR = "#a4bc51"
private const val REFLECTION_BUTTON_COLOR = "#c9aad9"

class DreamDetailFragment : Fragment() {

    private lateinit var dreamWithEntries: DreamWithEntries
    private lateinit var photoFile: File
    private lateinit var photoUri: Uri
    private lateinit var photoLauncher: ActivityResultLauncher<Uri>
//    private lateinit var entryButtonList: List<Button>
    private var _binding: FragmentDreamDetailBinding? = null
    // when fragment is not attached to the activity, it should be set to null
    private val binding: FragmentDreamDetailBinding
        get() = _binding!!
    private val viewModel: DreamDetailViewModel by viewModels()

    private var adapter: DreamEntryAdapter? = null





    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dreamWithEntries = DreamWithEntries(Dream(), emptyList())
        val dreamId: UUID = arguments?.getSerializable(ARG_DREAM_ID) as UUID
        viewModel.loadDream(dreamId)
        setHasOptionsMenu(true)
        photoLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()){
            if(it){
                updatePhotoView()
            }
            requireActivity().revokeUriPermission(photoUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentDreamDetailBinding.inflate(inflater, container, false)
        val view = binding.root
        binding.dreamEntryRecyclerView.layoutManager = LinearLayoutManager(context)

        // itemTouchHelper CallBack For RecyclerView
        val swipeToDeleteCallback = object : SwipeToDeleteCallback() {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val pos = viewHolder.absoluteAdapterPosition
                val swipedDreamEntry = dreamWithEntries.dreamEntries[pos]
                if (swipedDreamEntry.kind == DreamEntryKind.REFLECTION) {
                    dreamWithEntries.dreamEntries -= swipedDreamEntry
                }
                updateUI()
            }
        }

        // attach ItemTouchHelper to RecyclerView
        val itemTouchHelper = ItemTouchHelper(swipeToDeleteCallback)
        itemTouchHelper.attachToRecyclerView(binding.dreamEntryRecyclerView)


        return view
    }

    // option menu callbacks
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_dream_detail, menu)
        val cameraAvailable = PictureUtils.isCameraAvailable(requireActivity())
        val menuItem = menu.findItem(R.id.take_dream_photo)
        menuItem.apply {
            Log.d(TAG, "Camera available: $cameraAvailable")
            isEnabled = cameraAvailable
            isVisible = cameraAvailable
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.share_dream -> {
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, getDreamReport())
                    putExtra(
                        Intent.EXTRA_SUBJECT,
                        getString(R.string.dream_report_subject)
                    )
                }.also { intent ->
                    val chooserIntent =
                        Intent.createChooser(intent, getString(R.string.share_report))
                    startActivity(chooserIntent)
                }
                true
            }
            R.id.take_dream_photo -> {
                val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                    putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                }
                requireActivity().packageManager
                    .queryIntentActivities(captureIntent, PackageManager.MATCH_DEFAULT_ONLY)
                    .forEach { cameraActivity ->
                        requireActivity().grantUriPermission(
                            cameraActivity.activityInfo.packageName,
                            photoUri,
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                    }
                photoLauncher.launch(photoUri)
                true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun getDreamReport(): String {
        val fulfilledDeferredString = if (dreamWithEntries.dream.isFulfilled) {
            getString(R.string.dream_report_fulfilled)
        } else if (dreamWithEntries.dream.isDeferred) {
            getString(R.string.dream_report_deferred)
        } else {
            ""
        }

        val df = DateFormat.getMediumDateFormat(context)
        val dateString = df.format(dreamWithEntries.dream.date)
        val dreamReflections =
            dreamWithEntries.dreamEntries.filter { dreamEntry -> dreamEntry.kind == DreamEntryKind.REFLECTION }
        var dreamReflectionsString = if (dreamReflections.isNotEmpty()) {
            "Dream reflections:\n"
        } else {
            getString(R.string.dream_report_no_reflection)
        }

        dreamReflections.forEach { dreamEntry ->
            dreamReflectionsString += "-" + dreamEntry.text + "\n"
        }

        return getString(
            R.string.dream_report,
            dreamWithEntries.dream.title,
            dateString,
            dreamReflectionsString,
            fulfilledDeferredString
        )
    }

    private fun updateUI() {
        binding.dreamTitleText.setText(dreamWithEntries.dream.title)
        binding.dreamFulfilledCheckbox.isChecked = dreamWithEntries.dream.isFulfilled
        binding.dreamDeferredCheckbox.isChecked = dreamWithEntries.dream.isDeferred
        adapter = DreamEntryAdapter(dreamWithEntries.dreamEntries)
        binding.dreamEntryRecyclerView.adapter = adapter
    }

    private fun updatePhotoView() {
        if (photoFile.exists()) {
            val bitmap = PictureUtils.getScaledBitmap(photoFile.path, 120, 120)
            binding.dreamPhoto.setImageBitmap(bitmap)
        } else {
            binding.dreamPhoto.setImageDrawable(null)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.dreamLiveData.observe(
            viewLifecycleOwner,
            Observer { dreamWithEntries ->
                dreamWithEntries?.let {
                    this.dreamWithEntries = dreamWithEntries
                    photoFile = viewModel.getPhotoFile(dreamWithEntries.dream)
                    photoUri = FileProvider.getUriForFile(
                        requireActivity(),
                    "edu.vt.cs.cs5254.dreamcatcher.fileprovider",
                        photoFile
                    )
                    updateUI()
                }
            })
    }


    // want listener only when user interactive
    // only want to trigger the listener, don't want to trigger when rotate

    override fun onStart() {
        super.onStart()
        binding.dreamTitleText.doOnTextChanged { text, start, before, count ->
            dreamWithEntries.dream.title = text.toString()
        }

        binding.dreamFulfilledCheckbox.apply {
            setOnClickListener {
                dreamWithEntries.dream.isFulfilled = isChecked
                if (isChecked) {
                    binding.dreamDeferredCheckbox.isEnabled = false
                    binding.addReflectionButton.isEnabled = false
                    if (!dreamWithEntries.dreamEntries.any { dreamEntry -> dreamEntry.kind == DreamEntryKind.FULFILLED }) {
                        dreamWithEntries.dreamEntries += DreamEntry(
                            kind = DreamEntryKind.FULFILLED,
                            dreamId = dreamWithEntries.dream.id
                        )
                    }
                } else {
                    binding.dreamDeferredCheckbox.isEnabled = true
                    binding.addReflectionButton.isEnabled = true
                    dreamWithEntries.dreamEntries =
                        dreamWithEntries.dreamEntries.filter { dreamEntry ->
                            dreamEntry.kind != DreamEntryKind.FULFILLED
                        }
                }

                updateUI()
            }
        }

        binding.dreamDeferredCheckbox.apply {
            setOnClickListener {
                dreamWithEntries.dream.isDeferred = isChecked
                if (isChecked) {
                    binding.dreamFulfilledCheckbox.isEnabled = false
                    if (!dreamWithEntries.dreamEntries.any { dreamEntry -> dreamEntry.kind == DreamEntryKind.DEFERRED }) {
                        dreamWithEntries.dreamEntries += DreamEntry(
                            kind = DreamEntryKind.DEFERRED,
                            dreamId = dreamWithEntries.dream.id
                        )
                    }
                } else {
                    binding.dreamFulfilledCheckbox.isEnabled = true
                    dreamWithEntries.dreamEntries =
                        dreamWithEntries.dreamEntries.filter { dreamEntry ->
                            dreamEntry.kind != DreamEntryKind.DEFERRED
                        }
                }
                updateUI()
            }
        }

    binding.addReflectionButton.setOnClickListener{
        AddReflectionDialog.newInstance(REQUEST_KEY_ADD_REFLECTION)
            .show(parentFragmentManager, REQUEST_KEY_ADD_REFLECTION)
    }

    parentFragmentManager.setFragmentResultListener(
        REQUEST_KEY_ADD_REFLECTION,
        viewLifecycleOwner)
        {_, bundle ->
            val reflectionText = bundle.getSerializable(BUNDLE_KEY_REFLECTION_TEXT) as String
            val newReflection = DreamEntry(
                kind = DreamEntryKind.REFLECTION,
                dreamId = dreamWithEntries.dream.id,
                text = reflectionText
            )
            dreamWithEntries.dreamEntries += newReflection
            updateUI()
        }


    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    private fun refreshDreamEntry(dreamEntry: DreamEntry, button: Button) {
        button.visibility = View.VISIBLE
        when (dreamEntry.kind) {
            DreamEntryKind.CONCEIVED -> {
                button.text = DreamEntryKind.CONCEIVED.name
                button.backgroundTintList =
                    ColorStateList.valueOf(Color.parseColor(CONCEIVED_BUTTON_COLOR))
            }
            DreamEntryKind.FULFILLED -> {
                button.text = DreamEntryKind.FULFILLED.name
                button.backgroundTintList =
                    ColorStateList.valueOf(Color.parseColor(FULFILLED_BUTTON_COLOR))
                button.setTextColor(Color.WHITE)
            }
            DreamEntryKind.DEFERRED -> {
                button.text = DreamEntryKind.DEFERRED.name
                button.backgroundTintList =
                    ColorStateList.valueOf(Color.parseColor(DEFERRED_BUTTON_COLOR))
                button.setTextColor(Color.WHITE)
            }
            else -> {
                val buttonText = DateFormat.format("MMM dd, yyyy", dreamEntry.date).toString() +
                        ": " + dreamEntry.text
                button.text = buttonText
                button.backgroundTintList =
                    ColorStateList.valueOf(Color.parseColor(REFLECTION_BUTTON_COLOR))
                button.setTextColor(Color.BLACK)
            }
        }
    }

    inner class DreamEntryHolder(val itemBinding: ListItemDreamEntryBinding) :
        RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(dreamEntry: DreamEntry) {
            refreshDreamEntry(dreamEntry, itemBinding.dreamEntryButton)
        }
    }

    private inner class DreamEntryAdapter(var dreamEntries: List<DreamEntry>) :
        RecyclerView.Adapter<DreamEntryHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DreamEntryHolder {
            val itemBinding = ListItemDreamEntryBinding
                .inflate(LayoutInflater.from(parent.context), parent, false)
            return DreamEntryHolder(itemBinding)
        }

        override fun getItemCount() = dreamEntries.size
        override fun onBindViewHolder(holder: DreamEntryHolder, position: Int) {
            val dreamEntry = dreamEntries[position]
            holder.bind(dreamEntry)
        }
    }


    override fun onStop() {
        super.onStop()
        viewModel.saveDream(dreamWithEntries)
    }

    // SwipeToDelete
    abstract class SwipeToDeleteCallback : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            return false
        }
    }

    companion object {
        fun newInstance(dreamId: UUID): DreamDetailFragment {
            val args = Bundle().apply {
                putSerializable(ARG_DREAM_ID, dreamId)
            }
            return DreamDetailFragment().apply {
                arguments = args
            }
        }
    }

}
