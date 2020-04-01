package com.zorroa.archivist.domain

/**
 * Standard Docker containers.
 */
object StandardContainers {
    const val CORE = "zmlp/plugins-core"
    const val ANALYSIS = "zmlp/plugins-analysis"
}

object Category {
    const val GOOGLE_VIDEO = "Video Intelligence"
    const val GOOGLE_VISION = "Google Vision"
    const val AWS_REK = "Amazon Rekognition"
    const val ZORROA_TL = "Zorroa Timeline Extraction"
    const val ZORROA_STD = "Zorroa Visual Intelligence"
    const val CLARIFAI_STD = "Clarifai Standard"
}

object Provider {
    const val CLARIFAI = "Clarifai"
    const val ZORROA = "Zorroa"
    const val GOOGLE = "Google"
    const val AMAZON = "Amazon"
}

object ModType {
    const val OCR = "Optical Character Recognition (OCR)"
    const val LABEL_DETECTION = "Label Detection"
    const val OBJECT_DETECTION = "Object Detection"
    const val LANDMARK_DETECTION = "Landmark Detection"
    const val LOGO_DETECTION = "Logo Detection"
    const val FACE_RECOGNITION = "Face Recognition"
    const val CLIPIFIER = "Asset Clipifier"
}

/**
 * Return a list of the standard pipeline modules.
 */
