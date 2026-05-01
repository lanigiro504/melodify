package com.melodify.controller.media;

import com.melodify.config.MelodifyAudioStorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/media/audio")
@RequiredArgsConstructor
public class AudioFileController {

	private final MelodifyAudioStorageProperties audioStorageProperties;

	@GetMapping("/{assetBizId}.mp3")
	public ResponseEntity<Resource> stream(@PathVariable String assetBizId) {
		if (!assetBizId.matches("[a-f0-9]{32}")) {
			return ResponseEntity.notFound().build();
		}
		if (!audioStorageProperties.isEnabled()) {
			return ResponseEntity.notFound().build();
		}
		Path base = Paths.get(audioStorageProperties.getLocalDir()).toAbsolutePath().normalize();
		Path file = base.resolve(assetBizId + ".mp3").normalize();
		if (!file.startsWith(base) || !Files.isReadable(file)) {
			return ResponseEntity.notFound().build();
		}
		FileSystemResource resource = new FileSystemResource(file);
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType("audio/mpeg"))
				.header(HttpHeaders.ACCEPT_RANGES, "bytes")
				.body(resource);
	}
}
