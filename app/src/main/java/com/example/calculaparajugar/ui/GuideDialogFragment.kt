package com.example.calculaparajugar.ui

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.example.calculaparajugar.R
import com.google.android.material.button.MaterialButton

class GuideDialogFragment : DialogFragment() {

    private data class GuideStep(
        val iconRes: Int,
        val title: String,
        val body: String
    )

    private val steps = listOf(
        GuideStep(
            R.drawable.ic_ui_apps,
            "Apps",
            "Elige que apps activan BrainWall. Puedes marcar algunas, seleccionar todas o activar el modo de todas las apps."
        ),
        GuideStep(
            R.drawable.ic_ui_timer,
            "Intervalo",
            "Define cada cuantos minutos aparece una pregunta mientras se usa una app monitoreada."
        ),
        GuideStep(
            R.drawable.ic_cat_math,
            "Categorias",
            "Escoge las materias que quieres practicar. Debe quedar al menos una categoria activa."
        ),
        GuideStep(
            R.drawable.ic_ui_lock,
            "Permisos",
            "BrainWall usa superposición para mostrar preguntas, acceso de uso para detectar localmente la app visible y notificaciones para mantener la protección activa. Puedes revisar cada estado en Ajustes."
        ),
        GuideStep(
            R.drawable.ic_ui_success,
            "Preguntas",
            "Después de 15 segundos de gracia aparece una pregunta. Los temas que más cuestan se repiten con mayor frecuencia y puedes pausar las preguntas durante 30 minutos."
        ),
        GuideStep(
            R.drawable.ic_ui_success,
            "Progreso seguro",
            "En Ajustes puedes guardar y restaurar niveles, racha, categorías y estadísticas. El PIN nunca se incluye en el respaldo."
        )
    )

    private var index = 0

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = layoutInflater.inflate(R.layout.dialog_guide, null)
        val icon = view.findViewById<ImageView>(R.id.imgGuideIcon)
        val step = view.findViewById<TextView>(R.id.tvGuideStep)
        val title = view.findViewById<TextView>(R.id.tvGuideTitle)
        val body = view.findViewById<TextView>(R.id.tvGuideBody)
        val back = view.findViewById<MaterialButton>(R.id.btnGuideBack)
        val next = view.findViewById<MaterialButton>(R.id.btnGuideNext)

        fun render() {
            val item = steps[index]
            icon.setImageResource(item.iconRes)
            step.text = "${index + 1} de ${steps.size}"
            title.text = item.title
            body.text = item.body
            back.visibility = if (index == 0) View.GONE else View.VISIBLE
            next.text = if (index == steps.lastIndex) "Listo" else "Siguiente"
        }

        return Dialog(requireContext()).also { dialog ->
            dialog.setContentView(view)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.setOnShowListener {
                dialog.window?.setLayout(
                    (resources.displayMetrics.widthPixels * 0.88f).toInt(),
                    WindowManager.LayoutParams.WRAP_CONTENT
                )
            }
            render()

            back.setOnClickListener {
                if (index > 0) {
                    index--
                    render()
                }
            }

            next.setOnClickListener {
                if (index == steps.lastIndex) {
                    dialog.dismiss()
                } else {
                    index++
                    render()
                }
            }
        }
    }
}
