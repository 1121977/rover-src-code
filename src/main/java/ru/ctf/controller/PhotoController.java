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
        logger.info("URI: {} host: {} file:{}", 
                    ((HttpServletRequest)servletRequest).getRequestURI(), 
                    servletRequest.getRemoteHost(), 
                    fileName);

        try {
            // Создаем объект File для базовой директории и нормализуем путь
            File baseDir = new File(pathDirPhoto).getCanonicalFile();
            
            // Проверяем, что базовая директория существует
            if (!baseDir.exists() || !baseDir.isDirectory()) {
                logger.error("Base directory does not exist or is not a directory: {}", pathDirPhoto);
                httpServletResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                                             "Server configuration error");
                return;
            }
            
            // Создаем объект для запрашиваемого файла и нормализуем путь
            File requestedFile = new File(baseDir, fileName).getCanonicalFile();
            
            // Проверяем, что файл находится внутри разрешенной директории
            if (!requestedFile.getPath().startsWith(baseDir.getPath())) {
                logger.error("Path traversal attempt detected: {}", fileName);
                httpServletResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
                return;
            }
            
            // Проверяем, что файл существует
            if (!requestedFile.exists()) {
                logger.error("File not found: {}", fileName);
                httpServletResponse.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found");
                return;
            }
            
            // Проверяем, что это действительно файл (не директория)
            if (!requestedFile.isFile()) {
                logger.error("Requested path is not a file: {}", fileName);
                httpServletResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file request");
                return;
            }
            
            // Отдаем файл
            try (InputStream is = new FileInputStream(requestedFile);
                 OutputStream outStream = httpServletResponse.getOutputStream()) {
                
                String mimeType = servletContext.getMimeType(requestedFile.getName());
                if (mimeType == null) {
                    mimeType = "application/octet-stream";
                }
                
                httpServletResponse.setContentType(mimeType);
                httpServletResponse.setContentLength((int) requestedFile.length());
                
                // Устанавливаем заголовки для ответа
                String headerKey = "Content-Disposition";
                String headerValue = String.format("attachment; filename=\"%s\"", 
                                                   requestedFile.getName());
                httpServletResponse.setHeader(headerKey, headerValue);
                
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                
                // Записываем данные из входного потока в выходной поток
                while ((bytesRead = is.read(buffer)) != -1) {
                    outStream.write(buffer, 0, bytesRead);
                }
            }
        } catch (IOException e) {
            logger.error("Error processing file request: {}", e.getMessage());
            try {
                httpServletResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                                             "Error processing file request");
            } catch (IOException ex) {
                logger.error("Error sending error response", ex);
            }
        } catch (Exception e) {
            logger.error("Unexpected error: {}", e.getMessage());
            try {
                httpServletResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                                             "Internal server error");
            } catch (IOException ex) {
                logger.error("Error sending error response", ex);
            }
        }
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }
}