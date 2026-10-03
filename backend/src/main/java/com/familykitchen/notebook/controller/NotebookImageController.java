package com.familykitchen.notebook.controller;

import com.familykitchen.common.api.ApiResponse;
import com.familykitchen.common.security.CurrentUserProvider;
import com.familykitchen.notebook.service.NotebookImageService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Authenticated private image upload, download and deletion. */
@RestController
@RequestMapping("/notebook/images")
public class NotebookImageController {
  private final CurrentUserProvider users;
  private final NotebookImageService service;

  /** Creates private image routes.
   * @param users current account
   * @param service storage operations */
  public NotebookImageController(CurrentUserProvider users, NotebookImageService service) {
    this.users = users; this.service = service;
  }

  /** Uploads a validated image to an editable record.
   *
   * @param request authenticated request
   * @param recordId record ID
   * @param file image
   *
   * @return opaque image ID and private record value key */
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ApiResponse<NotebookImageService.Uploaded> upload(HttpServletRequest request, @RequestParam long recordId,
      @RequestParam MultipartFile file) {
    return ApiResponse.ok(service.upload(users.require(request).userId(), recordId, file));
  }

  /** Uploads a temporary private image before a new record is saved.
   * @param request authenticated request
   * @param eventId event ID
   * @param file image file
   * @return staged private image reference */
  @PostMapping(path = "/staged", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ApiResponse<NotebookImageService.Uploaded> stage(HttpServletRequest request,
      @RequestParam long eventId, @RequestParam MultipartFile file) {
    return ApiResponse.ok(service.stage(users.require(request).userId(), eventId, file));
  }

  /** Returns bytes only after full record access is rechecked.
   *
   * @param request authenticated request
   * @param id image ID
   * @return no-store image bytes */
  @GetMapping("/{id}")
  public ResponseEntity<byte[]> read(HttpServletRequest request, @PathVariable long id) {
    var data = service.read(users.require(request).userId(), id);
    return ResponseEntity.ok().cacheControl(CacheControl.noStore())
        .header("X-Content-Type-Options", "nosniff")
        .contentType(MediaType.parseMediaType(data.contentType())).body(data.bytes());
  }

  /** Deletes metadata and queues private-file cleanup.
   *
   * @param request authenticated request
   * @param id image ID
   * @return empty success */
  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(HttpServletRequest request, @PathVariable long id) {
    service.delete(users.require(request).userId(), id);
    return ApiResponse.ok();
  }
}
