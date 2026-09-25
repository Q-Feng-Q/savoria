package com.familykitchen.feedback;

import com.familykitchen.common.api.ApiResponse;
import com.familykitchen.common.security.CurrentUserProvider;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** Login-only feedback endpoints; family and merchant scope are deliberately not required. */
@RestController
@RequestMapping("/feedback")
public class FeedbackController {
  private final CurrentUserProvider users;
  private final FeedbackService service;
  private final FeedbackImageService images;
  /**
   * Create login-only feedback endpoints.
   * @param users current identity provider
   * @param service feedback application service
   * @param images private image service
   */
  public FeedbackController(CurrentUserProvider users,FeedbackService service,FeedbackImageService images){this.users=users;this.service=service;this.images=images;}
  @PostMapping
  /**
   * Submit feedback without requiring family membership.
   * @param request authenticated request
   * @param body submission fields
   * @return owner detail envelope
   */
  public ApiResponse<Map<String,Object>> create(HttpServletRequest request,@RequestBody FeedbackService.Create body){return ApiResponse.ok(service.create(users.require(request),body));}
  @GetMapping
  /**
   * List only the current account's feedback.
   * @param request authenticated request
   * @param page one-based page
   * @param pageSize page length
   * @return paginated envelope
   */
  public ApiResponse<Map<String,Object>> list(HttpServletRequest request,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int pageSize){return ApiResponse.ok(service.list(users.require(request),false,null,null,page,pageSize));}
  @GetMapping("/{id}")
  /**
   * Read feedback owned by the caller.
   * @param request authenticated request
   * @param id feedback ID
   * @return owner detail envelope
   */
  public ApiResponse<Map<String,Object>> detail(HttpServletRequest request,@PathVariable long id){return ApiResponse.ok(service.detail(users.require(request),id,false));}
  @PostMapping(value="/images",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
  /**
   * Upload one private JPEG or PNG.
   * @param request authenticated request
   * @param file multipart image
   * @return attachment ID envelope
   */
  public ApiResponse<Map<String,String>> upload(HttpServletRequest request,@RequestPart("file") MultipartFile file){return ApiResponse.ok(images.upload(users.require(request),file));}
  @GetMapping("/images/{id}")
  /**
   * Serve authorized bytes with private response headers.
   * @param request authenticated request
   * @param id attachment ID
   * @return non-cacheable image response
   */
  public ResponseEntity<byte[]> image(HttpServletRequest request,@PathVariable String id){var image=images.read(users.require(request),id);
    return ResponseEntity.ok().contentType(MediaType.parseMediaType(image.mime())).cacheControl(CacheControl.noStore()).header("X-Content-Type-Options","nosniff").body(image.bytes());}
}
