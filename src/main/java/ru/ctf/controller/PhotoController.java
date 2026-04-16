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
import java.nio.file.Path;
import java.nio.file.Paths;
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
    public void getPhoto(@RequestParam(name = "file") String fileName, HttpServletResponse httpServletResponse, ServletRequest servletRequest) throws InvalidAlgorithmParameterException {
        logger.info("URI: {} host: {} file:{}", ((HttpServletRequest)servletRequest).getRequestURI(), servletRequest.getRemoteHost(), fileName);

        try {
            Path basePath = Paths.get(pathDirPhoto).toAbsolutePath().normalize();
            Path targetPath = basePath.resolve(fileName).normalize();

            if (!targetPath.startsWith(basePath)) {
                httpServletResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            File downloadFile = targetPath.toFile();

            if (!downloadFile.exists() || !downloadFile.isFile()) {
                httpServletResponse.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            try (InputStream is = new FileInputStream(downloadFile);
                 OutputStream outStream = httpServletResponse.getOutputStream();
            ) {
                String mimeType = servletContext.getMimeType(downloadFile.getAbsolutePath());
                if (mimeType == null) {
                    mimeType = "application/octet-stream";
                }
                httpServletResponse.setContentType(mimeType);
                httpServletResponse.setContentLength((int)downloadFile.length());
                
                String headerKey = "Content-Disposition";
                String headerValue = String.format("attachment; filename=\"%s\"", downloadFile.getName());
                httpServletResponse.setHeader(headerKey, headerValue);
                
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead = -1;

                while ((bytesRead = is.read(buffer)) != -1) {
                    outStream.write(buffer, 0, bytesRead);
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            httpServletResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }
}