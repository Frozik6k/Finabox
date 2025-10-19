package ru.frozik6k.finabox

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import ru.frozik6k.lohouse.decoration.SpacesItemDecoration
import java.io.File

class LoadArchiveDialogFragment : Fragment() {

    private val LOG_TAG = "LoadArchive"

    private var mColumnCount: Int = 1
    private var mListener: OnDialogListener? = null
    private var adapter: ListArchiveRecyclerViewAdapter? = null
    private lateinit var recyclerView: RecyclerView

    companion object {
        private const val ARG_COLUMN_COUNT = "column-count"

        @JvmStatic
        fun newInstance(columnCount: Int): LoadArchiveDialogFragment {
            val fragment = LoadArchiveDialogFragment()
            val args = Bundle()
            args.putInt(ARG_COLUMN_COUNT, columnCount)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            mColumnCount = it.getInt(ARG_COLUMN_COUNT)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_archive_load_list, container, false)

        val context = view.context
        recyclerView = view.findViewById(R.id.list)
        recyclerView.layoutManager =
            if (mColumnCount <= 1) LinearLayoutManager(context)
            else GridLayoutManager(context, mColumnCount)

        updateUI()
        return view
    }

    private fun updateUI() {
        if (adapter == null) {
            val thingContent = ru.frozik6k.lohouse.thing.ThingContent.get(activity)
            val list: List<File>? = thingContent.backupList
            if (list == null || list.isEmpty()) {
                Toast.makeText(requireActivity().applicationContext, R.string.message_no_arhive, Toast.LENGTH_SHORT).show()
                mListener?.onDialog("", OnDialogListener.DIALOG_LOAD)
                return
            } else {
                adapter = ListArchiveRecyclerViewAdapter(list, mListener)
                recyclerView.adapter = adapter
            }
        } else {
            adapter?.notifyDataSetChanged()
        }

        recyclerView.itemAnimator = DefaultItemAnimator()
        recyclerView.addItemDecoration(SpacesItemDecoration(1))
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnDialogListener) {
            mListener = context
        } else {
            throw RuntimeException("$context must implement OnDialogListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    override fun onResume() {
        updateUI()
        super.onResume()
    }

    private inner class ListArchiveRecyclerViewAdapter(
        private val mValues: List<File>,
        private val mListener: OnDialogListener?
    ) : RecyclerView.Adapter<ListArchiveRecyclerViewAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.fragment_archive_load, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val file = mValues[position]
            holder.bindThing(file)
        }

        override fun getItemCount(): Int = mValues.size

        inner class ViewHolder(private val mView: View) :
            RecyclerView.ViewHolder(mView), View.OnClickListener {

            private val mIconView: TextView = mView.findViewById(R.id.iconText)
            private val mNameArchiveView: TextView = mView.findViewById(R.id.nameArchive)
            lateinit var mFile: File

            fun bindThing(file: File) {
                mFile = file
                mIconView.setBackgroundResource(R.drawable.icon_thing)
                mIconView.text = mFile.name.first().uppercaseChar().toString()
                mNameArchiveView.text = mFile.name
                mView.setOnClickListener(this)
            }

            override fun onClick(v: View?) {
                if (mListener != null) {
                    val builder = AlertDialog.Builder(requireActivity())
                    builder.setTitle(R.string.title_restore_load_archive)
                    val message = getString(R.string.message_restore_load_archive1) +
                            " " + mFile.name +
                            getString(R.string.message_restore_load_archive2)
                    builder.setMessage(message)
                    builder.setCancelable(true)
                    builder.setPositiveButton(R.string.ok) { _: DialogInterface, _: Int ->
                        mListener.onDialog(mFile.name, OnDialogListener.DIALOG_LOAD)
                    }
                    builder.setNegativeButton(R.string.cancel, null)
                    val dialog = builder.create()
                    dialog.show()
                }
            }

            override fun toString(): String {
                return super.toString() + " '" + mNameArchiveView.text + "'"
            }
        }
    }
}






