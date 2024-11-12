package com.luv2code.jobportal.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;


//PLEASE NOTE: This configuration class will amp requests for /photos to server files from a directory on our file system

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    // setting up a field for the name of our upload directory
    private static final String UPLOAD_DIR = "photos";

    // here we're OVERRIDING the default implementation to set up a custom resource handler
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        exposeDirectory(UPLOAD_DIR, registry);
    }


    // HERE; we convert the uploadDir string to a Path. This maps the web URL request starting with "/photos/**" to a file system location
    // the ** will match on all sub-directories or sub-URLs for those given request
    private void exposeDirectory(String uploadDir, ResourceHandlerRegistry registry) {

        Path path = Paths.get(uploadDir);
        registry.addResourceHandler("/" + uploadDir + "/**")
                .addResourceLocations("file:" + path.toAbsolutePath() + "/");

    }
}
