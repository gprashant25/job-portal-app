package com.luv2code.jobportal.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class FileUploadUtil {

    // here we will have a method for helping us to save the file
    // Note: the Multipart file actually has the image file that the user uploaded from the form
    public static void saveFile(String uploadDir, String filename,
                       MultipartFile multipartFile) throws IOException {

        Path uploadPath = Paths.get(uploadDir);
        if(!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        // we'll read the input stream
        try(InputStream inputStream = multipartFile.getInputStream();){

            Path path = uploadPath.resolve(filename);

            System.out.println("FilePath " + path);
            System.out.println("filename " + filename);

            //copy the content from the input stream to the path
            Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING);

        }catch (IOException ioe) {
            throw new IOException("Could not save image file: " + filename, ioe);
        }

    }
}
