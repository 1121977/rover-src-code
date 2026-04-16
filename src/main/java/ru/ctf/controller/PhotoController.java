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
import java.nio.file.*;
import java.security.InvalidAlgorithmParameterException;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Controller
public class PhotoController implements ServletContextAware {

    private ServletContext servletContext;
    private static final int BUFFER_SIZE = 4096;
    
    // Разрешённые MIME типы и расширения
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
        "image/jpeg", "image/png", "image/gif", "image/webp"
    );
    
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
        if (!photoDir.exists() || !photoDir.isDirectory()) {
            logger.error("Photo directory does not exist: {}", pathDirPhoto);
            model.addAttribute("fileSet", Set.of());
            model.addAttribute("path", new File("").getAbsolutePath());
            return "list";
        }
        
        Set<String> filesSet = Stream.of(photoDir.listFiles())
                .filter(file -> !file.isDirectory())
                .map(File::getName)
                .filter(name -> {
                    String ext = getFileExtension(name);
                    return ALLOWED_EXTENSIONS.contains(ext.toLowerCase());
                })
                .collect(Collectors.toSet());
        model.addAttribute("fileSet", filesSet);
        model.addAttribute("path", new File("").getAbsolutePath());
        return "list";
    }

    @GetMapping(value = "/photo/get")
    public void getPhoto(@RequestParam(name = "file") String fileName, 
                         HttpServletResponse httpServletResponse, 
                         ServletRequest servletRequest) {
        
        logger.info("URI: {} host: {} file: {}", 
                    ((HttpServletRequest)servletRequest).getRequestURI(), 
                    servletRequest.getRemoteHost(), 
                    fileName);
        
        // ========== ВАЛИДАЦИЯ ВХОДНЫХ ДАННЫХ ==========
        
        // 1. Проверка на null и пустую строку
        if (fileName == null || fileName.trim().isEmpty()) {
            logger.warn("Empty file name provided");
            try {
                httpServletResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, "File name is required");
            } catch (IOException e) {
                logger.error("Failed to send error response", e);
            }
            return;
        }
        
        // 2. Извлечение безопасного имени файла (только имя, без путей)
        String safeFileName = extractSafeFileName(fileName);
        if (safeFileName == null || safeFileName.isEmpty()) {
            logger.warn("Invalid file name after sanitization: {}", fileName);
            try {
                httpServletResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name");
            } catch (IOException e) {
                logger.error("Failed to send error response", e);
            }
            return;
        }
        
        // 3. Проверка расширения файла
        String extension = getFileExtension(safeFileName);
        if (extension.isEmpty() || !ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            logger.warn("Blocked file with invalid extension: {} (original: {})", extension, fileName);
            try {
                httpServletResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "File type not allowed");
            } catch (IOException e) {
                logger.error("Failed to send error response", e);
            }
            return;
        }
        
        // ========== БЕЗОПАСНАЯ РАБОТА С ПУТЯМИ ==========
        
        Path basePath;
        try {
            basePath = Paths.get(pathDirPhoto).toRealPath().normalize();
        } catch (IOException e) {
            logger.error("Failed to resolve base path: {}", pathDirPhoto, e);
            try {
                httpServletResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server configuration error");
            } catch (IOException ex) {
                logger.error("Failed to send error response", ex);
            }
            return;
        }
        
        // Безопасное разрешение пути
        Path filePath;
        try {
            filePath = basePath.resolve(safeFileName).normalize();
        } catch (InvalidPathException e) {
            logger.error("Invalid path after resolution: {}", safeFileName, e);
            try {
                httpServletResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file path");
            } catch (IOException ex) {
                logger.error("Failed to send error response", ex);
            }
            return;
        }
        
        // Проверка, что файл находится внутри разрешённой директории
        if (!filePath.startsWith(basePath)) {
            logger.error("Path traversal attempt detected: {} -> {}", fileName, filePath);
            try {
                httpServletResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
            } catch (IOException e) {
                logger.error("Failed to send error response", e);
            }
            return;
        }
        
        // ========== ПРОВЕРКА ФАЙЛА ==========
        
        File downloadFile = filePath.toFile();
        
        // Проверка существования и типа файла
        if (!downloadFile.exists()) {
            logger.warn("File not found: {}", safeFileName);
            try {
                httpServletResponse.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found");
            } catch (IOException e) {
                logger.error("Failed to send error response", e);
            }
            return;
        }
        
        if (!downloadFile.isFile()) {
            logger.warn("Path is not a regular file: {}", safeFileName);
            try {
                httpServletResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Not a regular file");
            } catch (IOException e) {
                logger.error("Failed to send error response", e);
            }
            return;
        }
        
        if (!downloadFile.canRead()) {
            logger.warn("File is not readable: {}", safeFileName);
            try {
                httpServletResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "File access denied");
            } catch (IOException e) {
                logger.error("Failed to send error response", e);
            }
            return;
        }
        
        // ========== БЕЗОПАСНАЯ ЗАГРУЗКА ФАЙЛА ==========
        
        try (InputStream is = new FileInputStream(downloadFile);
             OutputStream outStream = httpServletResponse.getOutputStream()) {
            
            // Определение MIME типа
            String mimeType = servletContext.getMimeType(safeFileName);
            if (mimeType == null || !ALLOWED_MIME_TYPES.contains(mimeType)) {
                mimeType = "application/octet-stream";
            }
            
            httpServletResponse.setContentType(mimeType);
            httpServletResponse.setContentLengthLong(downloadFile.length()); // Используем setContentLengthLong для больших файлов
            
            // Безопасное формирование Content-Disposition
            String safeFilenameForHeader = safeFileName.replace("\"", "\\\"");
            String headerValue = String.format("attachment; filename=\"%s\"", safeFilenameForHeader);
            httpServletResponse.setHeader("Content-Disposition", headerValue);
            
            // Запрет кеширования для чувствительных данных
            httpServletResponse.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            httpServletResponse.setHeader("Pragma", "no-cache");
            
            // Потоковая передача файла
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                outStream.write(buffer, 0, bytesRead);
            }
            outStream.flush();
            
            logger.info("Successfully served file: {}", safeFileName);
            
        } catch (FileNotFoundException e) {
            logger.error("File disappeared during download: {}", safeFileName, e);
            if (!httpServletResponse.isCommitted()) {
                try {
                    httpServletResponse.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found");
                } catch (IOException ex) {
                    logger.error("Failed to send error response", ex);
                }
            }
        } catch (IOException e) {
            logger.error("IO error downloading file: {}", safeFileName, e);
            if (!httpServletResponse.isCommitted()) {
                try {
                    httpServletResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Download failed");
                } catch (IOException ex) {
                    logger.error("Failed to send error response", ex);
                }
            }
        }
    }

    /**
     * Безопасное извлечение имени файла из потенциально опасной строки.
     * Удаляет все path traversal попытки и оставляет только чистое имя файла.
     */
    private String extractSafeFileName(String input) {
        if (input == null || input.trim().isEmpty()) {
            return null;
        }
        
        // Нормализация: замена обратных слешей на прямые
        String normalized = input.replace('\\', '/');
        
        // Извлечение последнего компонента пути (чистое имя файла)
        String fileName = normalized;
        int lastSlash = normalized.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash + 1 < normalized.length()) {
            fileName = normalized.substring(lastSlash + 1);
        }
        
        // Удаление потенциально опасных символов
        fileName = fileName.trim()
            .replaceAll("[\\.]{2,}", "")  // Удаляем множественные точки
            .replaceAll("[\\x00-\\x1f]", "");  // Удаляем управляющие символы
        
        // Проверка, что осталось что-то разумное
        if (fileName.isEmpty() || fileName.equals(".") || fileName.equals("..")) {
            return null;
        }
        
        return fileName;
    }
    
    /**
     * Безопасное получение расширения файла.
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            return fileName.substring(lastDot + 1).toLowerCase();
        }
        return "";
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }
}