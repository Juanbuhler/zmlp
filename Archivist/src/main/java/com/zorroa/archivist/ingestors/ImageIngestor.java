package com.zorroa.archivist.ingestors;

import com.drew.imaging.ImageMetadataReader;
import com.drew.lang.Rational;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.GpsDirectory;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.exception.UnrecoverableIngestProcessorException;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import com.zorroa.archivist.sdk.schema.ImageSchema;
import com.zorroa.archivist.sdk.schema.KeywordsSchema;
import org.elasticsearch.common.joda.time.DateTime;
import org.elasticsearch.common.joda.time.format.DateTimeFormat;
import org.elasticsearch.common.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * The ImageProcessor contains all of the logic to process an image.  This includes populating
 * the ImageSchema on the asset and gleaning as much information as possible from the file
 * itself.  This includes, width, hight, bits-per-pixel, colorspace as well as EXIF, EXR header,
 * DPX headers, etc.
 *
 */
public class ImageIngestor extends IngestProcessor {

    private static final Logger logger = LoggerFactory.getLogger(ImageIngestor.class);

    public ImageIngestor() {
        for (String format: ImageIO.getReaderFormatNames()) {
            supportedFormats.add(format);
        }
    }

    @Override
    public void process(AssetBuilder asset) {

        /*
         * Extract the standard image metadata, like width/height.
         */
        if (!asset.contains("image")) {
            extractImageMetadata(asset, asset.getInputStream());

            /**
             * Need to copy fields from the standard metadata area to someplace we can control.
             * Maybe these can be in a user editable schema.
             */
            try {
                if (!asset.isUpdate()) {
                    int rating = asset.getAttrOrDefault("Xmp.Rating", 0);
                    asset.setAttr("user", "rating", rating);
                }
            } catch (Exception e) {
                logger.warn("Failed to set image rating on {}", asset.getAbsolutePath(), e);
            }
        }
        else {
            logger.debug("Image metadata already exists for {}", asset);
        }

        /*
         * Extract EXIF metadata
         */
        extractExifMetadata(asset);
    }

