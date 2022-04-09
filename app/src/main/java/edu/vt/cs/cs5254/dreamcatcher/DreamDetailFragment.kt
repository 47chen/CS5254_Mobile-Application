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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import edu.vt.cs.cs5254.dreamcatcher.databinding.ListItemDreamEntryBinding
import java.util.*


private const val TAG = "DreamDetailFragment"
private const val ARG_DREAM_ID = "dream_id"
private const val CONCEIVED_BUTTON_COLOR = "#a0cace"
private const val DEFERRED_BUTTON_COLOR = "#f4dec7"
private const val FULFILLED_BUTTON_COLOR = "#a4bc51"
private const val REFLECTION_BUTTON_COLOR = "#c9aad9"

class DreamDetailFragment : Fragment() {

    private lateinit var dreamWithEntries: DreamWithEntries
    private lateinit var entryButtonList: List<Button>
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
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentDreamDetailBinding.inflate(inflater, container, false)
        val view = binding.root
        binding.dreamEntryRecyclerView.layoutManager = LinearLayoutManager(context)

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
        adapter = DreamEntryAdapter(dreamWithEntries.dreamEntries)
        binding.dreamEntryRecyclerView.adapter = adapter
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
