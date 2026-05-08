import com.intellij.openapi.ui.DialogWrapper
import com.pan.plugin.GlobalConfigService
import kotlinx.serialization.Serializable
import java.awt.GridLayout
import javax.swing.*

@Serializable
data class SvgOption(
    val key: String,
    val label: String,
    var checked: Boolean = false
)

class SvgSettingsDialog(
    options: List<SvgOption>
) : DialogWrapper(true) {

    private val resetAction = object : AbstractAction("Reset") {
        override fun actionPerformed(e: java.awt.event.ActionEvent?) {
            val ins = GlobalConfigService.getInstance()
            ins.reset();

            checkboxes.forEach { cb ->
                val key = cb.getClientProperty("key") as String
                cb.isSelected = ins.state.optimizeOptions[key] ?: false
            }
        }
    }

    private val checkboxes: List<JCheckBox> = options.map {
        val jb = JCheckBox(it.label, it.checked)
        jb.putClientProperty("key", it.key) // 绑定 key
        return@map jb
    }

    var selectedOptions = options;

    init {
        title = "Svgo Settings"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(GridLayout(0, 1, 5, 5))
        checkboxes.forEach { panel.add(it) }
        val scrollPane = com.intellij.ui.components.JBScrollPane(panel)
        scrollPane.preferredSize = java.awt.Dimension(400, 500)
        return scrollPane
    }

    override fun doOKAction() {
        selectedOptions = checkboxes.map { SvgOption(it.getClientProperty("key") as String, it.text, it.isSelected) }
        println("Selected options: $selectedOptions")
        super.doOKAction()
    }

    override fun createActions(): Array<Action> {
        return arrayOf(
            resetAction,
            okAction,
            cancelAction
        )
    }
}
