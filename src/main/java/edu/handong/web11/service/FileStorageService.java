package edu.handong.web11.service;

import edu.handong.web11.domain.Attachment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {
    private static final Set<String> ALLOWED = Set.of("jpg", "jpeg", "png", "gif", "pdf", "txt");
    private final Path uploadDir;

    public FileStorageService(@Value("${app.upload-dir}") String path) throws IOException {
        uploadDir = Path.of(path).toAbsolutePath().normalize();
        Files.createDirectories(uploadDir);
    }

    public Attachment store(MultipartFile file) {
        // 빈 파일이면 저장하지 않고 오류를 낸다.
        if (file.isEmpty()) throw new IllegalArgumentException("빈 파일은 업로드할 수 없습니다.");
        // 사용자가 첨부한 파일의 원래 이름을 가져온다.
        String original = file.getOriginalFilename();
        // 파일명이 없거나 /, \\, ..이 들어 있으면 거부한다.
        if (original == null || original.isBlank() || original.contains("/") || original.contains("\\") || original.contains(".."))
            throw new IllegalArgumentException("잘못된 파일 이름입니다.");
        // 파일명이 240자를 넘으면 오류를 낸다.
        if (original.length() > 240) throw new IllegalArgumentException("파일 이름이 너무 깁니다.");
        // 파일명에서 마지막 점의 위치를 찾는다.
        int dot = original.lastIndexOf('.');
        // 점 뒤의 확장자를 꺼내 소문자로 바꾼다.
        String ext = dot < 0 ? "" : original.substring(dot + 1).toLowerCase(Locale.ROOT);
        // 허용된 확장자인지 확인한다. 파일 내부 내용까지 검사하는 것은 아니다.
        if (!ALLOWED.contains(ext)) throw new IllegalArgumentException("허용 형식: jpg, jpeg, png, gif, pdf, txt");
        // 파일 크기가   10L * 1024 * 1024를 넘으면 오류를 낸다.
        if (file.getSize() > 10L * 1024 * 1024) throw new IllegalArgumentException("파일은 10MB 이하만 가능합니다.");
        String stored = UUID.randomUUID() + "." + ext;
        // 저장 폴더 경로와 새 파일명을 합쳐 실제 저장할 위치를 만든다.
        Path target = uploadDir.resolve(stored).normalize();
        // 저장 위치가 업로드 폴더 안인지 확인한다. 폴더 밖이면 저장하지 않는다.
        if (!target.startsWith(uploadDir)) throw new IllegalArgumentException("잘못된 파일 경로입니다.");

        try (InputStream input = file.getInputStream()) {
            // 읽은 파일 내용을 저장 위치에 복사한다. 같은 경로에 파일이 있으면 덮어쓴다.
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            // 사용자가 보낸 파일 타입 대신 확장자를 보고 파일 종류를 정한다.
            // jpg와 jpeg는 이미지로, 마지막에 남는 txt는 일반 텍스트로 정한다.
            String contentType = switch (ext) {
                case "jpg", "jpeg" -> "image/jpeg";
                case "png" -> "image/png";
                case "gif" -> "image/gif";
                case "pdf" -> "application/pdf";
                default -> "text/plain";
            };
            // 파일 이름, 종류, 크기를 첨부정보로 묶어 돌려준다.
            return new Attachment(original, stored, contentType, file.getSize());
        } catch (IOException e) {
            // 저장 중 오류가 나면 덜 저장된 파일을 지우려고 시도
            try { Files.deleteIfExists(target); } catch (IOException ignored) {}
            // 저장에 실패했다고 알림
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "파일 저장 실패", e);
        }
    }

    public Resource load(Attachment attachment) {
        Path target = uploadDir.resolve(attachment.getStoredFilename()).normalize();
        if (!target.startsWith(uploadDir) || !Files.isRegularFile(target))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "파일이 없습니다.");
        return new PathResource(target);
    }

    public void deleteIfExists(Attachment attachment) {
        try {
            Files.deleteIfExists(uploadDir.resolve(attachment.getStoredFilename()).normalize());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "파일 삭제 실패", e);
        }
    }
}
