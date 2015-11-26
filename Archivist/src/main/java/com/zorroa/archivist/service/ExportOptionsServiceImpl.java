package com.zorroa.archivist.service;

import com.zorroa.archivist.sdk.domain.*;
import com.zorroa.archivist.sdk.schema.SourceSchema;
import net.coobird.thumbnailator.Thumbnails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

@Component
public class ExportOptionsServiceImpl implements ExportOptionsService {

    private static final Logger logger = LoggerFactory.getLogger(ExportOptionsServiceImpl.class);

    @Override
    public ExportedAsset applyOptions(Export export, ExportOutput output, Asset asset) throws Exception {
        /*
         * Currently we only handle image data.
         */
        ExportedAsset result = new ExportedAsset(export, output, asset);
        switch (asset.getType()) {
            case Image:
                applyImageOptions(export, output, result);
                break;
            default:
                logger.warn("Asset type: '{}' currently not supported.", asset.getType());
        }
        return result;
    }

    public void applyImageOptions(Export export, ExportOutput output, ExportedAsset asset) throws Exception {
        ExportOptions.Images imgOpts = export.getOptions().getImages();
        if (imgOpts == null) {
            return;
        }

        SourceSchema source = asset.getAsset().getValue("source", SourceSchema.class);
        String format =  imgOpts.getFormat() == null ? source.getExtension() : imgOpts.getFormat();
        BufferedImage inputImage = ImageIO.read(asset.getCurrentFile());
        BufferedImage outputImage;

        outputImage = Thumbnails.of(inputImage)
                .outputQuality(imgOpts.getQuality())
                .outputFormat(imgOpts.getFormat())
                .scale(imgOpts.getScale())
                .asBufferedImage();

        File outputFile = new File(asset.nextPath(source.getBasename(), format));
        ImageIO.write(outputImage, format, outputFile);
    }
}
