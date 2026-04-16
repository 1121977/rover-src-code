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
    public void getPhoto(@RequestParam(name = "file") String fileName, HttpServletResponse httpServletResponse, ServletRequest servletRequest) {
        logger.info("URI: {} host: {} file:{}", ((HttpServletRequest)servletRequest).getRequestURI(), servletRequest.getRemoteHost(), fileName);

        if (isMissingFileName(fileName)) {
            httpServletResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        try {
            PhotoResolve resolved = resolvePhotoFile(fileName);
            switch (resolved.kind()) {
                case FORBIDDEN -> {
                    httpServletResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
                }
                case NOT_FOUND -> {
                    httpServletResponse.setStatus(HttpServletResponse.SC_NOT_FOUND);
                }
                case OK -> writePhotoFileToResponse(resolved.file(), httpServletResponse);
            }
        } catch (IOException e) {
            logger.error(e.getMessage());
            httpServletResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        }
    }

    private static boolean isMissingFileName(String fileName) {
        return fileName == null || fileName.isBlank();
    }

    private PhotoResolve resolvePhotoFile(String fileName) throws IOException {
        File baseDir = new File(pathDirPhoto).getCanonicalFile();
        File downloadFile = new File(baseDir, fileName).getCanonicalFile();
        Path basePath = baseDir.toPath();
        Path filePath = downloadFile.toPath();
        if (!filePath.startsWith(basePath)) {
            logger.warn("Path outside photo directory: {}", fileName);
            return PhotoResolve.forbidden();
        }
        if (!downloadFile.isFile()) {
            return PhotoResolve.notFound();
        }
        return PhotoResolve.ok(downloadFile);
    }

    private void writePhotoFileToResponse(File downloadFile, HttpServletResponse response) throws IOException {
        try (InputStream is = new FileInputStream(downloadFile);
             OutputStream outStream = response.getOutputStream()) {
            String mimeType = servletContext.getMimeType(downloadFile.getPath());
            if (mimeType == null) {
                mimeType = "application/octet-stream";
            }
            response.setContentType(mimeType);
            response.setContentLength((int) downloadFile.length());
            response.setHeader("Content-Disposition", String.format("attachment; filename=\"%s\"",
                    downloadFile.getName()));
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                outStream.write(buffer, 0, bytesRead);
            }
        }
    }

    private static final class PhotoResolve {
        enum Kind {
            OK,
            FORBIDDEN,
            NOT_FOUND
        }

        private final Kind kind;
        private final File file;

        private PhotoResolve(Kind kind, File file) {
            this.kind = kind;
            this.file = file;
        }

        static PhotoResolve ok(File file) {
            return new PhotoResolve(Kind.OK, file);
        }

        static PhotoResolve forbidden() {
            return new PhotoResolve(Kind.FORBIDDEN, null);
        }

        static PhotoResolve notFound() {
            return new PhotoResolve(Kind.NOT_FOUND, null);
        }

        Kind kind() {
            return kind;
        }

        File file() {
            return file;
        }
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }
}
