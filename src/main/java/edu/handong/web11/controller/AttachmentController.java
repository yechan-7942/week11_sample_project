package edu.handong.web11.controller;

import edu.handong.web11.domain.Attachment;
import edu.handong.web11.repository.AttachmentRepository;
import edu.handong.web11.service.FileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/attachments")
public class AttachmentController {
    private final AttachmentRepository attachments;
    private final FileStorageService storage;
    public AttachmentController(AttachmentRepository attachments, FileStorageService storage) {
        this.attachments = attachments; this.storage = storage;
    }
    // 다운로드 버튼이 요청하는 주소다. {id}에는 첨부파일 번호가 들어간다.
    @GetMapping("/{id}/download")
    // 주소에 들어 있는 첨부파일 번호를 id로 받는다.
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        // 첨부파일 id로 DB에서 파일을 찾는다.
        Attachment file = attachments.findById(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "첨부파일이 없습니다."));
        // 파일 정보에 적힌 저장 이름으로 서버의 실제 파일을 찾는다.
        Resource resource = storage.load(file);
        // 브라우저에 파일을 다운로드하라고 알려줄 정보를 만든다.
        String disposition = ContentDisposition.attachment()
            // 다운로드할 때 원래 파일명을 사용한다.
            .filename(file.getOriginalFilename(), StandardCharsets.UTF_8)
            // 위에서 정한 다운로드 정보를 문자열로 만든다.
            .build().toString();
        // 요청이 성공했다는 200 응답을 만든다.
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
            .body(resource);
    }


    //이미지 미리보기(Image Preview) 기능을 구현하기!
    @GetMapping("/{id}/view")
    public ResponseEntity<Resource> view(@PathVariable Long id) {
        Attachment file = attachments.findById(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "첨부파일이 없습니다."));

        Resource resource = storage.load(file);
        String contentType = file.getContentType();

        if (!"image/jpeg".equals(contentType) && !"image/png".equals(contentType)
                && !"image/gif".equals(contentType)) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "이미지 파일만 미리볼 수 있습니다.");
        }

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(contentType))
            .body(resource);
    }
}
