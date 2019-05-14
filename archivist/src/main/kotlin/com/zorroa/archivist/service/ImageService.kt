package com.zorroa.archivist.service

import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import com.zorroa.archivist.config.ApplicationProperties
import com.zorroa.archivist.domain.FileStorage
import com.zorroa.archivist.domain.WatermarkSettingsChanged
import com.zorroa.archivist.security.getUsername
import com.zorroa.archivist.util.copyInputToOuput
import com.zorroa.common.schema.Proxy
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.CacheControl
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.awt.AlphaComposite
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.awt.Graphics2D
import java.awt.Image
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.time.Duration
import java.util.Date
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import javax.annotation.PostConstruct
import javax.imageio.ImageIO
import javax.imageio.stream.FileImageInputStream
import javax.servlet.http.HttpServletResponse
import io.micrometer.core.instrument.Timer as MeterTimer

inline fun bufferedImageToInputStream(size: Int, img: BufferedImage): InputStream {
    val ostream = object : ByteArrayOutputStream(size) {
        // Overriding this to not create a copy
        @Synchronized
        override fun toByteArray(): ByteArray {
            return this.buf
        }
    }
    ImageIO.write(img, "jpg", ostream)
    return ByteArrayInputStream(ostream.toByteArray())
}

/**
 * Created by chambers on 7/8/16.
 */
interface ImageService {

    /**
     * Stream the given stored file to the [HttpServletResponse].
     *
     * @param rsp The [HttpServletResponse] to stream to.
     * @param storage The stored file.
     * @param isWatermarkSize Set to True if the image should be watermarked.
     */
    @Throws(IOException::class)
    fun serveImage(rsp: HttpServletResponse, storage: FileStorage, isWatermarkSize: Boolean)

    /**
     * Stream the given Proxy file to the provided [HttpServletResponse]
     *
     * @param rsp The [HttpServletResponse] to stream to.
     * @param proxy The [Proxy] to stram.
     */
    @Throws(IOException::class)
    fun serveImage(rsp: HttpServletResponse, proxy: Proxy?)

    /**
     * Stream the given stored file to the [HttpServletResponse]
     *
     * @param rsp The [HttpServletResponse] to stream to.
     * @param storage The stored file.
     */
    @Throws(IOException::class)
    fun serveImage(rsp: HttpServletResponse, storage: FileStorage)

    /**
     * Watermark the given [InputStream] and return a watermarked  BufferedImage.
     *
     * @param inputStream an [InputStream] pointing to an image.
     * @return A watermarked [BufferedImage]
     */
    fun watermark(inputStream: InputStream): BufferedImage

    /**
     * Find the dimensions for the given image without loading the
     * whole image into memory.
     *
     * @param imgFile A file pointing to an image.
     * @return a [Dimension] representing the size of the image.
     */
    fun getImageDimension(imgFile: File): Dimension
}

/**
 * Created by chambers on 7/8/16.
 */
