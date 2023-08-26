package com.tfkfan.styushsoft

import javafx.application.Platform
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.scene.text.TextFlow
import javafx.stage.DirectoryChooser
import org.apache.poi.hwpf.*
import org.apache.poi.hwpf.extractor.WordExtractor
import org.apache.poi.xwpf.extractor.XWPFWordExtractor
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.awt.Desktop
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger


class MainController {
    @FXML
    private lateinit var directorySearchInput: TextField

    @FXML
    private lateinit var directorySearch: Button

    @FXML
    private lateinit var loader: ProgressBar

    @FXML
    private lateinit var search: Button

    @FXML
    private lateinit var stop: Button

    @FXML
    private lateinit var searchText: TextArea

    @FXML
    private lateinit var foundDocuments: TextFlow

    @FXML
    private lateinit var foundLabel: Label

    @FXML
    private lateinit var recursiveSearchCheckbox: CheckBox

    private var executorService = Executors.newFixedThreadPool(8)
    private var directory: File? = null
    private val processed = AtomicInteger(0)
    private val found = AtomicInteger(0)
    private val total = AtomicInteger(0)
    private val stopped = AtomicBoolean(false)

    @FXML
    private fun onDirectorySearchClick() {
        val fileChooser = DirectoryChooser()
        directory = fileChooser.showDialog(null)
        directorySearchInput.text = directory?.absolutePath
    }

    @FXML
    private fun onStopClick() {
        stopped.set(true)
        executorService.shutdownNow()
    }

    @FXML
    private fun onSearchClick() {
        if (directory == null) return
        foundLabel.text = ""
        stopped.set(false)
        processed.set(0)
        found.set(0)
        updateProgress(true)
        foundDocuments.children.clear()
        foundDocuments.isVisible = true

        executorService = Executors.newFixedThreadPool(8)
        executorService.submit {
            val text = searchText.text
            try {
                val files = ArrayList<File>()

                if (recursiveSearchCheckbox.isSelected) {
                    val fileStack = Stack<File>()
                    directory!!.listFiles()?.let { fileStack.addAll(it) }
                    while (!fileStack.isEmpty()) {
                        val file = fileStack.pop()
                        if (file.isDirectory) file!!.listFiles()?.let { fileStack.addAll(it) }
                        else if (file.isFile) files.add(file)
                    }
                } else
                    directory!!.listFiles()?.let { files.addAll(it) }

                total.set(files.size)

                for (file in files) {
                    if (stopped.get())
                        break
                    if (!file.isFile) {
                        processed.incrementAndGet()
                        Platform.runLater {
                            updateProgress()
                        }
                        continue
                    }

                    executorService.submit {
                        try {

                            val inputString = try {
                                println(file.extension)
                                when (file.extension) {
                                    "doc" -> {
                                        WordExtractor(HWPFDocument(file.inputStream())).text
                                    }

                                    "docx" -> {
                                        XWPFWordExtractor(XWPFDocument(file.inputStream())).text
                                    }

                                    else -> {
                                        val inputStream: InputStream = file.inputStream()
                                        inputStream.bufferedReader().use { it.readText() }
                                    }
                                }
                            } catch (e: Exception) {
                                processed.incrementAndGet()
                                Platform.runLater {
                                    updateProgress()
                                }
                                return@submit
                            }
                            if (inputString.contains(text, true)) {
                                found.incrementAndGet()
                                Platform.runLater {
                                    val link = Hyperlink(file.absolutePath)
                                    foundDocuments.children.add(link)
                                    link.setOnAction { _: ActionEvent ->
                                        try {
                                            if (Desktop.isDesktopSupported()) {
                                                try {
                                                    Desktop.getDesktop().open(file)
                                                } catch (ex: IOException) {
                                                    println(ex)
                                                }
                                            }
                                        } catch (e: Exception) {
                                            println(e)
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            println(e)
                        }
                        processed.incrementAndGet()
                        Platform.runLater {
                            updateProgress()
                        }
                    }
                }
            } catch (ex: Exception) {
                println(ex)
            }
        }
    }

    private fun updateProgress(setZero: Boolean = false) {
        synchronized(loader) {
            foundLabel.isVisible = true
            foundLabel.text = "Найдено в ${found.get()} документах из ${total.get()}"
            if (setZero) loader.progress = 0.0
            else loader.progress = processed.get() / total.get().toDouble() + 1 / total.get().toDouble()
        }
    }
}
