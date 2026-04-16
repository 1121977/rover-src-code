package ru.ctf.controller;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.ServletContextAware;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Controller
public class PhotoController implements ServletContextAware {

    private ServletContext servletContext;
    private static final int BUFFER_SIZE = 4096;

    // Разрешаем только безопасные имена файлов без слэшей и спецсимволов
    private static final Pattern SAFE_FILE_NAME = Pattern.compile("^[a-zA-Z0-9._-]+$");

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
    public String getPhoto2(Model model, ServletRequest servletRequest, HttpServletResponse response) throws IOException {
        logger.info("URI: {} host: {}", ((HttpServletRequest) servletRequest).getRequestURI(), servletRequest.getRemoteHost());

        // Если страницу со списком файлов нельзя ограничить авторизацией,
        // безопаснее вообще не раскрывать содержимое каталога.
        model.addAttribute("fileSet", Collections.emptySet());
        model.addAttribute("path", "");
        return "list";
    }

    @GetMapping(value = "/photo/get")
    public void getPhoto(@RequestParam(name = "file") String fileName,
                         HttpServletResponse httpServletResponse,
                         ServletRequest servletRequest) throws IOException {

        logger.info("URI: {} host: {} file:{}", ((HttpServletRequest) servletRequest).getRequestURI(),
                servletRequest.getRemoteHost(), fileName);

        if (fileName == null || fileName.isBlank()) {
            httpServletResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, "File name is required");
            return;
        }

        if (!SAFE_FILE_NAME.matcher(fileName).matches()) {
            httpServletResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file name");
            return;
        }

        Path basePath = Paths.get(pathDirPhoto).toAbsolutePath().normalize();
        Path requestedPath = basePath.resolve(fileName).normalize();

        if (!requestedPath.startsWith(basePath)) {
            httpServletResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied");
            return;
        }

        if (!Files.exists(requestedPath) || !Files.isRegularFile(requestedPath) || !Files.isReadable(requestedPath)) {
            httpServletResponse.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found");
            return;
        }

        String mimeType = servletContext.getMimeType(requestedPath.toString());
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }

        httpServletResponse.setContentType(mimeType);
        httpServletResponse.setContentLengthLong(Files.size(requestedPath));

        String safeDownloadName = requestedPath.getFileName().toString();
        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(safeDownloadName, StandardCharsets.UTF_8)
                .build();
        httpServletResponse.setHeader(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString());

        try (InputStream is = Files.newInputStream(requestedPath);
             OutputStream outStream = httpServletResponse.getOutputStream()) {

            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                outStream.write(buffer, 0, bytesRead);
            }
            outStream.flush();

        } catch (IOException e) {
            logger.error("Error while sending file: {}", requestedPath, e);
            if (!httpServletResponse.isCommitted()) {
                httpServletResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to send file");
            }
        }
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }
}