@Service
class ImageServiceImpl @Autowired constructor(
    private val fileStorageService: FileStorageService,
    private val fileServerProvider: FileServerProvider,
    private val properties: ApplicationProperties,
    private val eventBus: EventBus,
    private val meterRegistry: MeterRegistry

) : ImageService {

    private var watermarkEnabled: Boolean = false
    private var watermarkMinProxySize: Int = 0
    private var watermarkTemplate: String = ""
    private var watermarkScale: Double = 1.0
    private var watermarkImage: BufferedImage? = null
    private var watermarkImageScale: Double = 0.2
    private var watermarkFontName: String = "Arial"
    private var timerBuilder = MeterTimer.builder("zorroa.ImageService.timer")
        .publishPercentileHistogram()
        .maximumExpectedValue(Duration.ofSeconds(5))

    @PostConstruct
    fun init() {
        setupWaterMarkResources(null)
        eventBus.register(this)
    }

    @Throws(IOException::class)
    override fun serveImage(
        rsp: HttpServletResponse,
        storage: FileStorage
    ) {

        val file = fileServerProvider.getServableFile(storage)
        val localFile = file.getLocalFile()
        if (watermarkEnabled && localFile != null) {
            val dim = getImageDimension(localFile.toFile())
            val isWatermarkSize = (dim.width <= watermarkMinProxySize && dim.height <= watermarkMinProxySize)
            serveImage(rsp, storage, isWatermarkSize)
        }
        else {
            serveImage(rsp, storage, false)
        }
    }

    @Throws(IOException::class)
    override fun serveImage(
        rsp: HttpServletResponse,
        storage: FileStorage,
        isWatermarkSize: Boolean
    ) {
        if (storage == null) {
            rsp.status = HttpStatus.NOT_FOUND.value()
        }
        val file = fileServerProvider.getServableFile(storage)
        val stat = file.getStat()

        rsp.setHeader("Pragma", "")
        rsp.bufferSize = BUFFER_SIZE
        if (watermarkEnabled && isWatermarkSize) {
            val timer = timerBuilder.tags("watermark", "true").register(meterRegistry)
            timer.record {
                val image = watermark(file.getInputStream())
                rsp.contentType = MediaType.IMAGE_JPEG_VALUE
                rsp.setHeader("Cache-Control", CacheControl.maxAge(1, TimeUnit.DAYS).cachePrivate().headerValue)
                ImageIO.write(image, "jpg", rsp.outputStream)
            }
        } else {
            val timer = timerBuilder.tags("watermark", "false").register(meterRegistry)
            timer.record {
                rsp.contentType = stat.mediaType
                rsp.setContentLengthLong(stat.size)
                rsp.setHeader("Cache-Control", CacheControl.maxAge(7, TimeUnit.DAYS).cachePrivate().headerValue)
                copyInputToOuput(file.getInputStream(), rsp.outputStream)
            }
        }
    }

    @Throws(IOException::class)
    override fun serveImage(rsp: HttpServletResponse, proxy: Proxy?) {
        if (proxy == null) {
            rsp.status = HttpStatus.NOT_FOUND.value()
            return
        }
        val isWatermarkSize = (proxy.width <= watermarkMinProxySize && proxy.height <= watermarkMinProxySize)
        val st = fileStorageService.get(proxy.id)
        serveImage(rsp, st, isWatermarkSize)
    }

    override fun watermark(inputStream: InputStream): BufferedImage {
        val req = (RequestContextHolder.currentRequestAttributes() as ServletRequestAttributes).request
        val src = ImageIO.read(inputStream)
        val g2d = src.createGraphics()
        try {
            if (watermarkImage != null) {
                watermarkImage?.let {
                    val width = src.width.times(watermarkImageScale).toInt()
                    val image = it.getScaledInstance(width, -1, Image.SCALE_SMOOTH)
                    val xpos = src.width - image.getWidth(null) - 10
                    val ypos = src.height - image.getHeight(null) - 10
                    g2d.drawImage(image, xpos, ypos, null)
                }
            } else {
                val replacements = mapOf(
                    "USER" to getUsername(),
                    "DATE" to SimpleDateFormat("MM/dd/yyyy").format(Date()),
                    "IP" to (req.getHeader("X-FORWARDED-FOR") ?: req.remoteAddr),
                    "HOST" to req.remoteHost
                )

                val sb = StringBuffer(watermarkTemplate.length * 2)
                val m = PATTERN.matcher(watermarkTemplate)
                while (m.find()) {
                    try {
                        m.appendReplacement(sb, replacements[m.group(1)])
                    } catch (ignore: Exception) {
                        //
                    }
                }
                m.appendTail(sb)
                val text = sb.toString()

                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
                val c = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f)
                g2d.composite = c
                g2d.font = getWatermarkFont(g2d, text, src.width)
                val x = ((src.width - g2d.getFontMetrics(g2d.font).stringWidth(text)) / 2).toFloat()
                val y = src.height - (1.1f * g2d.getFontMetrics(g2d.font).height)
                g2d.paint = Color.black
                g2d.drawString(text, x - 1, y + 1)
                g2d.drawString(text, x - 1, y - 1)
                g2d.drawString(text, x + 1, y + 1)
                g2d.drawString(text, x + 1, y - 1)
                g2d.paint = Color.white
                g2d.drawString(text, x, y)
            }
        } finally {
            g2d.dispose()
        }
        return src
    }

    override fun getImageDimension(imgFile: File): Dimension {
        val pos = imgFile.name.lastIndexOf(".")
        if (pos == -1)
            throw RuntimeException("No extension for file: " + imgFile.absolutePath)
        val suffix = imgFile.name.substring(pos + 1)
        val iter = ImageIO.getImageReadersBySuffix(suffix)
        while (iter.hasNext()) {
            val reader = iter.next()
            try {
                val stream = FileImageInputStream(imgFile)
                reader.input = stream
                val width = reader.getWidth(reader.minIndex)
                val height = reader.getHeight(reader.minIndex)
                return Dimension(width, height)
            } catch (e: IOException) {
                logger.warn("Error reading: ${imgFile.absolutePath}", e)
            } finally {
                reader.dispose()
            }
        }

        throw IOException("Not a known image file: " + imgFile.absolutePath)
    }

    @Synchronized
    @Subscribe
    fun setupWaterMarkResources(e: WatermarkSettingsChanged?) {
        watermarkEnabled = properties.getBoolean("archivist.watermark.enabled")
        watermarkTemplate = properties.getString("archivist.watermark.template")
        watermarkMinProxySize = properties.getInt("archivist.watermark.min-proxy-size")
        watermarkScale = properties.getDouble("archivist.watermark.scale")
        watermarkFontName = properties.getString("archivist.watermark.font-name")
        watermarkImageScale = properties.getDouble("archivist.watermark.image-scale")

        val imagePath: String? = properties.getString("archivist.watermark.image-path")
        if (imagePath != null && imagePath.isNotBlank()) {
            try {
                logger.info("loading watermark image: '{}'", imagePath)
                watermarkImage = ImageIO.read(File(imagePath))
            } catch (e: Exception) {
                logger.warn("Failed to load watermark Image '{}'", imagePath, e)
            }
        } else {
            watermarkImage = null
        }
    }

    /**
     * Calculates the correct font size for the watermark based on the width and height of the
     * image and watermark scale. Returns the Font to use for the watermark.
     *
     * @param g2d the current graphics2D instance
     * @param text the string to calculate the size of
     * @param imageWidth the full image width
     */
    fun getWatermarkFont(g2d: Graphics2D, text: String, imageWidth: Int): Font {
        val baseFontSize = 20
        val baseFont = Font(watermarkFontName, Font.PLAIN, baseFontSize)
        val fontWidth = g2d.getFontMetrics(baseFont).stringWidth(text)
        val scale = (imageWidth.toFloat() * 0.75) / fontWidth.toFloat()
        var fontSize = baseFontSize.toFloat() * scale * watermarkScale
        return Font(watermarkFontName, Font.PLAIN, fontSize.toInt())
    }

    companion object {

        private val logger = LoggerFactory.getLogger(ImageServiceImpl::class.java)

        /**
         * Output stream buffer size
         */
        const val BUFFER_SIZE = 16 * 1024

        /**
         * The pattern used to define text replacements in the watermark texts
         */
        private val PATTERN = Pattern.compile("#\\[(.*?)\\]")
    }
}
