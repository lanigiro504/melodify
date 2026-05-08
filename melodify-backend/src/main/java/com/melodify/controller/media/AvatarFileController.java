package com.melodify.controller.media;

import com.melodify.config.MelodifyAvatarStorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Locale;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/media/avatar")
@RequiredArgsConstructor
public class AvatarFileController {

	private static final Pattern ALLOWED_FILE = Pattern.compile(
			"^\\d+_[0-9a-fA-F\\-]{36}\\.(jpg|jpeg|png|webp)$");

	private final MelodifyAvatarStorageProperties properties;

	@GetMapping("/{filename:.+}")
	public ResponseEntity<Resource> get(@PathVariable String filename) {
		if (filename == null || !ALLOWED_FILE.matcher(filename).matches()) {
			return ResponseEntity.notFound().build();
		}
		Path base = Paths.get(properties.getLocalDir()).toAbsolutePath().normalize();
		Path file = base.resolve(filename).normalize();
		if (!file.startsWith(base) || !Files.isReadable(file)) {
			return ResponseEntity.notFound().build();
		}
		String lower = filename.toLowerCase(Locale.ROOT);
		MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
		if (lower.endsWith(".png")) {
			mediaType = MediaType.IMAGE_PNG;
		} else if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
			mediaType = MediaType.IMAGE_JPEG;
		} else if (lower.endsWith(".webp")) {
			mediaType = MediaType.parseMediaType("image/webp");
		}
		FileSystemResource resource = new FileSystemResource(file);
		return ResponseEntity.ok()
				.contentType(mediaType)
				.cacheControl(CacheControl.maxAge(Duration.ofDays(7)))
				.body(resource);
	}
}
