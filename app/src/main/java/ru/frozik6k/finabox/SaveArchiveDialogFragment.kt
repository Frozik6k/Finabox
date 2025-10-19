package ru.frozik6k.finabox

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.text.InputFilter
import android.text.Spanned
import android.view.View
import android.widget.EditText
import ru.frozik6k.lohouse.thing.Thing
import java.util.Date
import java.util.regex.Pattern

class SaveArchiveDialogFragment : DialogFragment(), DialogInterface.OnClickListener {

    private var form: View? = null
    private var mListener: OnDialogListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mListener = if (context is OnDialogListener) {
            context
        } else {
            throw ClassCastException("$context must implement ru.frozik6k.lohouse.OnDialogListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        form = requireActivity().layoutInflater.inflate(R.layout.fragment_dialog_save_archive, null)
        val zipNameEt = form!!.findViewById<EditText>(R.id.nameArchive)

        val inpf = object : InputFilter {
            override fun filter(
                source: CharSequence,
                start: Int,
                end: Int,
                dest: Spanned,
                dstart: Int,
                dend: Int
            ): CharSequence? {
                val text = dest.subSequence(0, dstart).toString() +
                        source.subSequence(start, end) +
                        dest.subSequence(dend, dest.length).toString()
                val pattern = "[а-яА-ЯёЁa-zA-Z0-9]+"
                return if (!Pattern.matches(pattern, text)) "" else null
            }
        }
        zipNameEt.filters = arrayOf(inpf)

        val title = resources.getString(R.string.save_archive)
        return AlertDialog.Builder(requireActivity())
            .setTitle(title)
            .setView(form)
            .setPositiveButton(android.R.string.ok, this)
            .setNegativeButton(android.R.string.cancel, null)
            .create()
    }

    override fun onClick(dialogInterface: DialogInterface, which: Int) {
        val nameEt = form!!.findViewById<EditText>(R.id.nameArchive)
        var name = nameEt.text?.toString() ?: ""
        if (name.isEmpty()) {
            name = requireActivity().resources.getString(R.string.app_name) + Thing.dateToString(Date())
        }
        mListener?.onDialog(name, OnDialogListener.DIALOG_SAVE)
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
    }
}






