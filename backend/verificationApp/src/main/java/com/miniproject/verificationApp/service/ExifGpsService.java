package com.miniproject.verificationApp.service;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.GpsDirectory;
import com.drew.lang.GeoLocation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class ExifGpsService {

    private static final Logger logger =
            LoggerFactory.getLogger(ExifGpsService.class);

    // Extract GPS coordinates from image
    public double[] extractGpsCoordinates(File imageFile) {

        try {

            Metadata metadata =
                    ImageMetadataReader.readMetadata(imageFile);

            GpsDirectory gpsDirectory =
                    metadata.getFirstDirectoryOfType(GpsDirectory.class);

            if (gpsDirectory == null) {
                return null;
            }

            GeoLocation geoLocation =
                    gpsDirectory.getGeoLocation();

            if (geoLocation == null || geoLocation.isZero()) {
                return null;
            }

            return new double[]{
                    geoLocation.getLatitude(),
                    geoLocation.getLongitude()
            };

        } catch (Exception e) {
            logger.warn(
                    "Unable to read image GPS metadata exceptionType={}",
                    e.getClass().getName()
            );
            return null;
        }
    }

    // Haversine Formula
    public double calculateDistanceMeters(
            double lat1,
            double lon1,
            double lat2,
            double lon2
    ) {

        final int EARTH_RADIUS = 6371000;

        double latDistance =
                Math.toRadians(lat2 - lat1);

        double lonDistance =
                Math.toRadians(lon2 - lon1);

        double a =
                Math.sin(latDistance / 2)
                        * Math.sin(latDistance / 2)
                        + Math.cos(Math.toRadians(lat1))
                        * Math.cos(Math.toRadians(lat2))
                        * Math.sin(lonDistance / 2)
                        * Math.sin(lonDistance / 2);

        double c =
                2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS * c;
    }
}