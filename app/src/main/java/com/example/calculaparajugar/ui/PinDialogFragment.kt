package com.example.calculaparajugar.ui

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.calculaparajugar.R
import com.example.calculaparajugar.util.PinManager
import com.google.android.material.button.MaterialButton

/**
 * Diálogo de PIN con 3 modos:
 * - SETUP: primera vez, crear PIN directamente
 * - UNLOCK: verificar PIN para acceder
 * - CHANGE: pide PIN actual, luego permite crear uno nuevo
 */
class PinDialogFragment : DialogFragment() {

    companion object {
        private const val ARG_MODE = "mode"
        private const val ARG_INITIAL_PROMPT = "initial_prompt"
        const val MODE_SETUP = "setup"
        const val MODE_UNLOCK = "unlock"
        const val MODE_CHANGE = "change"
        const val MODE_UNLOCK_ACTION = "unlock_action"

        fun newInstance(isSetup: Boolean): PinDialogFragment {
            val mode = if (isSetup) MODE_SETUP else MODE_UNLOCK
            return newInstanceWithMode(mode)
        }

        fun newInstanceWithMode(mode: String): PinDialogFragment {
            return PinDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_MODE, mode)
                }
            }
        }

        fun newInitialSetupInstance(): PinDialogFragment {
            return newInstanceWithMode(MODE_SETUP).apply {
                arguments!!.putBoolean(ARG_INITIAL_PROMPT, true)
            }
        }
    }

    var onUnlockSuccess: (() -> Unit)? = null
    private var changeStep = 0 // 0 = verificar actual, 1 = crear nuevo

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val mode = arguments?.getString(ARG_MODE, MODE_SETUP) ?: MODE_SETUP
        val view = layoutInflater.inflate(R.layout.dialog_pin, null)
        val etPin = view.findViewById<EditText>(R.id.etPin)
        val tvTitle = view.findViewById<TextView>(R.id.tvPinTitle)
        val tvSubtitle = view.findViewById<TextView>(R.id.tvPinSubtitle)
        val btnCancel = view.findViewById<MaterialButton>(R.id.btnCancelPin)
        val btnConfirm = view.findViewById<MaterialButton>(R.id.btnConfirmPin)

        val title: String
        val subtitle: String
        val positiveText: String

        when (mode) {
            MODE_SETUP -> {
                title = "Crear PIN"
                subtitle = "Elige un PIN numerico de al menos 4 digitos."
                positiveText = "Guardar"
            }
            MODE_UNLOCK, MODE_UNLOCK_ACTION -> {
                title = "Ingresar PIN"
                subtitle = "Desbloquea los ajustes para continuar."
                positiveText = "Desbloquear"
            }
            MODE_CHANGE -> {
                title = "PIN actual"
                subtitle = "Confirma tu PIN antes de crear uno nuevo."
                positiveText = "Verificar"
            }
            else -> {
                title = "PIN"
                subtitle = "Ingresa tu PIN para continuar."
                positiveText = "OK"
            }
        }

        tvTitle.text = title
        tvSubtitle.text = subtitle
        btnConfirm.text = positiveText
        btnCancel.visibility = if (mode == MODE_UNLOCK) View.GONE else View.VISIBLE

        return Dialog(requireContext()).also { dialog ->
            dialog.setContentView(view)
            dialog.setCancelable(mode != MODE_UNLOCK)
            dialog.setCanceledOnTouchOutside(mode != MODE_UNLOCK)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            btnCancel.setOnClickListener {
                rememberInitialPromptDismissal()
                dialog.dismiss()
            }
            btnConfirm.setOnClickListener {
                val pin = etPin.text.toString()

                when (mode) {
                    MODE_SETUP -> handleSetup(pin, dialog)
                    MODE_UNLOCK -> handleUnlock(pin, dialog)
                    MODE_UNLOCK_ACTION -> handleUnlockAction(pin, dialog)
                    MODE_CHANGE -> handleChange(pin, etPin, tvTitle, tvSubtitle, btnConfirm, dialog)
                }
            }
        }
    }

    override fun onCancel(dialog: DialogInterface) {
        rememberInitialPromptDismissal()
        super.onCancel(dialog)
    }

    private fun rememberInitialPromptDismissal() {
        if (arguments?.getBoolean(ARG_INITIAL_PROMPT, false) == true &&
            !PinManager.hasPin(requireContext())) {
            PinManager.dismissInitialPrompt(requireContext())
        }
    }

    private fun handleSetup(pin: String, dialog: Dialog) {
        if (pin.length < 4) {
            Toast.makeText(context, "Mínimo 4 dígitos", Toast.LENGTH_SHORT).show()
            return
        }
        PinManager.setPin(requireContext(), pin)
        Toast.makeText(context, "PIN creado", Toast.LENGTH_SHORT).show()
        dialog.dismiss()
        onUnlockSuccess?.invoke()
    }

    private fun handleUnlock(pin: String, dialog: Dialog) {
        if (pin.isEmpty()) return
        if (PinManager.verifyPin(requireContext(), pin)) {
            Toast.makeText(context, "Acceso concedido", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            onUnlockSuccess?.invoke()
        } else {
            Toast.makeText(context, "PIN incorrecto", Toast.LENGTH_SHORT).show()
            requireActivity().finish()
        }
    }

    private fun handleUnlockAction(pin: String, dialog: Dialog) {
        if (pin.isEmpty()) return
        if (PinManager.verifyPin(requireContext(), pin)) {
            Toast.makeText(context, "Acceso concedido", Toast.LENGTH_SHORT).show()
            onUnlockSuccess?.invoke()
            dialog.dismiss()
        } else {
            Toast.makeText(context, "PIN incorrecto", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleChange(
        pin: String,
        etPin: EditText,
        tvTitle: TextView,
        tvSubtitle: TextView,
        btnConfirm: MaterialButton,
        dialog: Dialog
    ) {
        if (changeStep == 0) {
            // Paso 1: verificar PIN actual
            if (pin.isEmpty()) return
            if (PinManager.verifyPin(requireContext(), pin)) {
                // Correcto: pasar a crear nuevo
                changeStep = 1
                etPin.text.clear()
                tvTitle.text = "Nuevo PIN"
                tvSubtitle.text = "Crea un PIN numerico de al menos 4 digitos."
                btnConfirm.text = "Guardar"
                Toast.makeText(context, "Verificado. Ingresa el nuevo PIN", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "PIN incorrecto", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Paso 2: guardar nuevo PIN
            if (pin.length < 4) {
                Toast.makeText(context, "Mínimo 4 dígitos", Toast.LENGTH_SHORT).show()
                return
            }
            PinManager.setPin(requireContext(), pin)
            Toast.makeText(context, "PIN actualizado", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
    }
}
