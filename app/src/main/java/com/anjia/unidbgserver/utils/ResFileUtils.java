package com.anjia.unidbgserver.utils;

import org.apache.commons.io.FileUtils;

import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;

public class ResFileUtils {

    public static File open(String path) throws IOException {
        File soLibFile = new File(System.getProperty("java.io.tmpdir"), path);
        if (!soLibFile.exists()) {
            FileUtils.copyInputStreamToFile(new ClassPathResource(path).getInputStream(), soLibFile);
        }
        return soLibFile;
    }
}
