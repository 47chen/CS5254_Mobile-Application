package edu.vt.cs.cs5254.dreamcatcher

import android.content.res.ColorStateList
import android.graphics.Color
import android.opengl.Visibility
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateFormat
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.view.children
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.viewModels
import edu.vt.cs.cs5254.dreamcatcher.databinding.FragmentDreamDetailBinding
import androidx.lifecycle.Observer
import java.util.*


private const val TAG = "DreamDetailFragment"
private const val ARG_DREAM_ID = "dream_id"
private const val CONCEIVED_BUTTON_COLOR = "#a0cace"
private const val DEFERRED_BUTTON_COLOR = "#f4dec7"
private const val FULFILLED_BUTTON_COLOR = "#a4bc51"
private const val REFLECTION_BUTTON_COLOR = "#c9aad9"

class DreamDetailFragment : Fragment() {

    private lateinit var dreamWithEntries: DreamWithEntries

    private var _binding: FragmentDreamDetailBinding? = null
    // when fragment is not attached to the activity, it should be set to null
    private val binding: FragmentDreamDetailBinding
        get() = _binding!!
    // !! make sure this cant not be null => don't want this binding is null but _binding is null

//    private val viewModel: DreamDetailViewModel by lazy {
//        ViewModelProvider(this).get(DreamDetailViewModel::class.java)
//    }
    // work same as above
    private val viewModel: DreamDetailViewModel by viewModels()

//    private var entBtn = viewModel.dreamWithEntries.dreamEntries
    private lateinit var entryButtonList: List<Button>



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dreamWithEntries = DreamWithEntries(Dream(), emptyList())
        val dreamId: UUID = arguments?.getSerializable(ARG_DREAM_ID) as UUID
        viewModel.loadDream(dreamId)
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentDreamDetailBinding.inflate(inflater, container, false)
        val view = binding.root

        updateUI()
//        binding.dreamTitleText.setText(viewModel.dreamWithEntries.dream.title)

//        binding.dreamEntry0Button.apply {
//            text = dreamWithEntries.dream.date.toString()
//            isEnabled = true
//        }


        if (dreamWithEntries.dream.isFulfilled) {
            binding.dreamFulfilledCheckbox.isChecked = dreamWithEntries.dream.isFulfilled
            binding.dreamDeferredCheckbox.isEnabled = false
        }

        if (dreamWithEntries.dream.isDeferred) {
            binding.dreamDeferredCheckbox.isChecked = dreamWithEntries.dream.isDeferred
            binding.dreamFulfilledCheckbox.isEnabled = false
        }

        entryButtonList = binding.root
            .children
            .toList()
            .filterIsInstance<Button>()


        val entryButtonListPair = entryButtonList.zip(dreamWithEntries.dreamEntries)
        entryButtonListPair.forEach {
                (btn, ent) ->
            btn.visibility = View.VISIBLE

            when(ent.kind) {
                DreamEntryKind.CONCEIVED -> {
                    btn.text = "CONCEIVED"
                    setButtonColor(btn, CONCEIVED_BUTTON_COLOR)
                    btn.setTextColor(Color.BLACK)
                } DreamEntryKind.DEFERRED -> {
                btn.text = "DEFERRED"
                setButtonColor(btn, DEFERRED_BUTTON_COLOR)
                btn.setTextColor(Color.BLACK)
            } DreamEntryKind.FULFILLED -> {
                btn.text = "FULFILLED"
                setButtonColor(btn, FULFILLED_BUTTON_COLOR)
                btn.setTextColor(Color.BLACK)
            } DreamEntryKind.REFLECTION -> {
                val time = DateFormat.format("MMM dd, yyyy", ent.date)
                btn.text = time.toString() + ": " + ent.text
                setButtonColor(btn, REFLECTION_BUTTON_COLOR)
                btn.setTextColor(Color.BLACK)
            }
            }
        }


