package ru.frozik6k.finabox

import android.content.Context
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.support.v7.view.ActionMode
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.*
import android.widget.TextView
import com.badoo.mobile.util.WeakHandler
import ru.frozik6k.lohouse.thing.Thing
import ru.frozik6k.lohouse.thing.ThingContent
import java.util.*

class ThingsFragment : Fragment() {

    companion object {
        private const val LOG_TAG = "ThingsFragment"
        private const val ARG_COLUMN_COUNT = "column-count"
        private const val SAVE_ACTIONMODE_ID_THINGS = "action_id_things"
        private const val SAVE_ACTIONMODE_PASTE = "action_paste"
        private const val SAVE_ACTIONMODE_COPY = "action_copy"

        @JvmStatic
        fun newInstance(columnCount: Int): ThingsFragment {
            val fragment = ThingsFragment()
            val args = Bundle()
            args.putInt(ARG_COLUMN_COUNT, columnCount)
            fragment.arguments = args
            return fragment
        }
    }

    private var mColumnCount = 1
    private var mListener: OnListFragmentInteractionListener? = null
    private var adapter: ThingsRecyclerViewAdapter? = null
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvPath: TextView
    private lateinit var mContext: Context

    // множество отмеченных пунктов
    private val actionModeSet: MutableSet<Long> = HashSet()

    private var actionModePaste = false    // true - режим вставки вещи
    private var actionModeCopy = false     // false - перемещение, true - копирование
    private var mActionMode: ActionMode? = null

    private var mHandler: WeakHandler? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { mColumnCount = it.getInt(ARG_COLUMN_COUNT) }
        Log.d(LOG_TAG, "onCreate $this")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        if (savedInstanceState != null) {
            actionModeSet.clear()
            savedInstanceState.getLongArray(SAVE_ACTIONMODE_ID_THINGS)?.forEach { actionModeSet.add(it) }
            actionModePaste = savedInstanceState.getBoolean(SAVE_ACTIONMODE_PASTE, false)
            actionModeCopy = savedInstanceState.getBoolean(SAVE_ACTIONMODE_COPY, false)
        }

        val view = inflater.inflate(R.layout.fragment_things_list, container, false)

        // Set the adapter
        val context = view.context
        recyclerView = view.findViewById(R.id.list)
        tvPath = view.findViewById(R.id.path)
        recyclerView.itemAnimator = DefaultItemAnimator()
        val itemDecoration = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        recyclerView.addItemDecoration(itemDecoration)
        recyclerView.layoutManager =
            if (mColumnCount <= 1) LinearLayoutManager(context) else GridLayoutManager(context, mColumnCount)

