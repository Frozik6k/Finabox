package ru.frozik6k.finabox

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

class DeleteThingsDialogFragment : DialogFragment() {

    companion object {
        private const val LOG_TAG = "deletedialog"
        private const val ARG_THINGS_ID = "things_id"

        fun newInstance(setIdThing: Set<Long>): DeleteThingsDialogFragment {
            val args = Bundle()
            val array = LongArray(setIdThing.size)
            val inputArray = setIdThing.toTypedArray()
            Log.d(LOG_TAG, "array = ${inputArray.contentToString()}")
            for (i in inputArray.indices) {
                array[i] = inputArray[i]
                Log.d(LOG_TAG, "id = ${inputArray[i]}")
            }
            args.putLongArray(ARG_THINGS_ID, array)
            return DeleteThingsDialogFragment().apply { arguments = args }
        }
    }

    private var form: View? = null
    private lateinit var mContext: Context
    private var mListener: DialogInterface.OnDismissListener? = null
    private lateinit var arrayIdThings: LongArray

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is DialogInterface.OnDismissListener) {
            mListener = context
        } else {
            throw RuntimeException("$context must implement OnDismissListener")
        }
        mContext = context
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        Log.d(LOG_TAG, "onCreateDialog")

        val activity = requireActivity()
        form = activity.layoutInflater.inflate(R.layout.fragment_dialog_delete_things, null)
        val listThing = form!!.findViewById<TextView>(R.id.tvListThing)

        val thingContent = ThingContent.get(mContext)
        arrayIdThings = arguments?.getLongArray(ARG_THINGS_ID)
            ?: return super.onCreateDialog(savedInstanceState)

        val things = buildString {
            append(' ')
            arrayIdThings.forEachIndexed { index, id ->
                val name = thingContent.getThing(id).thingName
                append(name)
                append(if (index == arrayIdThings.lastIndex) '.' else ", ")
            }
        }
        listThing.text = things

        val title = resources.getString(R.string.delete_things)
        return AlertDialog.Builder(activity)
            .setTitle(title)
            .setView(form)
            .setPositiveButton(android.R.string.ok) { _: DialogInterface, _: Int ->
                ThingContent.get(mContext).delThings(arrayIdThings)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        mListener?.onDismiss(dialog)
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
    }
}






