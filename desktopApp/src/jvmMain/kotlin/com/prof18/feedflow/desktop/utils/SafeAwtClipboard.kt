package com.prof18.feedflow.desktop.utils

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.NativeClipboard
import androidx.compose.ui.platform.asAwtTransferable
import java.awt.HeadlessException
import java.awt.Toolkit
import java.awt.datatransfer.ClipboardOwner
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException

@OptIn(ExperimentalComposeUiApi::class)
internal class SafeAwtClipboard : Clipboard {

    private val systemClipboard by lazy {
        try {
            Toolkit.getDefaultToolkit().systemClipboard
        } catch (_: HeadlessException) {
            null
        }
    }

    override suspend fun getClipEntry(): ClipEntry? {
        return try {
            val transferable = systemClipboard?.getContents(null) ?: return null
            val flavors = transferable.transferDataFlavors
            if (flavors.isNullOrEmpty()) return null
            ClipEntry(transferable)
        } catch (_: IllegalStateException) {
            // Windows locks the system clipboard while another process owns it. Compose's default
            // AWT clipboard lets that transient failure escape during paste, which crashes the app.
            null
        }
    }

    override suspend fun setClipEntry(clipEntry: ClipEntry?) {
        try {
            val transferable = clipEntry?.asAwtTransferable
            systemClipboard?.setContents(
                transferable ?: EmptyTransferable,
                transferable as? ClipboardOwner,
            )
        } catch (_: IllegalStateException) {
            // Treat a temporary Windows clipboard lock as unavailable instead of fatal.
        }
    }

    override val nativeClipboard: NativeClipboard
        get() = systemClipboard ?: NoClipboard
}

private object NoClipboard

private object EmptyTransferable : Transferable {
    override fun getTransferDataFlavors(): Array<DataFlavor> = emptyArray()

    override fun isDataFlavorSupported(flavor: DataFlavor?): Boolean = false

    override fun getTransferData(flavor: DataFlavor?): Any {
        throw UnsupportedFlavorException(flavor)
    }
}
