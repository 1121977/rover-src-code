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
        logger.info("URI: {} host: {}",
                ((HttpServletRequest) servletRequest).getRequestURI(),
                servletRequest.getRemoteHost());

        Set<String> filesSet = Stream.of(new File(pathDirPhoto).listFiles())
                .filter(file -> !file.isDirectory())
                .map(File::getName)
                .collect(Collectors.toSet());

        model.addAttribute("fileSet", filesSet);
        model.addAttribute("path", new File("").getAbsolutePath());
        return "list";
    }

    @GetMapping(value = "/photo/get")
    public void getPhoto(
            @RequestParam(name = "file") String fileName,
            HttpServletResponse httpServletResponse,
            ServletRequest servletRequest
    ) {
        logger.info("URI: {} host: {} file: {}",
                ((HttpServletRequest) servletRequest).getRequestURI(),
                servletRequest.getRemoteHost(),
                fileName);

        
        if (fileName == null || fileName.isEmpty()) {
            httpServletResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        
        if (fileName.contains("\0") || fileName.contains("/") || fileName.contains("\\")) {
            logger.warn("Blocked suspicious file request: {}", fileName);
            httpServletResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        try {
            
            File baseDir = new File(pathDirPhoto).getCanonicalFile();
            File requestedFile = new File(baseDir, fileName).getCanonicalFile();

            
            if (!requestedFile.getAbsolutePath().startsWith(baseDir.getAbsolutePath() + File.separator)) {
                logger.warn("Path traversal attempt blocked! Requested: {} Base: {}",
                        requestedFile.getAbsolutePath(), baseDir.getAbsolutePath());
                httpServletResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            if (!requestedFile.exists() || !requestedFile.isFile()) {
                httpServletResponse.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            // --- Безопасная отдача файла ---
            String fullPath = requestedFile.getAbsolutePath();
            String mimeType = servletContext.getMimeType(fullPath);
            if (mimeType == null) {
                mimeType = "application/octet-stream";
            }

            httpServletResponse.setContentType(mimeType);
            httpServletResponse.setContentLength((int) requestedFile.length());
            httpServletResponse.setHeader(
                    "Content-Disposition",
                    String.format("attachment; filename=\"%s\"", requestedFile.getName())
            );

            try (InputStream is = new FileInputStream(requestedFile);
                 OutputStream outStream = httpServletResponse.getOutputStream()) {
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    outStream.write(buffer, 0, bytesRead);
                }
            }

        } catch (IOException e) {
            logger.error("Error serving file: {}", e.getMessage());
            httpServletResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }
}