        return view
    }

    private fun updateUI() {
        binding.dreamTitleText.setText(dreamWithEntries.dream.title)
        binding.dreamFulfilledCheckbox.isChecked = dreamWithEntries.dream.isFulfilled
        binding.dreamDeferredCheckbox.isChecked = dreamWithEntries.dream.isDeferred
        binding.dreamEntry0Button.apply {
            text = dreamWithEntries.dream.date.toString()
            isEnabled = true
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.dreamLiveData.observe(
            viewLifecycleOwner,
            Observer { dreamWithEntries ->
                dreamWithEntries?.let {
                    this.dreamWithEntries = dreamWithEntries
                    updateUI()
                }
            })
    }


    // want listener only when user interactive
    // only want to trigger the listener, don't want to trigger when rotate

    override fun onStart() {
        super.onStart()
//        val titleWatcher = object : TextWatcher {
//            override fun beforeTextChanged(
//                sequence: CharSequence?, start: Int, count: Int, after: Int) { }
//            override fun onTextChanged(sequence: CharSequence?,
//                                       start: Int, before: Int, count: Int) {
//                dreamWithEntries.dream.title = sequence.toString()
//            }
//            override fun afterTextChanged(sequence: Editable?) { }
//        }
//        binding.dreamTitleText.addTextChangedListener(titleWatcher)
        binding.dreamTitleText.doOnTextChanged{text, start, before, count ->
            dreamWithEntries.dream.title = text.toString()
        }

        // When the fulfilled checkbox is check, the deferred checkbox should be
        // disable, and vice-versa

        binding.dreamFulfilledCheckbox.apply {
            setOnCheckedChangeListener { _, isChecked ->
                dreamWithEntries.dream.isFulfilled = isChecked
                binding.dreamDeferredCheckbox.apply {
                    isEnabled = false
                }
            }
        }
        binding.dreamDeferredCheckbox.apply {
            setOnCheckedChangeListener { _, isChecked ->
                dreamWithEntries.dream.isDeferred = isChecked
                binding.dreamFulfilledCheckbox.apply {
                    isEnabled = false
                }
            }
        }

        binding.dreamFulfilledCheckbox.apply {
            setOnCheckedChangeListener { _, isChecked ->
                dreamWithEntries.dream.isFulfilled = isChecked

                if(isChecked){
                    val dreamId = dreamWithEntries.dream.id
                    dreamWithEntries.dreamEntries += DreamEntry(kind = DreamEntryKind.FULFILLED,dreamId = dreamId)
                }else{
                    val newEntry = dreamWithEntries.dreamEntries.toMutableList()
                    newEntry.removeLast()
                    dreamWithEntries.dreamEntries = newEntry
                }

                refreshView()
            }  //viewModel.crime.isSolved = isChecked }
        }

        binding.dreamDeferredCheckbox.apply {
            setOnCheckedChangeListener { _, isChecked ->
                dreamWithEntries.dream.isDeferred = isChecked
                if(isChecked){
                    val dreamId = dreamWithEntries.dream.id
                    dreamWithEntries.dreamEntries += DreamEntry(kind = DreamEntryKind.DEFERRED,dreamId = dreamId)
                }else{
                    val newEntry = dreamWithEntries.dreamEntries.toMutableList()
                    newEntry.removeLast()
                    dreamWithEntries.dreamEntries = newEntry
                }
                refreshView()
            }
        }
    //TODO: setOnClickListener / setFragmentResultListener
        // need button and REQUEST_KEY
        // Drk use data in criminal, we need DreamEntry button
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

    private fun refreshView() {

        when {
            dreamWithEntries.dream.isFulfilled -> {
                binding.dreamDeferredCheckbox.isEnabled = false
            }
            dreamWithEntries.dream.isDeferred -> {
                binding.dreamFulfilledCheckbox.isEnabled = false
            }
            else -> {
                binding.dreamFulfilledCheckbox.isEnabled = true
                binding.dreamDeferredCheckbox.isEnabled = true
            }
        }

        entryButtonList.forEach { it.visibility = View.INVISIBLE }

        entryButtonList.zip(dreamWithEntries.dreamEntries) {
                btn, ent -> btn.visibility = View.VISIBLE
            when (ent.kind) {
                DreamEntryKind.CONCEIVED -> {
                    btn.text = "CONCEIVED"
                    setButtonColor(btn, CONCEIVED_BUTTON_COLOR)
                    btn.setTextColor(Color.BLACK)
                }
                DreamEntryKind.REFLECTION -> {
                    val time = DateFormat.format("MMM dd, yyyy", ent.date)
                    btn.text = time.toString() + ": " + ent.text
                    setButtonColor(btn, REFLECTION_BUTTON_COLOR)
                    btn.setTextColor(Color.BLACK)
                }
                DreamEntryKind.FULFILLED -> {
                    btn.text = "FULFILLED"
                    setButtonColor(btn, FULFILLED_BUTTON_COLOR)
                    btn.setTextColor(Color.BLACK)
                }
                DreamEntryKind.DEFERRED -> {
                    btn.text = "DEFERRED"
                    setButtonColor(btn, DEFERRED_BUTTON_COLOR)
                    btn.setTextColor(Color.BLACK)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onStop() {
        super.onStop()
        viewModel.saveDream(dreamWithEntries)
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

    private fun setButtonColor(button: Button, colorString: String) {
        button.backgroundTintList =
            ColorStateList.valueOf(Color.parseColor(colorString))
        button.setTextColor(Color.WHITE)
        button.alpha = 1f
    }

}
