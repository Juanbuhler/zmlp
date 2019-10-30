package com.zorroa

import com.aspose.pdf.Document
import com.aspose.pdf.DocumentInfo
import com.aspose.pdf.devices.JpegDevice
import com.aspose.pdf.devices.Resolution
import com.aspose.pdf.facades.PdfExtractor
import com.aspose.pdf.facades.PdfFileInfo
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.nio.charset.Charset
import kotlin.system.measureTimeMillis

/**
 * Handles rendering a PDF as an image and json metadata file.
 */
class PdfDocument(options: Options) : com.zorroa.Document(options) {

    val pdfDocument = Document(ioHandler.getInputPath())

    init {
        logger.info("opening file: {}", options.inputFile)
    }

    override fun renderAllImages() {
        val stack = PdfImageRenderStack(this, options)
        val fileInfo = PdfFileInfo(pdfDocument)
        for (page in 1..fileInfo.numberOfPages) {
            stack.renderImage(page)
        }
    }

    override fun renderAllMetadata() {
        val fileInfo = PdfFileInfo(pdfDocument)
        for (page in 1..fileInfo.numberOfPages) {
            renderMetadata(page)
        }
    }

    override fun renderImage(page: Int) {
        PdfImageRenderStack(this, options).renderImage(page)
    }

    override fun renderMetadata(page: Int) {
        val time = measureTimeMillis {
            val documentInfo = DocumentInfo(pdfDocument)
            val fileInfo = PdfFileInfo(pdfDocument)

            val metadata = mutableMapOf<String, Any?>()
            val height = fileInfo.getPageHeight(page)
            val width = fileInfo.getPageWidth(page)

            metadata["title"] = fileInfo.title
            metadata["author"] = fileInfo.author
            metadata["keywords"] = fileInfo.keywords
            metadata["description"] = fileInfo.subject
            metadata["creator"] = fileInfo.creator
            metadata["timeCreated"] = try {
                documentInfo.creationDate
            } catch (e: Exception) {
                null
            }
            metadata["timeModified"] = try {
                documentInfo.modDate
            } catch (e: Exception) {
                null
            }
            metadata["pages"] = fileInfo.numberOfPages
            metadata["height"] = height
            metadata["width"] = width
            metadata["orientation"] = if (height > width) "portrait" else "landscape"

            if (options.content) {
                metadata["content"] = extractPageContent(page)
            }

            Json.mapper.writeValue(getMetadataFile(page), metadata)
        }

        logMetadataTime(page, time)
    }

    private fun extractPageContent(page: Int): String? {
        val pdfExtractor = PdfExtractor()

        pdfExtractor.bindPdf(pdfDocument)
        pdfExtractor.startPage = page
        pdfExtractor.endPage = page

        pdfExtractor.extractText(Charset.forName("UTF-8"))

        val byteStream = ByteArrayOutputStream()
        pdfExtractor.getText(byteStream)

        return byteStream.toString("UTF-8").replace(whitespaceRegex, " ")
    }

    override fun close() {
        try {
            logger.info("closing file: {}", options.inputFile)
            pdfDocument.close()
        } catch (e: Exception) {
            // ignore
        }
    }

    companion object {
        init {
            val classLoader = this::class.java.classLoader
            val licenseAsStream = classLoader.getResourceAsStream(ASPOSE_LICENSE_FILE)
            com.aspose.pdf.License().setLicense(licenseAsStream)
        }

        fun extractPdfText(byteArray: ByteArray): String {
            val pdfStream = ByteArrayInputStream(byteArray)

            val tmpPdf = Document(pdfStream)
            val pdfExtractor = PdfExtractor()

            pdfExtractor.bindPdf(tmpPdf)
            val byteStream = ByteArrayOutputStream()

            pdfExtractor.extractText(Charset.forName("UTF-8"))
            pdfExtractor.getText(byteStream)
            return byteStream.toString("UTF-8")
        }
    }

    /**
     * A helper class which makes it easy to resuse a bytestream and
     * jpegDevice when rendering all pages.
     */
    class PdfImageRenderStack(private val doc: PdfDocument, val options: Options) {

        private val byteStream = ByteArrayOutputStream(1024 * 25)
        private val jpegDevice = JpegDevice(Resolution(options.dpi), 100)

        fun renderImage(page: Int) {
            val time = measureTimeMillis {
                val path = doc.ioHandler.getImagePath(page)
                jpegDevice.process(doc.pdfDocument.pages.get_Item(page), byteStream)
                FileOutputStream(path.toFile()).use { outputStream ->
                    byteStream.writeTo(outputStream)
                }
                byteStream.reset()
            }
            doc.logImageTime(page, time)
        }
    }
}
