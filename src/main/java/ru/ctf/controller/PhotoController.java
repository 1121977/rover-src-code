package ru.ctf.controller;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.ServletContextAware;

import java.io.*;
import java.security.InvalidAlgorithmParameterException;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Controller
public class PhotoController implements ServletContextAware {

    private ServletContext servletContext;
    private static final int BUFFER_SIZE = 4096;
    @Autowired
    @Qualifier("pathDirPhoto")
    private String pathDirPhoto;
    @Autowired
    private Logger logger;

    @GetMapping("/")
    public String getRoot(Model model) {
        return "index";
    }

    @GetMapping(value = {"/photo/", "/photo"})
    public String getPhoto2(Model model, ServletRequest servletRequest) {
        logger.info("URI: {} host: {}", ((HttpServletRequest)servletRequest).getRequestURI(), servletRequest.getRemoteHost());
        Set<String> filesSet = Stream.of(new File(pathDirPhoto).listFiles())
                .filter(file -> !file.isDirectory())
                .map(File::getName)
                .collect(Collectors.toSet());
        model.addAttribute("fileSet", filesSet);
        model.addAttribute("path", new File("").getAbsolutePath());
        return "list";
    }

    @GetMapping(value = "/photo/get")
public void getPhoto(@RequestParam(name = "file") String fileName,
                     HttpServletResponse httpServletResponse,
                     ServletRequest servletRequest) throws InvalidAlgorithmParameterException {

    logger.info("URI: {} host: {} file:{}", ((HttpServletRequest)servletRequest).getRequestURI(),
            servletRequest.getRemoteHost(), fileName);

    try {
        File baseDirFile = new File(pathDirPhoto);
        String baseDir = baseDirFile.getCanonicalPath();

        File file = new File(baseDirFile, fileName);
        String canonicalPath = file.getCanonicalPath();

        
        if (!canonicalPath.startsWith(baseDir + File.separator)) {
            throw new SecurityException("Invalid file path");
        }

        try (InputStream is = new FileInputStream(file);
             OutputStream outStream = httpServletResponse.getOutputStream()) {

            String mimeType = servletContext.getMimeType(file.getAbsolutePath());
            if (mimeType == null) {
                mimeType = "application/octet-stream";
            }

            httpServletResponse.setContentType(mimeType);
            httpServletResponse.setContentLength((int) file.length());

            String headerValue = String.format("attachment; filename=\"%s\"", file.getName());
            httpServletResponse.setHeader("Content-Disposition", headerValue);

            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;

            while ((bytesRead = is.read(buffer)) != -1) {
                outStream.write(buffer, 0, bytesRead);
            }
        }

    } catch (Exception e) {
        logger.error(e.getMessage(), e);
    }
}

    @Override
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }
}
