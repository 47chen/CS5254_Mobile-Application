package edu.vt.cs.cs5254.dreamcatcher

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.text.format.DateFormat
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import edu.vt.cs.cs5254.dreamcatcher.databinding.FragmentDreamListBinding
import edu.vt.cs.cs5254.dreamcatcher.databinding.ListItemDreamBinding
import java.util.*

private const val TAG = "DreamListFragment"
class DreamListFragment : Fragment() {

    //whenever we have lifecycle methods, we like to put in order (action1->action2->action3 )

    interface Callbacks {
        fun onDreamSelected(dreamId: UUID)
    }

    private var callbacks: Callbacks? = null
    private var adapter: DreamAdapter? = null
    private var _binding: FragmentDreamListBinding? = null
    private val binding get() = _binding!!
    private val vm: DreamListViewModel by viewModels()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = context as Callbacks?
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        _binding = FragmentDreamListBinding.inflate(inflater, container, false)
        val view = binding.root
        binding.dreamRecyclerView.layoutManager = LinearLayoutManager(context)
        //Grid layout manager & linear layout manager
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vm.dreamListLiveData.observe(
            viewLifecycleOwner,
            Observer { dreams ->
                dreams?.let {
                    Log.i(TAG, "Got dreams ${dreams.size}")
                    updateUI(dreams)
                }
            }
        )
    }

    override fun onDetach() {
        super.onDetach()
        callbacks = null
    }

    // option menu callbacks
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_dream_list, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.add_dream -> {
                val dreamWithEntries = DreamWithEntries(Dream(), emptyList())
                vm.addDream(dreamWithEntries)
                callbacks?.onDreamSelected(dreamWithEntries.dream.id)
                true
            }
            R.id.delete_all_dreams -> {
                vm.deleteAllDream()
                true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }


    private fun updateUI(dreams: List<Dream>) {
        adapter = DreamAdapter(dreams)
        binding.dreamRecyclerView.adapter = adapter
    }

    // DreamHolder && DreamAdapter
    // holder => hold a list of dream items

    public inner class DreamHolder(val itemBinding: ListItemDreamBinding)
        : RecyclerView.ViewHolder(itemBinding.root), View.OnClickListener {
        private lateinit var dream: Dream

        init {
            itemView.setOnClickListener(this)
        }

        fun bind(dream: Dream) {
            this.dream = dream
            itemBinding.dreamItemTitle.text = this.dream.title
            val time = DateFormat.format("MMM dd, yyyy", this.dream.date)
            itemBinding.dreamItemDate.text = time.toString()
            when {
                dream.isDeferred -> {
                    itemBinding.dreamItemImage.setImageResource(R.drawable.dream_deferred_icon)
                    itemBinding.dreamItemImage.tag = R.drawable.dream_deferred_icon
                }
                dream.isFulfilled -> {
                    itemBinding.dreamItemImage.setImageResource(R.drawable.dream_fulfilled_icon)
                    itemBinding.dreamItemImage.tag = R.drawable.dream_fulfilled_icon
                }
                else -> {
                    itemBinding.dreamItemImage.setImageResource(0)
                    itemBinding.dreamItemImage.tag = 0
                }
            }
        }
        override fun onClick(v: View) {
            callbacks?.onDreamSelected(dream.id)
            // crime.id vs dreamId
        }
    }

    private inner class DreamAdapter(var dreams: List<Dream>)
        : RecyclerView.Adapter<DreamHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DreamHolder {
            val itemBinding = ListItemDreamBinding
                .inflate(LayoutInflater.from(parent.context), parent, false)
            return DreamHolder(itemBinding)
        }
        override fun getItemCount() = dreams.size
        override fun onBindViewHolder(holder: DreamHolder, position: Int) {
            val dream = dreams[position]
            holder.bind(dream)
        }
    }

    companion object {
        fun newInstance(): DreamListFragment {
            return DreamListFragment()
        }
    }

}