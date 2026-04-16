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
        logger.info("URI: {} host: {}", ((HttpServletRequest)servletRequest).getRequestURI(), servletRequest.getRemoteHost());
        File photoDir = new File(pathDirPhoto);
        if (photoDir.exists() && photoDir.isDirectory()) {
            Set<String> filesSet = Stream.of(photoDir.listFiles())
                    .filter(file -> !file.isDirectory())
                    .map(File::getName)
                    .collect(Collectors.toSet());
            model.addAttribute("fileSet", filesSet);
        }
        model.addAttribute("path", new File("").getAbsolutePath());
        return "list";
    }

    @GetMapping("/photo/get")
    public void getPhoto(@RequestParam(name = "file") String fileName, 
                         HttpServletResponse httpServletResponse, 
                         ServletRequest servletRequest) {
        logger.info("URI: {} host: {} file:{}", 
            ((HttpServletRequest)servletRequest).getRequestURI(), 
            servletRequest.getRemoteHost(), 
            fileName);

        File baseDir = new File(pathDirPhoto);
        File requestedFile = new File(baseDir, fileName);
        
        try {
            String canonicalBasePath = baseDir.getCanonicalPath();
            String canonicalRequestedPath = requestedFile.getCanonicalPath();
            

            if (!canonicalRequestedPath.startsWith(canonicalBasePath + File.separator) &&
                !canonicalRequestedPath.equals(canonicalBasePath)) {
                logger.warn("Path traversal attempt blocked: {}", fileName);
                httpServletResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
                return;
            }
            
            if (!requestedFile.exists() || requestedFile.isDirectory()) {
                httpServletResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            
            try (InputStream is = new FileInputStream(requestedFile);
                 OutputStream outStream = httpServletResponse.getOutputStream()) {
                
                String mimeType = servletContext.getMimeType(requestedFile.getName());
                if (mimeType == null) { mimeType = "application/octet-stream"; }
                
                httpServletResponse.setContentType(mimeType);
                httpServletResponse.setContentLength((int) requestedFile.length());
                httpServletResponse.setHeader("Content-Disposition", "attachment; filename=\"" + requestedFile.getName() + "\"");
                
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    outStream.write(buffer, 0, bytesRead);
                }
            }
        } catch (IOException e) {
            try { httpServletResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); } catch (IOException ignored) {}
        }
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }
}