        updateUI()
        Log.d(LOG_TAG, "onCreateView $this")
        return view
    }

    private fun updateUI() {
        Log.d(LOG_TAG, "updateUI $this")
        Log.d(LOG_TAG, "parent = ${ThingContent.get(mContext).parent}")
        if (adapter == null) {
            adapter = ThingsRecyclerViewAdapter(ThingContent.get(mContext).things, mListener)
            recyclerView.adapter = adapter
        } else {
            adapter?.notifyDataSetChanged()
        }

        if (actionModeSet.isNotEmpty() && mActionMode == null) {
            mActionMode = (mContext as AppCompatActivity).startSupportActionMode(callback)
            mActionMode?.title = actionModeSet.size.toString()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnListFragmentInteractionListener) {
            mListener = context
        } else {
            throw RuntimeException("$context must implement OnListFragmentInteractionListener")
        }
        mContext = context
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        val array = LongArray(actionModeSet.size)
        var i = 0
        for (id in actionModeSet) array[i++] = id
        outState.putLongArray(SAVE_ACTIONMODE_ID_THINGS, array)
        outState.putBoolean(SAVE_ACTIONMODE_PASTE, actionModePaste)
        outState.putBoolean(SAVE_ACTIONMODE_COPY, actionModeCopy)
        super.onSaveInstanceState(outState)
    }

    private val callback = object : ActionMode.Callback {

        private var menuRef: Menu? = null

        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            mode.menuInflater.inflate(R.menu.action_mode_things_fragment, menu)
            menu.setGroupVisible(R.id.group_actionmode_main, true)
            menu.setGroupVisible(R.id.group_actionmode_copy, false)
            menuRef = menu
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean = false

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            val thingContent = ThingContent.get(mContext)

            when (item.itemId) {
                R.id.action_edit -> {
                    if (actionModeSet.size == 1) {
                        val id = actionModeSet.iterator().next()
                        thingContent.setThingEdit(id)
                        mActionMode?.finish()
                        startActivity(ThingActivity.newIntent(mContext, true))
                    }
                }

                R.id.action_delete -> {
                    val deleteDialog: DialogFragment =
                        DeleteThingsDialogFragment.newInstance(actionModeSet)
                    deleteDialog.show(fragmentManager, "DeleteThings")
                    mActionMode?.finish()
                }

                R.id.action_view -> {
                    if (actionModeSet.size == 1) {
                        val id = actionModeSet.iterator().next()
                        thingContent.setThingEdit(id)
                        mActionMode?.finish()
                        startActivity(ThingActivity.newIntent(mContext, false))
                    }
                }

                R.id.action_cut -> {
                    actionModeCopy = false
                    actionModePaste = true
                    menuRef?.setGroupVisible(R.id.group_actionmode_main, false)
                    menuRef?.setGroupVisible(R.id.group_actionmode_copy, true)
                    adapter?.notifyDataSetChanged()
                }

                R.id.action_paste -> {
                    thingContent.updateThings(actionModeSet)
                    mActionMode?.finish()
                }
            }
            return true
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            actionModeSet.clear()
            actionModePaste = false
            actionModeCopy = false
            mActionMode = null
            adapter?.notifyDataSetChanged()
        }
    }

    interface OnListFragmentInteractionListener {
        fun onListFragmentInteraction(item: Thing)
    }

    inner class ThingsRecyclerViewAdapter(
        private val mValues: List<Thing>,
        private val mListener: OnListFragmentInteractionListener?
    ) : RecyclerView.Adapter<ThingsRecyclerViewAdapter.ViewHolder>() {

        private val BACK_ITEM = " .."
        private var parent: Long = ThingContent.get(mContext).parent

        override fun onCreateViewHolder(parentView: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parentView.context)
                .inflate(R.layout.fragment_things, parentView, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val thing: Thing = if (parent == 0L || ThingContent.get(mContext).actionSearch) {
                mValues[position]
            } else {
                if (position == 0) Thing(parent, -1, BACK_ITEM)
                else mValues[position - 1]
            }
            tvPath.text = ThingContent.get(mContext).path
            holder.bindThing(thing)
        }

        override fun getItemCount(): Int {
            parent = ThingContent.get(mContext).parent
            return if (parent == 0L || ThingContent.get(mContext).actionSearch) {
                mValues.size
            } else {
                mValues.size + 1
            }
        }

        inner class ViewHolder(private val mView: View) :
            RecyclerView.ViewHolder(mView), View.OnClickListener, View.OnLongClickListener {

            private val mIconView: TextView = mView.findViewById(R.id.iconText)
            private val mNameThingView: TextView = mView.findViewById(R.id.nameThing)
            lateinit var mThing: Thing

            fun bindThing(thing: Thing) {
                mThing = thing

                if (!actionModeSet.contains(mThing.id)) {
                    if (mThing.isBox == Thing.AS_BOX) {
                        mIconView.setBackgroundResource(R.drawable.icon_box)
                    } else {
                        mIconView.setBackgroundResource(R.drawable.icon_thing)
                    }
                    mView.isSelected = false
                    if (mThing.thingName.isNotEmpty()) {
                        mIconView.text = mThing.thingName.first().uppercaseChar().toString()
                    } else {
                        mIconView.text = ""
                    }
                } else {
                    mIconView.text = mView.context.getString(R.string.char_pressed)
                    mView.isSelected = true
                }

                mNameThingView.text = mThing.thingName
                mView.setOnClickListener(this)

                if (mThing.thingName != BACK_ITEM) {
                    mView.setOnLongClickListener(this)
                    mIconView.setOnClickListener { clickActionMode() }
                } else {
                    mView.setOnLongClickListener(null)
                    mIconView.setOnClickListener(null)
                }
            }

            override fun onClick(v: View) {
                val thingContent = ThingContent.get(mContext)

                // Действие при нажатии на BACK_ITEM
                if (mThing.thingName == BACK_ITEM) {
                    thingContent.setParent(true)
                    parent = thingContent.parent
                    if (!actionModePaste) {
                        actionModeSet.clear()
                        mActionMode?.finish()
                    }
                    notifyDataSetChanged()
                    return
                }

                if (actionModeSet.isEmpty() || actionModePaste) {
                    mListener?.onListFragmentInteraction(mThing)
                    if (mThing.isBox == Thing.AS_BOX) {
                        parent = mThing.id
                        if (!(actionModePaste && actionModeSet.contains(parent))) {
                            thingContent.parent = parent
                            notifyDataSetChanged()
                        }
                    }
                } else {
                    clickActionMode()
                }
            }

            override fun onLongClick(view: View): Boolean {
                clickActionMode()
                return true
            }

            // Обработка выбора ActionMode
            private fun clickActionMode() {
                if (!actionModePaste) {
                    if (actionModeSet.isEmpty()) {
                        mActionMode = (mContext as AppCompatActivity).startSupportActionMode(callback)
                        actionModeSet.add(mThing.id)
                    } else if (actionModeSet.contains(mThing.id)) {
                        actionModeSet.remove(mThing.id)
                        if (actionModeSet.isEmpty()) {
                            mActionMode?.finish()
                        }
                    } else {
                        actionModeSet.add(mThing.id)
                    }

                    mActionMode?.title = actionModeSet.size.toString()
                    bindThing(mThing)
                }
            }

            override fun toString(): String = super.toString() + " '" + mNameThingView.text + "'"
        }
    }
}