fun getStandardModules(): List<PipelineModSpec> {
    return listOf(
        PipelineModSpec(
            "zvi-document-page-clips",
            "Extract all pages in MS Office/PDF documents into separate assets.",
            Provider.ZORROA,
            Category.ZORROA_TL,
            ModType.CLIPIFIER,
            listOf(SupportedMedia.Documents),
            listOf(
                ModOp(
                    ModOpType.SET_ARGS,
                    mapOf("extract_doc_pages" to true),
                    OpFilter(OpFilterType.REGEX, ".*FileImportProcessor")
                )
            ),
            restricted = false,
            standard = true
        ),
        PipelineModSpec(
            "zvi-image-page-clips",
            "Extract all layers in multi page image formats such as tiff and psd as as " +
                "separate assets",
            Provider.ZORROA,
            Category.ZORROA_TL,
            ModType.CLIPIFIER,
            listOf(SupportedMedia.Images),
            listOf(
                ModOp(
                    ModOpType.SET_ARGS,
                    mapOf("extract_image_pages" to true),
                    OpFilter(OpFilterType.REGEX, ".*FileImportProcessor")
                )
            ),
            restricted = false,
            standard = true
        ),
        PipelineModSpec(
            "zvi-video-shot-clips",
            "Break video files into individual assets based on a shot detection algorithm.",
            Provider.ZORROA,
            Category.ZORROA_TL,
            ModType.CLIPIFIER,
            listOf(SupportedMedia.Video),
            listOf(
                ModOp(
                    ModOpType.APPEND,
                    listOf(
                        ProcessorRef("zmlp_core.clipify.ShotDetectionVideoClipifier",
                            StandardContainers.CORE)
                    )
                )
            ),
            restricted = false,
            standard = true
        ),
        PipelineModSpec(
            "zvi-object-detection",
            "Detect everyday objects in images, video, and documents.",
            Provider.ZORROA,
            Category.ZORROA_STD,
            ModType.OBJECT_DETECTION,
            listOf(SupportedMedia.Images, SupportedMedia.Documents),
            listOf(
                ModOp(
                    ModOpType.APPEND,
                    listOf(
                        ProcessorRef("zmlp_analysis.detect.ZmlpObjectDetectionProcessor",
                            StandardContainers.ANALYSIS)
                    )
                )
            ),
            restricted = false,
            standard = true
        ),
        PipelineModSpec(
            "zvi-label-detection",
            "Generate keyword labels for image, video, and documents.",
            Provider.ZORROA,
            Category.ZORROA_STD,
            ModType.LABEL_DETECTION,
            listOf(SupportedMedia.Images, SupportedMedia.Documents),
            listOf(
                ModOp(
                    ModOpType.APPEND,
                    listOf(
                        ProcessorRef("zmlp_analysis.mxnet.ZviLabelDetectionResNet152",
                            StandardContainers.ANALYSIS)
                    )
                )
            ),
            restricted = false,
            standard = true
        ),
        PipelineModSpec(
            "zvi-text-detection",
            "Utilize OCR technology to detect text on an image.",
            Provider.ZORROA,
            Category.ZORROA_STD,
            ModType.OCR,
            listOf(SupportedMedia.Images),
            listOf(
                ModOp(
                    ModOpType.APPEND,
                    listOf(
                        ProcessorRef("zmlp_analysis.ocr.processors.OcrProcessor",
                            StandardContainers.ANALYSIS)
                    )
                )
            ),
            restricted = false,
            standard = true
        ),
        PipelineModSpec(
            "clarifai-predict-general",
            "Clarifai prediction API with general model.",
            Provider.CLARIFAI,
            Category.CLARIFAI_STD,
            ModType.LABEL_DETECTION,
            listOf(SupportedMedia.Images, SupportedMedia.Documents),
            listOf(
                ModOp(
                    ModOpType.APPEND,
                    listOf(
                        ProcessorRef("zmlp_analysis.clarifai.ClarifaiPredictGeneralProcessor",
                            StandardContainers.ANALYSIS)
                    )
                )
            ),
            restricted = false,
            standard = true
        ),
        PipelineModSpec(
            "gcp-label-detection",
            "Detect and extract information about entities in " +
                "an image, across a broad group of categories.",
            Provider.GOOGLE,
            Category.GOOGLE_VISION,
            ModType.LABEL_DETECTION,
            listOf(SupportedMedia.Images, SupportedMedia.Documents),
            listOf(
                ModOp(
                    ModOpType.APPEND,
                    listOf(
                        ProcessorRef("zmlp_analysis.google.CloudVisionDetectLabels",
                            StandardContainers.ANALYSIS)
                    )
                )
            ),
            restricted = false,
            standard = true
        ),
        PipelineModSpec(
            "gcp-object-detection",
            "Detect and extract multiple objects in an image.",
            Provider.GOOGLE,
            Category.GOOGLE_VISION,
            ModType.OBJECT_DETECTION,
            listOf(SupportedMedia.Images, SupportedMedia.Documents),
            listOf(
                ModOp(
                    ModOpType.APPEND,
                    listOf(
                        ProcessorRef("zmlp_analysis.google.CloudVisionDetectObjects",
                            StandardContainers.ANALYSIS)
                    )
                )
            ),
            restricted = false,
            standard = true
        ),
        PipelineModSpec(
            "gcp-logo-detection",
            "Detect popular product logos within an image.",
            Provider.GOOGLE,
            Category.GOOGLE_VISION,
            ModType.LOGO_DETECTION,
            listOf(SupportedMedia.Images, SupportedMedia.Documents),
            listOf(
                ModOp(
                    ModOpType.APPEND,
                    listOf(
                        ProcessorRef("zmlp_analysis.google.CloudVisionDetectLogos",
                            StandardContainers.ANALYSIS)
                    )
                )
            ),
            restricted = false,
            standard = true
        ),
        PipelineModSpec(
            "gcp-landmark-detection",
            "Detect popular natural and man-made structures within an image.",
            Provider.GOOGLE,
            Category.GOOGLE_VISION,
            ModType.LANDMARK_DETECTION,
            listOf(SupportedMedia.Images, SupportedMedia.Documents),
            listOf(
                ModOp(
                    ModOpType.APPEND,
                    listOf(
                        ProcessorRef("zmlp_analysis.google.CloudVisionDetectLandmarks",
                            StandardContainers.ANALYSIS)
                    )
                )
            ),
            restricted = false,
            standard = true
        )
    )
}
