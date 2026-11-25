package com.ecoretos.api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class ArchivoService {

    @Value("${upload.dir}")
    private String uploadDir;

    @Value("${app.public-url:}")
    private String publicUrl;

    /**
     * URL base pública del API. Usa PUBLIC_URL si está definida;
     * si no, la deduce de la petición actual (funciona detrás del proxy de Railway).
     */
    private String obtenerUrlBase() {
        if (publicUrl != null && !publicUrl.isBlank()) {
            return publicUrl.endsWith("/") ? publicUrl.substring(0, publicUrl.length() - 1) : publicUrl;
        }
        return ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
    }

    public String guardarImagen(MultipartFile imagen) {
        try {
            if (imagen.isEmpty()) {
                throw new RuntimeException("La imagen está vacía");
            }

            String nombreOriginal = imagen.getOriginalFilename();
            String extension = "";

            if (nombreOriginal != null && nombreOriginal.contains(".")) {
                extension = nombreOriginal.substring(nombreOriginal.lastIndexOf("."));
            }

            String nombreArchivo = UUID.randomUUID() + extension;

            Path carpeta = Paths.get(uploadDir).toAbsolutePath();
            Files.createDirectories(carpeta);

            Path rutaArchivo = carpeta.resolve(nombreArchivo);
            Files.copy(imagen.getInputStream(), rutaArchivo);

            return obtenerUrlBase() + "/uploads/" + nombreArchivo;

        } catch (Exception e) {
            throw new RuntimeException("Error al guardar imagen: " + e.getMessage());
        }
    }
}