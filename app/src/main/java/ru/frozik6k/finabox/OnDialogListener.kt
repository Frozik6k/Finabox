package ru.frozik6k.finabox

interface OnDialogListener {
    fun onDialog(name: String, isDialog: Int)

    companion object {
        const val DIALOG_SAVE = 1
        const val DIALOG_LOAD = 2
    }
}