    /**
     * Extract all metadata fields into the format <directory>:<tag>=<value>,
     * and store two versions of the value, the original value, and an optional
     * .description variant which is more human-readable. Tag names are stored
     * using a string containing only [A-Za-a0-9] characters, with spaces, dashes
     * and other characters removed. This results in the EXIF-standard names,
     * though some formats (e.g. IPTC) contain tags with '/' or '-', (ugh).
     * Some tags have multiple names, which we'll handle later.
     *
     * @param asset
     */
    private void extractExifMetadata(AssetBuilder asset) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(asset.getInputStream());
            extractExifMetadata(asset, metadata);   // Extract all useful metadata fields in raw & descriptive format
            extractExifLocation(asset, metadata);   // Find the best location value and promote to top-level
        } catch (Exception e) {
            throw new UnrecoverableIngestProcessorException(
                    "Unable to extract EXIF metadata from " + asset.getAbsolutePath(), e, getClass());
        }
    }
    /**
     * Extracts the non-EXIF metadata from the image.  This can include things like size,
     * colorspace, or anything else defined in the ImageSchema class.
     *
     * If there is an error processing this information, the image is not processable.
     * An IngestProcessorException is thrown which skips this asset.
     *
     * @param asset
     */
    private void extractImageMetadata(AssetBuilder asset, InputStream inputStream) {
        try {
            BufferedImage image = ImageIO.read(asset.getInputStream());
            asset.setImage(image);
            ImageSchema schema = new ImageSchema();
            schema.setWidth(image.getWidth());
            schema.setHeight(image.getHeight());
            asset.addSchema(schema);
        } catch (Exception e) {
            throw new UnrecoverableIngestProcessorException(
                    "Unable to determine image dimensions" + asset.getAbsolutePath(), e, getClass());
        }
    }

    /**
     * The default set of tags for building the keyword list.
     */
    private static final Set<String> defaultKeywordTags = ImmutableSet.<String>builder()
            .add("Exif.UserComment")
            .add("Exif.ColorSpace")
            .add("Exif.Make")
            .add("Exif.Model")
            .add("IPTC.Keywords")
            .add("IPTC.CopyrightNotice")
            .add("IPTC.Source")
            .add("IPTC.City")
            .add("IPTC.ProvinceState")
            .add("IPTC.CountryPrimaryLocationName")
            .add("File.Filename")
            .add("Xmp.Lens")
            .build();

    private static final List<String> defaultDateArgs = ImmutableList.<String>builder()
            .add("Exif.DateTimeOriginal")
            .add("Exif.DateTimeDigitized")
            .add("Exif.DateTime")
            .add("IPTC.DateCreated")
            .add("IPTC.TimeCreated")
            .add("File.FileModifiedDate")
            .build();

    private static final DateTimeFormatter extraDateFormatter = DateTimeFormat.forPattern("yyyy:MM:dd HH:mm:ss");

    private void extractExifMetadata(AssetBuilder asset, Metadata metadata) {

        Set<String> keywordArgs = getArgs().containsKey("keywordTags") ?
                ImmutableSet.copyOf((List<String>) getArgs().get("keywordTags")) : defaultKeywordTags;

        List<String> dateArgs = getArgs().containsKey("dateTags") ?
                (List<String>)getArgs().get("dateTags") : defaultDateArgs;

        /*
         * Set our mostValidDateField to a value outside the possible range.
         * If we find a date field less than this value we use it.  If we then
         * find one even further up the list, we use that.
         */
        int mostValidDateField = defaultDateArgs.size() + 1;

        for (Directory directory : metadata.getDirectories()) {
            String namespace = directory.getName().split(" ", 2)[0];
            for (Tag tag : directory.getTags()) {
                Object value = directory.getObject(tag.getTagType());
                if (value == null) {
                    continue;
                }

                String key = tag.getTagName().replaceAll("[^A-Za-z0-9]", "");
                String id = namespace + "." + key;

                //logger.info("{}= {}", id, value);
                /*
                 * Handle string formatted dates
                 */
                Date date = directory.getDate(tag.getTagType());
                if (date == null && value instanceof String) {
                    try {
                        DateTime dt = extraDateFormatter.parseDateTime((String)value);
                        date = dt.toDate();
                    } catch (IllegalArgumentException e) {
                        /*
                         * It wasn't a date, just ignore
                         */
                    }
                }

                /*
                 * Check for the most valid date.
                 */
                if (date != null) {
                    Calendar cal = Calendar.getInstance();
                    int curYear = cal.get(Calendar.YEAR);
                    cal.setTime(date);
                    int year = cal.get(Calendar.YEAR);
                    if (year > 1700 && year <= curYear + 1 /* just to be safe */) {
                        int dateFieldPriority = dateArgs.indexOf(id);
                        if (dateFieldPriority >= 0 && dateFieldPriority < mostValidDateField) {
                            mostValidDateField = dateFieldPriority;
                            asset.getSource().setDate(date);
                            continue;
                        }
                    }
                }

                // Descriptions are human-readable forms of the metadata.
                // Always save the original format, and also save a description if it
                // has some useful additional information for searching & display.
                String description = tag.getDescription();
                String descriptionKey = key + ".description";
                if (description.equals(directory.getString(tag.getTagType()))) {
                    description = null;
                }
                if (value.getClass().isArray() &&
                        (value.getClass().getComponentType().getName().equals("java.lang.String")
                                || Array.getLength(value) > 16)) {
                    description = null;
                }

                /*
                 * Check for special data types that need to be handled, otherwise
                 * just add the data to the object
                 */
                if (value instanceof String) {

                    String strValue = (String) value;
                    if (strValue.length() >= 256) {
                        continue;
                    }
                    asset.setAttr(namespace, key, strValue);
                    asset.addKeywords(keywordArgs.contains(id) ? KeywordsSchema.CONFIDENCE_MAX : 0, true, strValue);
                } else if (value instanceof Rational) {
                    Rational rational = (Rational)value;
                    asset.setAttr(namespace, key, rational.doubleValue());
                    if (description != null) {
                        asset.setAttr(namespace, descriptionKey, description);
                    }
                } else if (value instanceof Number) {
                    asset.setAttr(namespace, key, value);
                    if (description != null) {
                        asset.setAttr(namespace, descriptionKey, description);
                    }
                } else if (value.getClass().isArray()) {
                    String componentName = value.getClass().getComponentType().getName();
                    if (componentName.equals("java.lang.String")) {
                        String[] strList = (String[]) value;
                        asset.setAttr(namespace, key, value);
                        asset.addKeywords(keywordArgs.contains(id) ? KeywordsSchema.CONFIDENCE_MAX : 0, true, strList);
                    } else if (componentName.equals("com.drew.lang.Rational")) {
                        Rational[] rationals = (Rational[]) value;
                        Double[] doubles = new Double[rationals.length];
                        for (int i = 0; i < rationals.length; i++) {
                            doubles[i] = rationals[i].doubleValue();
                        }
                        asset.setAttr(namespace, key, doubles);
                    } else if (value.getClass().getComponentType().isPrimitive()) {
                        if (Array.getLength(value) <= 16) {
                            asset.setAttr(namespace, key, value);
                            if (description != null) {
                                asset.setAttr(namespace, descriptionKey, description);
                            }
                        }
                    }
                }
                /*
                 * If the value isn't a type we specifically handle, then its just
                 * skipped over.  It likely will just be garble to ElasticSearch
                 * anyway.
                 */
            }
        }
    }

    private static double dmsToDegrees(int d, int m, int s) {
        return Math.signum(d) * (Math.abs(d) + (m / 60.0) + (s / 3600.0));
    }

    private void extractExifLocation(AssetBuilder asset, Metadata metadata) {
        Directory exifDirectory = metadata.getFirstDirectoryOfType(GpsDirectory.class);
        if (exifDirectory != null) {
            int[] latitude = exifDirectory.getIntArray(GpsDirectory.TAG_LATITUDE);
            int[] longitude = exifDirectory.getIntArray(GpsDirectory.TAG_LONGITUDE);
            if (latitude != null && longitude != null) {
                double lat = dmsToDegrees(latitude[0], latitude[1], latitude[2]);
                double lon = dmsToDegrees(longitude[0], longitude[1], longitude[2]);
                Point2D.Double location = new Point2D.Double(lat, lon);
                asset.getSchema("image", ImageSchema.class).setLocation(location);
            }
        }
    }
}
