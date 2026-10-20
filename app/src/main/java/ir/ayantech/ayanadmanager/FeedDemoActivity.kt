package ir.ayantech.ayanadmanager

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ir.ayantech.ayanadmanager.core.AdBinding
import ir.ayantech.ayanadmanager.core.AdSlot
import ir.ayantech.ayanadmanager.core.AdSlotState
import ir.ayantech.ayanadmanager.core.AyanAdManager
import ir.ayantech.ayanadmanager.core.ScreenAdSession
import ir.ayantech.ayanadmanager.sample.R
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.launch

class FeedDemoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(FrameLayout(this).apply { id = R.id.feed_host })
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction().replace(R.id.feed_host, AdsFeedFragment().apply {
                arguments = Bundle().apply { putString("containerKey", intent.getStringExtra("containerKey")) }
            }).commit()
        }
    }
}

/** Real fragment/view-holder integration. Use a configured test placement when profiling. */
class AdsFeedFragment : Fragment() {
    private var recycler: RecyclerView? = null
    private var ads: ScreenAdSession? = null
    private val adPositions = (9 until 100 step 10).toList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View =
        RecyclerView(requireContext()).also { recycler = it }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (!AyanAdManager.isInitialized()) {
            // The demo Activity is normally opened after initialization. Process restoration
            // should return to the launcher, which owns SDK initialization.
            requireActivity().finish()
            return
        }
        val session = AyanAdManager.createScreenSession(
            requireActivity() as AppCompatActivity, viewLifecycleOwner,
            attemptTimeout = 15.seconds,
        ).also { ads = it }
        val list = requireNotNull(recycler)
        val layout = LinearLayoutManager(requireContext())
        list.layoutManager = layout
        val key = requireArguments().getString("containerKey").orEmpty()
        val adapter = FeedAdapter(session)
        list.adapter = adapter

        fun prepareVisibleSlots() {
            val first = layout.findFirstVisibleItemPosition().coerceAtLeast(0)
            val last = layout.findLastVisibleItemPosition().coerceAtLeast(first + 10)
            val nearby = adPositions.filter { it >= first && it <= last + 10 }.take(4)
            session.prepare(nearby.map { AdSlot("ad-row-$it", key) })
        }
        prepareVisibleSlots()
        list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) = prepareVisibleSlots()
        })
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                var previous = emptyMap<String, AdSlotState>()
                session.states.collect { states ->
                    val changed = adPositions.filter { previous["ad-row-$it"] != states["ad-row-$it"] }
                    // A viewport update can emit while RecyclerView is computing layout.
                    list.post {
                        if (recycler === list && list.adapter === adapter) {
                            changed.forEach { adapter.notifyItemChanged(it, "ad-state") }
                        }
                    }
                    previous = states
                }
            }
        }
    }

    override fun onDestroyView() {
        recycler?.clearOnScrollListeners()
        recycler?.adapter = null
        ads?.close()
        ads = null
        recycler = null
        super.onDestroyView()
    }
}

private class FeedAdapter(private val session: ScreenAdSession) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    init { setHasStableIds(true) }
    override fun getItemCount() = 100
    override fun getItemId(position: Int) = position.toLong() // Fixed demo data; real feeds use model IDs.
    override fun getItemViewType(position: Int) = if (position % 10 == 9) 1 else 0

    private class AdHolder(val container: FrameLayout) : RecyclerView.ViewHolder(container) {
        var binding: AdBinding? = null
        var slotId: String? = null
    }
    private class ContentHolder(val text: TextView) : RecyclerView.ViewHolder(text)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val height = (80 * parent.resources.displayMetrics.density).toInt()
        return if (viewType == 1) {
            AdHolder(FrameLayout(parent.context).apply {
                layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height)
            })
        } else {
            ContentHolder(TextView(parent.context).apply {
                layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height)
                gravity = Gravity.CENTER_VERTICAL
                setPadding(24, 0, 24, 0)
            })
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ContentHolder) {
            holder.text.text = "Feed item ${position + 1}"
        } else if (holder is AdHolder) {
            val id = "ad-row-$position"
            if (holder.slotId != id || id !in session.states.value) {
                holder.binding?.dispose()
                holder.binding = null
            }
            holder.slotId = id
            if (id in session.states.value) holder.binding = session.bind(id, holder.container)
        }
    }

    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        (holder as? AdHolder)?.binding?.attach()
    }
    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        (holder as? AdHolder)?.binding?.detach()
    }
    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        (holder as? AdHolder)?.apply {
            binding?.dispose()
            binding = null
            slotId = null
        }
    }
}
